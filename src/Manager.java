import java.io.IOException;
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

            consoleHandler.setLevel(Level.ALL);
            fileHandler.setLevel(Level.ALL);
            logger.setLevel(Level.ALL);

            logger.config("Logger configuration finished.");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "IOException thrown building log.", e);
        }

        logger.log(Level.INFO, "Manager says hi! :)");
    }
}
