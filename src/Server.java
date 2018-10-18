import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.util.Calendar;
import java.util.logging.*;

public class Server implements Runnable {

    private ServerSocket listener;
    private Logger logger;
    private String dbUrl;
    private Connection connection;

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

        dbUrl = "jdbc:sqlite:" + databaseUrl;

        try {
            buildDb();
        } catch (SQLException e) {
            throw new IOException("Could not create database.");
        }

        listener = new ServerSocket(port);
        logger.log(Level.CONFIG, "Server listener created on port: " + port);
    }

    private void buildDb() throws SQLException {
        try {
            connection = DriverManager.getConnection(dbUrl);
            logger.log(Level.CONFIG, "Registered database at: " + dbUrl);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Could not create connection to given database.");
            throw new SQLException(e);
        }

        try {
            String sqlCreateTable = "CREATE TABLE IF NOT EXISTS Locations (\n" +
                    " key text PRIMARY KEY,\n" +
                    " latitude real,\n" +
                    " longitude real\n" +
                    ");";
            connection.createStatement().execute(sqlCreateTable);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Could not create Locations table in database.");
            throw new SQLException(e);
        }
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
     * Insert and updated LocationObject. Find any existing location with the same key and remove it first.
     *
     * @param locationObject the updated location
     * @throws SQLException
     */
    private void findAndInsertLocation(LocationObject locationObject) throws SQLException {
        // TODO: find existing information first
        String sqlInsertLocation = "INSERT INTO Locations(key, latitude, longitude) VALUES(?,?)";

        PreparedStatement statement = connection.prepareStatement(sqlInsertLocation);
        statement.setString(1, locationObject.getKey());
        statement.setDouble(2, locationObject.getLatitude());
        statement.setDouble(3, locationObject.getLongitude());

        statement.executeUpdate();
    }

    /**
     * Start the server in another thread.
     */
    public void run() {
        try {
            while (true) {
                // cannot check at the end if an exception is thrown and execution continues
                if (Thread.interrupted()) {
                    logger.log(Level.WARNING, "Server interrupted in loop.");
                    connection.close();
                    return;
                }

                Socket socket = listener.accept();

                // grab client info
                InetAddress clientAddr = socket.getInetAddress();
                StringBuilder stringBuilder = new StringBuilder();
                for (byte b : clientAddr.getAddress())
                    stringBuilder.append(b);
                String ip = stringBuilder.toString();
                logger.log(Level.INFO, "New connection opened with client at: " + ip);

                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                LocationObject locationObject;

                try {
                    locationObject = (LocationObject) in.readObject();
                } catch (ClassNotFoundException e) {
                    logger.log(Level.WARNING, "Client sent wrong object type.");
                    out.writeObject(new LocationObject(-90, -90)); // send out-of-bounds to signify error
                    continue; // don't want to stop the server for a single incorrect input
                }

                if (locationObject.getKey() == null) {
                    String key = Calendar.getInstance().getTime().toString() + " | " + ip;
                    locationObject.setKey(key);
                }

                try {
                    findAndInsertLocation(locationObject);
                } catch (SQLException e) {
                    logger.log(Level.SEVERE, "Could not insert new location into database.");
                    out.writeObject(new LocationObject(-90, -90)); // send out-of-bounds to signify error
                    continue;
                }

                // TODO: find closest location object and send that
                out.writeObject(locationObject);
            }
        } catch (IOException e) {
            e.getMessage();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Could not close connection to SQLite DB.");
        }
    }
}
