package net.pool2go;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.*;

/**
 * Manages the server
 */
public class Manager {

    private Logger logger;
    private Connection connection;
    private Thread server;

    /**
     * Manages the Server, required for console input on when to stop the server.
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
            throw new Exception("Could not build Manager log.");
        }

        // create database
        connection = null;

        try {
            dbFactory(path, filename);
            connection.close();
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

    /**
     * Build a logger for Manager.
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
            logger.log(Level.INFO, "Server interrupted.");
        }
    }
}
