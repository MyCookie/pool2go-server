import java.io.IOException;
import java.util.Scanner;
import java.util.logging.*;

/**
 * Main
 */
public class Manager {

    public static void main(String [] argv) {

        // build logger
        Logger logger = Logger.getLogger((new Manager()).getClass().getSimpleName()); // change to another name?
        Handler consoleHandler;
        Handler fileHandler;

        try {
            consoleHandler = new ConsoleHandler();
            fileHandler = new FileHandler("logger." + Manager.class.getSimpleName() + ".log");
            fileHandler.setFormatter(new SimpleFormatter());

            logger.addHandler(consoleHandler);
            logger.addHandler(fileHandler);

            consoleHandler.setLevel(Level.WARNING);
            fileHandler.setLevel(Level.ALL);
            logger.setLevel(Level.ALL);

            logger.config("Logger configuration finished.");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "IOException thrown building log.", e);
        }

        Thread server = null;

        try {
            server = new Thread(new Server(8080));
        } catch (IOException e) {
            logger.log(Level.SEVERE, "IOException thrown starting Server.", e);
        }

        logger.log(Level.INFO, "Server says hi! :)");

        Scanner in = new Scanner(System.in);
        System.out.println("Server says hi! :) Type 'quit' to stop server");

        boolean done = false;
        while(!done && in.hasNextLine()) {
            if (in.nextLine().toLowerCase().trim().equals("quit"))
                done = true;
        }

        server.interrupt();

        logger.log(Level.INFO, "Server interrupted.");
        System.out.println("Goodbye server :(");
    }
}
