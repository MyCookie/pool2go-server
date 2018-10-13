import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.*;

public class Server implements Runnable {

    private ServerSocket listener;
    private Logger logger;

    /**
     * Create a ServerSocket on a given port.
     *
     * @param port
     * @throws IOException
     */
    public Server(int port) throws IOException {
        Handler fileHandler = new FileHandler("logger." + this.getClass().getSimpleName() + ".log");
        fileHandler.setFormatter(new SimpleFormatter());
        Handler consoleHandler = new ConsoleHandler();

        logger = Logger.getLogger(this.getClass().getSimpleName());
        logger.addHandler(consoleHandler);
        logger.addHandler(fileHandler);

        consoleHandler.setLevel(Level.WARNING);
        fileHandler.setLevel(Level.ALL);
        logger.setLevel(Level.ALL);

        logger.log(Level.CONFIG, "Server logger configured");

        listener = new ServerSocket(port);
        logger.log(Level.CONFIG, "Server listener created");
    }

    /**
     * Start the server in another thread.
     */
    public void run() {
        try {
            while (true) {
                Socket socket = listener.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String s = in.readLine();

                logger.log(Level.INFO, "New input received");

                if (Thread.interrupted()) {
                    logger.log(Level.WARNING, "Server interrupted in loop.");
                    return;
                }
            }
        } catch (IOException e) {
            e.getMessage();
        }
    }
}
