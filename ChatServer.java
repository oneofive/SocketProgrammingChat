import java.io.*;
import java.net.*;
import java.util.*;

/**
 * A multithreaded chat room server.  When a client connects the
 * server requests a screen name by sending the client the
 * text "SUBMITNAME", and keeps requesting a name until
 * a unique one is received.  After a client submits a unique
 * name, the server acknowledges with "NAMEACCEPTED".  Then
 * all messages from that client will be broadcast to all other
 * clients that have submitted a unique screen name.  The
 * broadcast messages are prefixed with "MESSAGE ".
 *
 * Because this is just a teaching example to illustrate a simple
 * chat server, there are a few features that have been left out.
 * Two are very useful and belong in production code:
 *
 *     1. The protocol should be enhanced so that the client can
 *        send clean disconnect messages to the server.
 *
 *     2. The server should do some logging.
 */

public class ChatServer {
    /**
     * The port that the server listens on.
     */
    private static final int PORT = 9001;
    /**
     * The set of all names of clients in the chat room.  Maintained
     * so that we can check that new clients are not registering name
     * already in use.
     */
    private static HashSet<String> names = new HashSet<>();
    /**
     * The set of all the print writers for all the clients.  This
     * set is kept so we can easily broadcast messages.
     */
    private static HashSet<PrintWriter> writers = new HashSet<>();
    private static HashMap<String, PrintWriter> whisperTargets = new HashMap<>(); // Store Whisper targets
    /**
     * The appplication main method, which just listens on a port and
     * spawns handler threads.
     */

    public static void main(String[] args) throws Exception {
        System.out.println("The chat server is running.");
        ServerSocket listener = new ServerSocket(PORT);
        try {
            while (true) {
                new Handler(listener.accept()).start();
            }
        } finally {
            listener.close();
        }
    }
    /**
     * A handler thread class.  Handlers are spawned from the listening
     * loop and are responsible for a dealing with a single client
     * and broadcasting its messages.
     */
    private static class Handler extends Thread {
        private String name;
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        /**
         * Constructs a handler thread, squirreling away the socket.
         * All the interesting work is done in the run method.
         */
        public Handler(Socket socket) {
            this.socket = socket;
        }
        /**
         * Services this thread's client by repeatedly requesting a
         * screen name until a unique one has been submitted, then
         * acknowledges the name and registers the output stream for
         * the client in a global set, then repeatedly gets inputs and
         * broadcasts them.
         */
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                // Request a name from this client.  Keep requesting until
                // a name is submitted that is not already used.  Note that
                // checking for the existence of a name and adding the name
                // must be done while locking the set of names.
                while (true) {
                    out.println("SUBMITNAME");
                    name = in.readLine();
                    if (name == null) {
                        return;
                    }
                    synchronized (names) {
                        if (!names.contains(name)) {
                            names.add(name);
                            break;
                        }
                    }
                }
                // Now that a successful name has been chosen, add the
                // socket's print writer to the set of all writers so
                // this client can receive broadcast messages.
                out.println("NAMEACCEPTED");
                writers.add(out);
                whisperTargets.put(name, out); // Store client's PrintWriter for Whisper
                // Accept messages from this client and broadcast them.
                // Ignore other clients that cannot be broadcasted to.
                while (true) {
                    String input = in.readLine();
                    if (input == null) {
                        return;
                    }
                    if (input.startsWith("<") && input.contains("/>")) {
                        // Whisper message format: <targetUser/> message
                        String targetUser = input.substring(1, input.indexOf("/>"));
                        String message = input.substring(input.indexOf("/>") + 2);
                        sendWhisper(name, targetUser, message);
                    } else {
                        broadcast(name + ": " + input);
                    }
                }
            } catch (IOException e) {
                System.out.println(e);
            } finally {
                // This client is going down!  Remove its name and its print
                // writer from the sets, and close its socket.
                if (name != null) {
                    names.remove(name);
                }
                if (out != null) {
                    writers.remove(out);
                    whisperTargets.remove(name);
                }
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }
        /**
         * Broadcasts a message to all clients.
         */
        private void broadcast(String message) {
            for (PrintWriter writer : writers) {
                writer.println("MESSAGE " + message);
            }
        }
        /**
         * Sends a whisper message to a specific client.
         */
        private void sendWhisper(String sender, String targetUser, String message) {
            PrintWriter targetWriter = whisperTargets.get(targetUser);
            if (targetWriter != null) {
                targetWriter.println("MESSAGE [Whisper from " + sender + "]: " + message);
            }
        }
    }
}