﻿// the main chat client script

var net = require('net');
var protocol = require('./protocol.js');

var mainWindow = null;
var ip = "127.0.0.1";
var client = null;
var username;

// ui and protocol handling
$(document).ready(function () {
	// handle username requests
	$('#username_form').submit(function (event) {
		event.preventDefault();
		var username = $('#username').val();
		ip = $('#ip_address').val();
		if (username == "") {
			$('#username_warning').text("Please enter a username");
		}
		else if (username.indexOf(" ") != -1) {
			$('#username_warning').text("Username may not contain spaces")
		}
		else {
			client = new net.Socket();
			client.setEncoding("utf8");
			client.connect(1337, ip, function () {
				listenForServer();
				client.write(protocol.usernameRequest(username));
			});
			// handle connection error
			client.on('error', function (error) { 
				$('#username_warning').text("Connection error. Verify IP address");
			});
		}
	});
	
	// handle general chat
	$('#general_chat_form').submit(function (event) {
		var message = $('#general_chat_input').val();
		event.preventDefault();
		if (message !== "") {
			client.write(protocol.generalMessage(message));
			$('#general_chat_input').val("");
		}
	});
	
	// handle disconnect
	$('#disconnect_button').click(function (event) {
		client.write(protocol.disconnectRequest());
	});
	
	// handle opening private message box
	$(document).on('click', '.user', function (event) {
		// do not open private chat with self
		if ($(this).data('user') != username) {
			console.log("looking for private div");
			var privateDiv = $('#private_chat').find($('#' + $(this).data('user').toLowerCase() + "_private"));
			if (!privateDiv.length) {
				privateDiv = createPrivateChat($(this).data('user'));
			}
		}
	});
	
	// handle sending private messages
	$(document).on('submit', '.private_form', function (event) {
		event.preventDefault();
		var target = $(this).data('user');
		var message = $('#' + target.toLowerCase() + "_input").val();
		if (message !== "") {
			client.write(protocol.privateMessage(username, target, message));
			$('#' + target.toLowerCase() + "_message").append(new Date().toLocaleTimeString() + " " + username + " says:<br>");
			$('#' + target.toLowerCase() + "_message").append(message + "<br><br>");
		}
		$('#' + target.toLowerCase() + "_input").val("");
	});
	
	// handle closing private chats
	$(document).on('click', '.close_private', function (event) {
		$(this).parent().remove();
	});
	
	// handle server messages
	function listenForServer()
	{
		client.on('data', function (data) {
			data = data.split("\r\n");
			// remove the last (blank) element
			data.pop();
			data.forEach(function (res) {
				console.log("server: " + res)
				var response = protocol.parseResponse(res);
				handleResponse(response);
			});
		});
	}
});

// function that handle server responses
function handleResponse(response) {
	switch (response.responseCode) {
		// server accepts username
		case '1':
			username = $('#username').val();
			displayOnlineUsers(response.userList);
			$('#login').hide();
			$('#username').val("");
			$('#username_warning').empty();
			$('#client').show();
			break;
		// server denies username
		case '2':
			$('#username_warning').text("Username taken")
			break;
		// server sends general message
		case '5':
			stickyScroll($('#general_chat'), function () {
				$('#general_chat').append(new Date().toLocaleTimeString() + " " + response.fromUser + " says:<br>");
				$('#general_chat').append(response.message + "<br><br>");
			});
			break;
		// server sends private message
		case '6':
			var privateDiv = $('#private_chat').find($('#' + response.fromUser.toLowerCase() + "_private"));
			if (!privateDiv.length) {
				privateDiv = createPrivateChat(response.fromUser);
			}
			$('#' + response.fromUser.toLowerCase() + "_message").append(new Date().toLocaleTimeString() + " " + response.fromUser + " says:<br>");
			$('#' + response.fromUser.toLowerCase() + "_message").append(response.message + "<br><br>");
			break;
		// disconnect confirmation
		case '8':
			client.destroy();
			$('#client').hide();
			$('#general_chat').empty();
			$('#private_chat').empty();
			$('#online_users').empty();
			$('#login').show();
			break;
		// other client disconnect notification
		case '9':
			$('#' + response.user.toLowerCase()).remove();
			break;
		// other client connect notification
		case '10':
			$('#online_users').append($('<li>', {'id': response.user.toLowerCase(), 'data-user': response.user }).addClass('user').text(response.user));
			break;
	}
}

// function to display the online user list
function displayOnlineUsers(userList) {
	userList.forEach(function (value) {
		if (value != "")
			$('#online_users').append($('<li>', { 'id': value.toLowerCase(), 'data-user': value }).addClass('user').text(value));
	});
}

// function to create a private chat window
function createPrivateChat(user) {
	if (user.toLowerCase() == username.toLowerCase())
		return;
	var chatDiv = $('<div>', { 'id': user.toLowerCase() + "_private" });
	chatDiv.append($('<h3>').text(user));
	chatDiv.append($('<div>', { 'id': user.toLowerCase() + "_message" }));
	
	var sendForm = $('<form>').addClass('private_form').data('user', user);
	sendForm.append($('<input>', { 'type': 'text', 'placeholder': 'Say something', 'id': user.toLowerCase() + "_input" }));
	sendForm.append($('<input>', { 'type': 'submit', 'value': 'Send it!' }).addClass('send_private'));
	
	chatDiv.append(sendForm);
	chatDiv.append($('<button>', { 'type': 'button' }).addClass('close_private').text('Close'));
	
	$('#private_chat').append(chatDiv);
	return chatDiv;
}

// function that auto-scrolls an element only if the user is scrolled to the bottom
function stickyScroll(div, callback) {
	var scrolledToBottom = div.prop('scrollHeight') - div.prop('clientHeight') <= div.scrollTop() + 1;
	callback();
	if (scrolledToBottom) {
		div.scrollTop(div.prop('scrollHeight') - div.prop('clientHeight'));
	}
}
