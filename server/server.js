var express = require('express');
var app = express();
var path = require('path'); 
var fs = require("fs");
var uuid = require('node-uuid');

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

var imagemod = 10;
var imagecount = 0;
var io = require('socket.io').listen(server);

io.on('connection', function(socket){
	socket.on('startSession', function() {
		if (socket.id in sessions) {
			socket.emit('watch_error', 'failed to start a session with phone client with id ' + socket.id);
			console.log('failed to start a session with phone client with id ' + socket.id);
		} else {
			sessions[socket.id] = {
				'clients' : [socket],
				'source' : null
			};
			socket.emit('startSessionSuccess', socket.id);
			console.log('set up phone client with id ' + socket.id);
		}
	});
	
	socket.on('joinSession', function(sessionID) {
		console.log(sessionID);
		if (socket.id in sessions) {
			socket.emit('watch_error', 'Cannot join this session because you already created it?!');
			console.log('phone did not join session with id ' + sessionID + ' probably because you created it');
		} else if (sessionID in sessions) {
			sessions[sessionID].clients.push(socket);
			socket.emit('joinSessionSuccess', '');
			console.log('phone joined session with id ' + sessionID);
		} else {
			socket.emit('watch_error', 'No such session exists!');
			console.log('phone did not join session with id ' + sessionID + ' probably because it does not exist');
		}
	});
	
	socket.on('sourceConnect', function(sessionID) {
		if (sessionID in sessions) {
			sessions[sessionID].source = socket;
			socket.emit('sourceConnectSuccess', '');
			console.log('connected source to session id ' + sessionID);
		} else {
			socket.emit('watch_error', 'No such session exists!');
			console.log('source did not join session with id ' + sessionID + ' probably because it does not exist');
		}
	});
	
	socket.on('image', function(data) {
		var sessionID = data.sessionID;
		var stringData = data.stringData;
		if (sessionID in sessions) {
			var listOfClients = sessions[sessionID].clients;
			for(var i=0; i<listOfClients.length; i+=1) {
				listOfClients[i].emit('image', stringData);
			}
		} else {
			console.log('could not send image to session with id ' + sessionID + ' probably because it does not exist');
		}
		
		// save it
		if (imagecount % imagemod == 0) {
			var base64Data = stringData.replace(/^data:image\/png;base64,/, "");
			var filename = "out_" + uuid.v4() + ".png";
			fs.writeFile("captures/" + filename, base64Data, 'base64', function(err) {
				if (err) {
					console.log(err);
				} else {
					console.log("Saved file " + filename);
				}
			});
		}
		imagecount += 1;
	});
	
	socket.on('disconnect', function() {
		for(var prop in sessions) {
			if (sessions.hasOwnProperty(prop)) {
				if (sessions[prop].source == socket.id) {
					delete sessions[prop];
					console.log('a source left');
					return;
				} else {
					var clients = sessions[prop].clients;
					for(var i=0; i<clients.length; i+=1) {
						if (clients[i].id == socket.id) {
							clients.slice(i, 1);
							console.log('a phone client left');
							return;
						}
					}
				}
			}
		}
	});
});