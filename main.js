// handles the electron code

var electron = require('electron');
var app = electron.app;
var BrowserWindow = electron.BrowserWindow;

// electron code
app.on('window-all-closed', function () {
	app.quit();
});

app.on('ready', function () {
	mainWindow = new BrowserWindow({ width: 1024, height: 768, resizable: false, fullscreen: false });
	
	mainWindow.setMenu(null);
	
	mainWindow.openDevTools();

	mainWindow.loadURL('file://' + __dirname + '/client.html');
	
	mainWindow.on('closed', function () {
		mainWindow = null;
	});
});