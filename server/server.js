var express = require('express');
var app = express();
var path = require('path'); 
var fs = require("fs");
var uuid = require('node-uuid');
var random = require("random-js")();


app.use(express.static(__dirname + '/public'));

app.get('/', function (req, res) {	
	if (/Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(req.headers['user-agent'] || "unknown")) {
		var dest = 'public/phone.html';
	} else {
		var dest = 'public/pc.html';
	}
	res.sendFile(dest, { root: __dirname });
});

var server = app.listen(3000, function () {
	var host = server.address().address;
	var port = server.address().port;

	console.log('Watch My Tech started at http://%s:%s', host, port);
});

function sendVoiceMessage(sessionID, message, socket) {
	if (sessionID in sessions) {
		var sourceSocket = sessions[sessionID].source;
		if (sourceSocket) {
			sourceSocket.emit('sayVoiceMessages', message);
			socket.emit('customBroadCastSuccess', '');
			console.log('sent voice to session id ' + sessionID);
		} else {
			socket.emit('watch_error', 'Source not set yet');
			console.log('Source not set yet');
		}
	} else {
		socket.emit('watch_error', 'No such session exists!');
		console.log('session with id ' + sessionID + ' not found probably because it does not exist');
	}
}

var countMod = 20;
var countNum = 0;
var sessions = [];
var sessionMAINID = 1000 + random.integer(1, 20);
var io = require('socket.io').listen(server);

io.on('connection', function(socket){

	socket.on('startSession', function() {
		sessions[sessionMAINID] = {
			'clients' : [socket],
			'source' : null
		};
		socket.emit('startSessionSuccess', sessionMAINID);
		console.log('set up phone client with id ' + sessionMAINID);
		sessionMAINID += random.integer(1, 20);
	});
	
	socket.on('joinSession', function(sessionID) {
		if (sessionID in sessions && sessions[sessionID].source) {
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
	
	socket.on('sendVoice', function(sessionID) {
		sendVoiceMessage(sessionID, '', socket);
	});
	
	socket.on('customBroadCast', function(data) {
		var sessionID = data.sessionID;
		var message = data.message;
		sendVoiceMessage(sessionID, message, socket);
	});
	
	socket.on('image', function(dataX) {
		if (sessionID in sessions) {
			var sessionID = dataX.sessionID;
			var dataList = dataX.frames;
		
			var listOfClients = sessions[sessionID].clients;
			for(var i=0; i<listOfClients.length; i+=1) {
				listOfClients[i].emit('image', dataList);
			}
			
			for(var i=0; i<dataList.length; i+=1) {
				var data = dataList[i];

				if (data.faces.length > 0) {
					if (countNum % countMod == 0) {
						var base64Data = data.stringData.replace(/^data:image\/png;base64,/, "");
						var filename = "out_" + uuid.v4() + ".png";
						fs.writeFile("captures/" + filename, base64Data, 'base64', function(err) {
							if (err) {
								console.log(err);
							} else {
								console.log("Saved file " + filename);
							}
						});
					}
					countNum += 1;
				}
			}
		}
	});
	
	socket.on('disconnect', function() {
		for(var prop in sessions) {
			if (sessions.hasOwnProperty(prop)) {
				if (sessions[prop].source && sessions[prop].source.id === socket.id) {
					var clients = sessions[prop].clients;
					for(var i=0; i<clients.length; i+=1) {
						clients[i].disconnect();
					}
					delete sessions[prop];
					console.log('a source left');
					return;
				} else {
					var clients = sessions[prop].clients;
					for(var i=0; i<clients.length; i+=1) {
						if (clients[i].id == socket.id) {
							clients.slice(i, 1);
							
							if (clients.length == 0) {
								setTimeout(function() {
									if (clients.length == 0) {
										delete sessions[prop];
										console.log('a source left because all clients left after, 3 minutes');
										return;
									}
								}, 60 * 3);
							}
							
							console.log('a phone client left');
							return;
						}
					}
				}
			}
		}
	});
});