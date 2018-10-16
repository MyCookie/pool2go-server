import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Scanner;
import java.util.logging.*;

/**
 * Main
 */
public class Manager {

    // TODO: replace static objects with reading arguments
    public static final int ARG_DB_PATH = 0;
    public static final int ARG_PORT = 1;

    public static void main(String [] argv) {

        // build logger
        Logger logger = Logger.getLogger((new Manager()).getClass().getSimpleName()); // change to another name?

        try {
            loggerFactory(logger);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "IOException thrown building log.", e);
            return;
        }

        // create database
        Connection connection = null;

        try {
            dbFactory(argv, logger, connection);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Could not build database.");
            return;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Could not create connection to SQL database.");
            return;
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

        if (server != null) {
            server.interrupt();
            logger.log(Level.INFO, "Server interrupted.");
            System.out.println("Goodbye server :(");
        }

        // inspector goofing here, if there is a problem creating it, it stops exec; fairly safe to ignore warning
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Could not close connection to database.", e);
            }
        }
    }

    private static void loggerFactory(Logger logger) throws IOException {
        Handler consoleHandler;
        Handler fileHandler;

        consoleHandler = new ConsoleHandler();
        fileHandler = new FileHandler("logger." + Manager.class.getSimpleName() + ".log");
        fileHandler.setFormatter(new SimpleFormatter());

        logger.addHandler(consoleHandler);
        logger.addHandler(fileHandler);

        consoleHandler.setLevel(Level.WARNING);
        fileHandler.setLevel(Level.ALL);
        logger.setLevel(Level.ALL);

        logger.config("Logger configuration finished.");
    }

    private static void dbFactory(String [] argv, Logger logger, Connection connection) throws IOException, SQLException {
        String dbFilePath = null;
        String dbUrl = "jdbc:sqlite:";

        logger.log(Level.CONFIG, "Current working directory: " + new File(".").getCanonicalPath());

        if (argv.length > 0) {
            dbFilePath = argv[ARG_DB_PATH];
            logger.log(Level.CONFIG, "Using custom database file path: " + dbFilePath);
        } else {
            try {
                dbFilePath = new File(".").getCanonicalPath();
                logger.log(Level.CONFIG, "Using current working directory as database file path: " + dbFilePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        dbUrl += dbFilePath + "pool2go.db";

        connection = DriverManager.getConnection(dbUrl);
    }

    private static void startServer() {
        //
    }
}
