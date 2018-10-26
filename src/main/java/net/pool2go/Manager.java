package net.pool2go;

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
            // need to make sure it's always the full path
            if (path.charAt(path.length() - 1) != '\\' && System.getProperty("os.name").toLowerCase().contains("windows"))
                path += "\\";
            // assuming it's running on either mac, linux or bsd
            // if it's haiku: let me know if there's a linux layer like what freebsd has, am interested in haiku
            else if (path.charAt(path.length() - 1) != '/')
                path += "/";

            System.out.println("Running on host type " + System.getProperty("os.name"));
            System.out.println("Running with the following arguments:" + "\n" +
                               "Full path for SQLite DB: " + path + "\n" +
                               "File name for SQLite DB: " + filename + "\n" +
                               "main.java.net.pool2go.Server running on port:  " + Integer.toString(port) + "\n");
        }

        // Start the manager
        Manager manager = null;

        try {
            manager = new Manager(path, filename, port);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Could not instantiate main.java.net.pool2go.Manager.");
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
    }

    /**
     * Manages the main.java.net.pool2go.Server, required for console input on when to stop the server.
     *
     * @param path
     * @param filename
     * @param port
     * @throws Exception
     */
    public Manager(String path, String filename, int port) throws Exception {
        // build logger
        logger = Logger.getLogger(this.getClass().getSimpleName()); // change to another name?
        logger.log(Level.CONFIG, "Current working directory: " + new File(".").getCanonicalPath());

        try {
            loggerFactory();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "IOException thrown building log.", e);
            throw new Exception("Could not build main.java.net.pool2go.Manager log.");
        }

        // create database
        connection = null;

        try {
            dbFactory(path, filename);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Could not create connection to SQL database.");
            throw new Exception("Could not create DB.");
        }
        // manager does't interact with the db, close it when creation was a success
        closeDb();

        // start server
        server = null;

        try {
            server = new Thread(new Server(port, path + filename));
        } catch (IOException e) {
            logger.log(Level.SEVERE, "IOException thrown starting main.java.net.pool2go.Server.", e);
            throw new Exception("Could not start server.");
        }

        logger.log(Level.INFO, "main.java.net.pool2go.Server says hi! :)");
    }

    /**
     * Build a logger for main.java.net.pool2go.Manager.
     *
     * @throws IOException could not create the log file, may not have read/write permissions in the working directory
     */
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

    /**
     * Create the database file if it doesn't exist and write to log database details.
     *
     * @param path full path for the database
     * @param filename full file name for the database
     * @throws SQLException could not connect to the database
     */
    private void dbFactory(String path, String filename) throws SQLException {
        String dbUrl = "jdbc:sqlite:" + path + filename;

        logger.log(Level.CONFIG, "Using custom database file path: " + path);
        logger.log(Level.CONFIG, "Using custom file name for DB: " + filename);
        logger.log(Level.CONFIG, "Full URL for SQLite-JDBC driver: " + dbUrl);

        connection = DriverManager.getConnection(dbUrl);
        logger.log(Level.CONFIG, "Successfully connected to SQLite DB.");
    }

    /**
     * Stop the server. Since Thread.stop() is not recommended, use Thread.interrupt().
     */
    public void stopServer() {
        if (server != null) {
            server.interrupt();
            logger.log(Level.INFO, "main.java.net.pool2go.Server interrupted.");
        }
    }

    /**
     * Close the connection to the database.
     */
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
