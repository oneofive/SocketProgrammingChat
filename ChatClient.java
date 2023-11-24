import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class ChatClient {

    BufferedReader in;
    PrintWriter out;
    JFrame frame = new JFrame("Chatter");
    JTextField textField = new JTextField(40);
    JTextArea messageArea = new JTextArea(8, 40);
    JButton whisperButton = new JButton("Whisper");
    String myName; // Store client's name
    private static final String CONFIG_FILE_PATH = "serverinfo.dat";

    /**
     * Constructs the client by laying out the GUI and registering a
     * listener with the textfield so that pressing Return in the
     * listener sends the textfield contents to the server.  Note
     * however that the textfield is initially NOT editable, and
     * only becomes editable AFTER the client receives the NAMEACCEPTED
     * message from the server.
     */
    public ChatClient() {
        // Layout GUI
        textField.setEditable(false);
        messageArea.setEditable(false);
        frame.getContentPane().add(textField, "North");
        frame.getContentPane().add(new JScrollPane(messageArea), "Center");
        frame.getContentPane().add(whisperButton, "South");
        frame.pack();

        // Add Listeners
        textField.addActionListener(new ActionListener() {
            /**
             * Responds to pressing the enter key in the textfield by sending
             * the contents of the text field to the server.    Then clear
             * the text area in preparation for the next message.
             */
            public void actionPerformed(ActionEvent e) {
                sendMessage(textField.getText());
                textField.setText("");
            }
        });
        whisperButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Prompt the user for the target user and whisper message
                String targetUser = JOptionPane.showInputDialog(frame, "Enter target user for Whisper:");
                if (targetUser != null && !targetUser.isEmpty()) {
                    String message = JOptionPane.showInputDialog(frame, "Enter Whisper message:");
                    if (message != null && !message.isEmpty()) {
                        // Send a whisper message to the server
                        sendMessage("<" + targetUser + "/>" + message);
                    }
                }
            }
        });
    }

    /**
     * Prompt for and return the address of the server.
     * If the configuration file exists, read the server address from the file.
     * If the file does not exist, set default information (e.g., localhost, port 9001).
     */
    private String getServerAddress() {
        File configFile = new File(CONFIG_FILE_PATH);
        if (configFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
                return reader.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // If the file does not exist or an error occurs, use default information
        return "localhost"; // Default server address
    }

    /**
     * Connects to the server then enters the processing loop.
     */
    private void run() throws IOException {
        String serverAddress = getServerAddress();
        Socket socket = new Socket(serverAddress, 9001);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        while (true) {
            String line = in.readLine();
            if (line.startsWith("SUBMITNAME")) {
                // Prompt the user for a screen name and send it to the server
                myName = getName();
                out.println(myName);
                frame.setTitle("Chatter - " + myName); // Set frame title with client's name
            } else if (line.startsWith("NAMEACCEPTED")) {
                // Enable textfield for user input after the server accepts the name
                textField.setEditable(true);
            } else if (line.startsWith("MESSAGE")) {
                // Display the received message in the message area
                displayMessage(line.substring(8));
            }
        }
    }

    /**
     * Send a message to the server with the current timestamp.
     */
    private void sendMessage(String message) {
        out.println(message + " " + getCurrentTime());
    }

    /**
     * Display a message in the message area.
     */
    private void displayMessage(String message) {
        messageArea.append(message + "\n");
    }

    /**
     * Get the current time in the "HH:mm:ss" format.
     */
    private String getCurrentTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        return "[" + dateFormat.format(new Date()) + "]";
    }

    /**
     * Prompt for and return the desired screen name.
     */
    private String getName() {
        return JOptionPane.showInputDialog(frame, "Choose a screen name:", "Screen name selection",
                JOptionPane.PLAIN_MESSAGE);
    }

    /**
     * Runs the client as an application with a closeable frame.
     */
    public static void main(String[] args) throws Exception {
        ChatClient client = new ChatClient();
        client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        client.frame.setVisible(true);
        client.run();
    }
}
