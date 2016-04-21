// A module to send chat protocol client messages

// request username string
exports.usernameRequest = function (username) {
	return "0 " + username + "\r\n";
}

// general message string
exports.generalMessage = function (message) {
	return "3 " + message + "\r\n";
}

// private message string
exports.privateMessage = function (selfName, targetName, message) {
	return "4 " + selfName + " " + targetName + " " + message + "\r\n";
}

// disconnect request string
exports.disconnectRequest = function () {
	return "7\r\n"
}

// parse server responses
exports.parseResponse = function (res) {
	var result = {};
	var responseCode = res.split(" ")[0];
	switch (responseCode) {
		// server accepts username
		case '1':
			var response = res.split(" ");
			result.userList = response[1].split(",");
			result.welcomeMessage = response[2];
			break;
		// server denies username
		case '2':
			break;
		// server sends general message
		case '5':
			var response = splitLimited(res, " ", 3);
			result.fromUser = response[1];
			result.timestamp = response[2];
			result.message = response[3];
			break;
		// server sends private message
		case '6':
			var response = splitLimited(res, " ", 4);
			result.fromUser = response[1];
			result.toUser = response[2];
			result.timestamp = response[3];
			result.message = response[4];
			break;
		// disconnect confirmation
		case '8':
			break;
		// other client disconnect notification
		case '9':
			var response = res.split(" ");
			result.user = response[1];
			break;
		// other client connect notification
		case '10':
			var response = res.split(" ");
			result.user = response[1];
			break;
	}
	result.responseCode = responseCode;
	return result;
};

// function to split a string by <delim> only <count> times
function splitLimited(str, delim, count) {
	var parts = str.split(delim);
	var tail = parts.slice(count).join(delim);
	var result = parts.slice(0, count);
	result.push(tail);
	return result;
}