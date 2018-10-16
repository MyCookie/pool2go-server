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

    private Logger logger;
    private Connection connection;
    private Thread server;

    // TODO: replace static objects with reading arguments
    public static final int ARG_DB_PATH = 0;
    public static final int ARG_PORT = 1;

    public static void main(String [] argv) {

        Manager manager = new Manager(argv);

        Scanner in = new Scanner(System.in);
        System.out.println("Server says hi! :) Type 'quit' to stop server");

        boolean done = false;
        while(!done && in.hasNextLine()) {
            if (in.nextLine().toLowerCase().trim().equals("quit"))
                done = true;
        }

        manager.stopServer();
        manager.closeDb();
    }

    public Manager(String [] args) {
        // build logger
        logger = Logger.getLogger(this.getClass().getSimpleName()); // change to another name?

        try {
            loggerFactory();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "IOException thrown building log.", e);
            return;
        }

        // create database
        connection = null;

        try {
            dbFactory(args);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Could not build database.");
            return;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Could not create connection to SQL database.");
            return;
        }

        // start server
        server = null;

        try {
            server = new Thread(new Server(8080));
        } catch (IOException e) {
            logger.log(Level.SEVERE, "IOException thrown starting Server.", e);
        }

        logger.log(Level.INFO, "Server says hi! :)");
    }

    private void loggerFactory() throws IOException {
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

    private void dbFactory(String [] args) throws IOException, SQLException {
        String dbFilePath = null;
        String dbUrl = "jdbc:sqlite:";

        logger.log(Level.CONFIG, "Current working directory: " + new File(".").getCanonicalPath());

        if (args.length > 0) {
            dbFilePath = args[ARG_DB_PATH];
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

    public void stopServer() {
        if (server != null) {
            server.interrupt();
            logger.log(Level.INFO, "Server interrupted.");
        }
    }

    public void closeDb() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Could not close connection to database.", e);
            }
        }
    }
}
