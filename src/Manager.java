import java.io.IOException;
import java.util.logging.*;

import static java.lang.Thread.sleep;

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

            consoleHandler.setLevel(Level.ALL);
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

        try {
            sleep(100);
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "Server interrupted before expected shutdown.");
        } finally {
            if (server != null) {
                server.interrupt();
                logger.log(Level.INFO, "Server says goodbye :(");
            }
        }
    }
}
