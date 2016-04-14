/**
 * A class representing a chat server
 *
 * Created by JeremyD on 4/13/2016.
 */

import java.net.*;
import java.io.*;
import java.util.concurrent.*;

public class ChatServer {
    public static final int port = 1337;

    // the thread pool
    public static final Executor exec = Executors.newCachedThreadPool();

    public static void main(String[] args) throws IOException {
        // the server
        ServerSocket sock = null;

        try {
            sock = new ServerSocket(port);

            // keep track of running state
            boolean running = true;

            // the server loop
            while(running) {
                Socket client = sock.accept();
                Runnable clientTask = new ClientConnection(client);
            }
        }
        catch (IOException ioe) {
            System.err.println(ioe);
        }
        finally {
            if (sock != null) {
                sock.close();
            }
        }
    }

}
