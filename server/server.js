var express = require('express');
var app = express();
var path = require('path'); 
var fs = require("fs");


var sessions = [];

app.use(express.static(__dirname + '/public'));

app.get('/', function (req, res) {
	var dest = 'index.html';
	res.sendFile(dest, { root: __dirname });
});

var server = app.listen(3000, function () {
	var host = server.address().address;
	var port = server.address().port;

	console.log('Watch My Tech started at http://%s:%s', host, port);
});


var io = require('socket.io').listen(server);

io.on('connection', function(socket){
	socket.on('startSession', function() {
		sessions[socket.id] = [];
		socket.emit('startSessionSuccess', socket.id);
		console.log('set up a cam client with id ' + socket.id);
	});
	
	socket.on('joinSession', function(sessionID) {
		if (sessionID in sessions) {
			sessions[sessionID].push(socket);
			socket.emit('startSessionSuccess', socket.id);
			console.log('regular user joined session with id ' + sessionID);
		} else {
			socket.emit('error', 'No such session exists!');
			console.log('regular user did not join session with id ' + sessionID + ' probably because it does not exist');
		}
	});
	
	socket.on('image', function(stringData) {
		var listOfClients = sessions[socket.id];
		for(var i=0; i<listOfClients.length; i+=1) {
			listOfClients[i].emit('image', stringData);
		}
	});
	
	socket.on('disconnect', function() {
		// check if the cam client disconnected
		if (socket.id in sessions) {
			// remove from the list
			delete sessions[socket.id];
			console.log('a cam client left');
		} else {
			console.log('a regular client left');
		}
	});
});