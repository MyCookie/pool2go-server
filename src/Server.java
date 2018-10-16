import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.*;

public class Server implements Runnable {

    private ServerSocket listener;
    private Logger logger;
    private String dbUrl;
    private Connection connection;

    /**
     * Create a ServerSocket on a given port.
     *
     * @param port
     * @throws IOException
     */
    public Server(int port) throws IOException {
        try {
            loggerFactory();
        } catch (IOException e) {
            throw new IOException("Could not build Server Logger.");
        }

        try {
            dbFactory();
            logger.log(Level.CONFIG, "No database URL passed, created one at: " + dbUrl);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "IOException thrown creating database.", e);
            throw new IOException("Could not create database");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "SQLException thrown creating database.", e);
            throw new IOException("Could not create database.");
        }

        listener = new ServerSocket(port);
        logger.log(Level.CONFIG, "Server listener created on port: " + port);
    }

    /**
     * Create a ServerSocket on a given port, and use a given database.
     *
     * @param port
     * @param databaseUrl
     * @throws IOException
     */
    public Server(int port, String databaseUrl) throws IOException {
        try {
            loggerFactory();
        } catch (IOException e) {
            throw new IOException("Could not build Server Logger.");
        }

        try {
            dbUrl = databaseUrl;
            connection = DriverManager.getConnection(dbUrl);
            logger.log(Level.CONFIG, "Registered database at: " + dbUrl);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Could not create connection to given database.");
            throw new IOException("Could not create database.");
        }

        listener = new ServerSocket(port);
        logger.log(Level.CONFIG, "Server listener created on port: " + port);
    }

    /**
     * Build a logger.
     *
     * @throws IOException
     */
    private void loggerFactory() throws IOException {
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
    }

    /**
     * Create a SQLite database in the current working directory.
     *
     * @throws SQLException
     * @throws IOException
     */
    private void dbFactory() throws SQLException, IOException {
        logger.log(Level.CONFIG, "Current working directory: " + new File(".").getCanonicalPath());

        String dbFilePath = new File(".").getCanonicalPath() + "server_sqlite.db";

        dbUrl = "jdbc:sqlite:" + dbFilePath;

        connection = DriverManager.getConnection(dbUrl);
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
