/**
 * A class representing a chat server
 *
 * Created by JeremyD on 4/13/2016.
 */

import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;

import java.net.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.ArrayList;

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
                // check to make sure client is connected
                if (clientTask.isConnected())
                    exec.execute(clientTask);
            }
        }
        catch (IOException ioe) {
            System.err.println(ioe);
        }
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
                if (this.usernameRequest(inStream.readLine())) {
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
                    // handle the incoming message based on message type
                    switch (getMessageType(message)) {
                        // client requests username
                        case 0:
                            usernameRequest(message);
                            break;
                        // client sends general message
                        case 3:
                            break;
                        // client sends private message
                        case 4:
                            break;
                        // client sends disconnect message
                        case 7:
                            break;
                        // otherwise, the message is malformed
                        default:
                            this.write("Malformed message. Connection will be closed");
                            this.close();
                    }
                }
                catch (IOException ioe) {
                    System.err.println("Unable to read message: " + ioe);
                }
            }
        }

        // accessor to check if connected
        public boolean isConnected() {
            return socket.isConnected();
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
        public void close() {
            try {
                if (outStream != null) outStream.close();
                if (inStream != null) inStream.close();
                if (socket != null) socket.close();
                running = false;
            }
            catch (IOException ioe) {
                System.err.println("Error closing client connection: " + ioe);
            }
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
                        if (clientList.get(i).getUsername().equals(name)) {
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
                    // send response message
                    outStream.write("1 " + usernameList + " " + "Welcome to the chat!\r\n");
                    this.username = name;
                }
                catch(StringIndexOutOfBoundsException iob) {
                    outStream.write("Invalid username request. Closing connection");
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
        }
    }
}
