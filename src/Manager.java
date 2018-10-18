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

    private static final int ARG_DB_PATH = 0;
    private static final int ARG_DB_NAME = 2;
    private static final int ARG_PORT = 4;

    public static void main(String [] argv) {

        /* remove comment block to check argv contents unabridged
        String s = "";
        for (String i : argv)
            s += i + " ";
        System.out.println(s);
        */

        String path = null;
        String filename = null;
        int port = 0;

        // parse arguments
        boolean valid_args = true;

        if (argv.length > 0) {
            if (argv[ARG_DB_PATH].equals("--help") || argv[ARG_DB_PATH].equals("-h")) valid_args = false;

            if (argv[ARG_DB_PATH].equals("--path") || argv[ARG_DB_PATH].equals("-a"))
                path = argv[ARG_DB_PATH + 1];
            else
                valid_args = false;

            if (argv[ARG_DB_NAME].equals("--filename") || argv[ARG_DB_NAME].equals("-f"))
                filename = argv[ARG_DB_NAME + 1];
            else
                valid_args = false;

            if (argv[ARG_PORT].equals("--port") || argv[ARG_PORT].equals("-p"))
                port = Integer.parseInt(argv[ARG_PORT + 1]);
            else
                valid_args = false;
        } else
            valid_args = false;

        if (!valid_args) {
            System.out.println("Invalid arguments.\n" +
                               "Valid arguments:" + "\n" +
                               "    --path, -a                full path for the SQLite database" + "\n" +
                               "    --filename, -f            full name (including extension) for the SQLite database" + "\n" +
                               "    --port, -p                port number for the server");
            return;
        } else {
            System.out.println("Running with the following arguments:" + "\n" +
                               "Full path for SQLite DB: " + path + "\n" +
                               "File name for SQLite DB: " + filename + "\n" +
                               "Server running on port:  " + Integer.toString(port) + "\n");
        }

        // Start the manager
        Manager manager = null;

        try {
            manager = new Manager(path, filename, port);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Could not instantiate Manager.");
            return;
        }

        Scanner in = new Scanner(System.in);
        System.out.println("Type 'quit' to stop server");

        boolean done = false;
        while(!done && in.hasNextLine()) {
            if (in.nextLine().toLowerCase().trim().equals("quit"))
                done = true;
        }

        manager.stopServer();
        manager.closeDb();
    }

    public Manager(String path, String filename, int port) throws Exception {
        // build logger
        logger = Logger.getLogger(this.getClass().getSimpleName()); // change to another name?
        logger.log(Level.CONFIG, "Current working directory: " + new File(".").getCanonicalPath());

        try {
            loggerFactory();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "IOException thrown building log.", e);
            throw new Exception("Could not build Manager log.");
        }

        // create database
        connection = null;

        try {
            dbFactory(path, filename);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Could not create connection to SQL database.");
            throw new Exception("Could not create DB.");
        }

        // start server
        server = null;

        try {
            server = new Thread(new Server(port, path + filename));
        } catch (IOException e) {
            logger.log(Level.SEVERE, "IOException thrown starting Server.", e);
            throw new Exception("Could not start server.");
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

    private void dbFactory(String path, String filename) throws SQLException {
        String dbUrl = "jdbc:sqlite:" + path + filename;

        logger.log(Level.CONFIG, "Using custom database file path: " + path);
        logger.log(Level.CONFIG, "Using custom file name for DB: " + filename);
        logger.log(Level.CONFIG, "Full URL for SQLite-JDBC driver: " + dbUrl);

        connection = DriverManager.getConnection(dbUrl);
        logger.log(Level.CONFIG, "Successfully connected to SQLite DB.");
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
