/**
 * A class representing a chat server
 *
 * Created by JeremyD on 4/13/2016.
 */

import java.net.*;
import java.io.*;
import java.util.TimeZone;
import java.util.concurrent.*;
import java.util.ArrayList;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class ChatServer {
    public static final int port = 1337;

    // list of connected clients
    public ArrayList<ClientConnection> clientList = new ArrayList<ClientConnection>();

    // the thread pool
    public static final Executor exec = Executors.newCachedThreadPool();

    // this method start the server
    public void start() throws IOException {
        // the server
        ServerSocket sock = null;

        try {
            sock = new ServerSocket(port);

            // keep track of running state
            boolean running = true;

            // the server loop
            while(running) {
                Socket client = sock.accept();
                ClientConnection clientTask = new ClientConnection(client);
                // check to make sure client initialization worked
                if (clientTask.isConnected())
                    // protocol message 10
                    broadcast("10 " + clientTask.getUsername() + "\r\n");
                    exec.execute(clientTask);
            }
        }
        catch (IOException ioe) {
            System.err.println(ioe);
        }
    }

    // broadcasts a message to every connected client
    private void broadcast(String msg) throws IOException {
        for (int i = 0; i < clientList.size(); i++) {
            clientList.get(i).write(msg);
        }
    }

    // helper method to get current GMT datetime
    private String getDatetimeGMT() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy:MM:dd:HH:mm:ss");
        Date date = new Date();
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormat.format(date);
    }

    public static void main(String[] args) {
        ChatServer server = new ChatServer();
        try {
            server.start();
        }
        catch (IOException ioe) {
            System.err.println("Error starting server: " + ioe);
        }
    }

    // represents a connected client
    private class ClientConnection implements Runnable {
        // client's username
        String username;

        // keep track of running state
        boolean running = true;

        // the client socket
        Socket socket;

        // an incoming message
        String message;

        // I/O streams
        PrintWriter outStream;
        BufferedReader inStream;

        // constructor
        public ClientConnection(Socket sock) {
            this.socket = sock;

            // construct I/O streams
            try {
                outStream = new PrintWriter(socket.getOutputStream());
                inStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            }
            catch (IOException ioe) {
                System.err.println("Error creating client I/O streams: " + ioe);
            }

            // perform username check
            try {
                // if the username request is accepted, add the client to the client list
				String initialRequest = inStream.readLine();
				System.out.println("client: " + initialRequest);
                if (this.usernameRequest(initialRequest)) {
                    clientList.add(this);
                }
            }
            catch (IOException ioe) {
                System.err.println("Error reading username request: " + ioe);
            }
        }

        // this will start the main thread loop
        public void run() {
            while(running) {
                try {
                    // read incoming messages
                    message = inStream.readLine();
					System.out.println("client: " + message);
                    // handle the incoming message based on message type
                    switch (getMessageType(message)) {
                        // client requests username
                        case 0:
                            this.usernameRequest(message);
                            break;
                        // client sends general message
                        case 3:
                            this.sendGeneralMessage(message);
                            break;
                        // client sends private message
                        case 4:
                            this.sendPrivateMessage(message);
                            break;
                        // client sends disconnect message
                        case 7:
                            this.disconnect();
                            break;
						// error getting message type
						case -1:
							System.err.println("Unknown message type. Closing connection");
							this.close();
                        // otherwise, the message is malformed
                        default:
                            System.err.println("Malformed message. Connection will be closed");
                            this.close();
                    }
                }
                catch (IOException ioe) {
                    System.err.println("Error reading from client stream. Connection will be closed");
					this.close();
                }
            }
        }

        // accessor to check if socket is currently connected
        public boolean isConnected() {
            return !socket.isClosed();
        }

        // accessor for username
        public String getUsername() {
            return this.username;
        }

        // method to write a string to the client
        public void write(String msg) throws IOException {
            // make sure the client is still connected
            if (!socket.isConnected()) {
                System.err.println("Unable to write message. Client socket already closed");
                this.close();
            }
            else {
                outStream.write(msg);
                outStream.flush();
            }
        }

        // method to close the client connection
        public boolean close() {
            try {
                if (outStream != null) outStream.close();
                if (inStream != null) inStream.close();
                if (socket != null) socket.close();
                running = false;
                clientList.remove(this);
				// check for duplicate username event
				if (this.getUsername() == null)
					return true;
				// protocol message 9
                broadcast("9 " + this.getUsername() + "\r\n");
                return true;
            }
            catch (IOException ioe) {
                System.err.println("Error closing client connection: " + ioe);
                return false;
            }
        }

        // PROTOCOL METHODS

        // method to handle general message requests
        public void sendGeneralMessage(String msg) throws IOException {
            String message = msg.substring(msg.indexOf(" ") + 1);
            String datetime = getDatetimeGMT();
            // protocol message 5
            broadcast("5 " + this.getUsername() + " " + datetime + " " + message + "\r\n");
        }

        // method to handle private message requests
        public void sendPrivateMessage(String msg) throws IOException {
            String[] msgArray = msg.split(" ", 4);
            String targetUser = msgArray[2];
            String message = msgArray[3];
            String datetime = getDatetimeGMT();
            // find the target client
            for (int i = 0; i < clientList.size(); i++) {
                ClientConnection target = clientList.get(i);
                if (target.getUsername().toLowerCase().equals(targetUser.toLowerCase())) {
                    // protocol message 6
                    target.write("6 " + this.getUsername() + " " + target.getUsername() + " " + datetime + " " + message + "\r\n");
                    this.write("6 " + this.getUsername() + " " + target.getUsername() + " " + datetime + " " + message + "\r\n");
					return;
                }
            }
            // if execution reaches this point, the target client was not found
            System.err.println("Target client not found");
        }

        // method to disconnect with proper protocol
        public boolean disconnect() throws IOException {
			// protocol message 8
            this.write("8\r\n");
            
			boolean closed = this.close();

            if (!closed) {
                System.err.println("Error disconnecting client");
            }

            return closed;
        }

        // parses a username request and sends back the appropriate response
        private boolean usernameRequest(String req) throws IOException {
            // validate request
            if (getMessageType(req) != 0) {
                this.write("Invalid username request. Closing connection");
                this.close();
            }
            else {
                try {
                    String name = req.substring(req.indexOf(" ") + 1);
                    // check for duplicate username
                    for (int i = 0; i < clientList.size(); i++) {
                        if (clientList.get(i).getUsername().toLowerCase().equals(name.toLowerCase())) {
                            // protocol message 2
                            this.write("2\r\n");
                            this.close();
                            return false;
                        }
                    }

                    // otherwise the username is valid
                    // construct username list
                    String usernameList = "";
                    for (int i = 0; i < clientList.size(); i++) {
                        usernameList += clientList.get(i).getUsername();
                        if (i != clientList.size() - 1) usernameList += ",";
                    }
                    // protocol message 1
                    this.write("1 " + usernameList + " " + "Connected to chat\r\n");
                    this.username = name;
                }
                catch(StringIndexOutOfBoundsException iob) {
                    this.write("Invalid username request. Closing connection");
                    this.close();
                    return false;
                }
            }
            return true;
        }

        private int getMessageType(String message) {
            try {
                int type = Character.getNumericValue(message.charAt(0));
                return type;
            }
            catch (StringIndexOutOfBoundsException iob) {
                System.err.println("Invalid Message");
                return -1;
            }
			catch(NullPointerException npe) {
				System.err.println("Null message. Closing connection");
				return -1;
			}
        }
    }
}
