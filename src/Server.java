import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.util.Calendar;
import java.util.logging.*;

/**
 * A simple I/O server that communicates in LocationObjects and writes updates to a SQLite database.
 *
 * If there are any runtime exceptions while the server is running, it will do it's best to continue execution. If it
 * needs to communicate to a client during this, it will send an out-of-bounds location of (360, 360) with a null key.
 *
 * The server will require the full path and filename for the SQLite database, see the constructor for details.
 *
 * It also requires read/write permissions for the directory it's in for it's log file.
 *
 * @see LocationObject
 */
public class Server implements Runnable {

    private static final double OUT_OF_BOUNDS_LATITUDE = 360;
    private static final double OUT_OF_BOUNDS_LONGITUDE = 360;

    private ServerSocket listener;
    private Logger logger;
    private String dbUrl;
    private Connection connection;

    private static final LocationObject NULL_LOCATION = new LocationObject(OUT_OF_BOUNDS_LATITUDE, OUT_OF_BOUNDS_LONGITUDE);

    /**
     * Create a ServerSocket on a given port, and use a given database.
     *
     * A full path and file name is required for the database location. For example: C:\ServerFiles\Databases\Database.sqlite,
     * or: /server_raid/databases/database.sqlite.
     *
     * An IOException is thrown if there was any problems/Exceptions thrown when building the Server. See logs for details.
     *
     * @param port port for the Server to run on
     * @param databaseUrl full path and filename for the database
     * @throws IOException generic exception when some exception occurred when building the server parts
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

    /**
     * Connect to the database and create the Locations table.
     *
     * @throws SQLException may either mean a connection failure or a table creation failure, see logs for details
     */
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
     * Build a logger. Requires read/write permissions in the directory this is in.
     *
     * @throws IOException cannot write files to the current working directory
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
     * Insert an updated location. Search for an existing record with the key; if none found, insert a new record,
     * if a key exists, update the record.
     *
     * @param locationObject the updated location
     * @throws SQLException could not write to the database
     */
    private void findAndInsertLocation(LocationObject locationObject) throws SQLException {
        // find an existing location entry with key
        String sqlFindExistingKey = "SELECT key FROM Locations WHERE key = ?";
        PreparedStatement statement = connection.prepareStatement(sqlFindExistingKey);
        statement.setString(1, locationObject.getKey());
        ResultSet resultSet = statement.executeQuery();

        // easier to check if there is a first entry
        // https://stackoverflow.com/questions/867194/java-resultset-how-to-check-if-there-are-any-results
        if (resultSet.isBeforeFirst()) { // no location entry for key, insert a new record
            String sqlInsertLocation = "INSERT INTO Locations(key, latitude, longitude) VALUES(?,?,?)";

            statement = connection.prepareStatement(sqlInsertLocation);
            statement.setString(1, locationObject.getKey());
            statement.setDouble(2, locationObject.getLatitude());
            statement.setDouble(3, locationObject.getLongitude());

            statement.executeUpdate();
        } else { // if an entry exists, update it
            String sqlUpdateExistingRecord = "UPDATE Locations\n" +
                    "SET latitude = ?,\n" +
                    "    longitude = ?\n" +
                    "WHERE key = ?";

            statement = connection.prepareStatement(sqlUpdateExistingRecord);
            statement.setDouble(1, locationObject.getLatitude());
            statement.setDouble(2, locationObject.getLongitude());
            statement.setString(3, locationObject.getKey());

            statement.executeUpdate();
        }
    }

    /**
     * Start the server in another thread.
     *
     * A runtime exception should not cause the thread to stop, but that means that the interrupt check will need to
     * happen first.
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

                String key = Calendar.getInstance().getTime().toString() + " | " + ip;

                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                LocationObject locationObject;

                // perform a handshake with the server
                try {
                    out.writeObject(new LocationObject(key, OUT_OF_BOUNDS_LATITUDE, OUT_OF_BOUNDS_LONGITUDE));
                    locationObject = (LocationObject) in.readObject();
                    int count = 6; // client gets 5 chances
                    while (!locationObject.getKey().equals(key) && count > 0) {
                        out.writeObject(new LocationObject(key, OUT_OF_BOUNDS_LATITUDE, OUT_OF_BOUNDS_LONGITUDE));
                        locationObject = (LocationObject) in.readObject();
                        --count;
                    }
                    if (count == 0) throw new Exception("Could not perform a handshake with the server.");
                } catch (ClassNotFoundException e) {
                    logger.log(Level.WARNING, "Client" + ip + " sent wrong object type.");
                    out.writeObject(NULL_LOCATION);
                    continue;
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Failed handshake with " + ip);
                    out.writeObject(NULL_LOCATION);
                    continue;
                }

                try {
                    locationObject = (LocationObject) in.readObject();
                } catch (ClassNotFoundException e) {
                    logger.log(Level.WARNING, "Client sent wrong object type.");
                    out.writeObject(NULL_LOCATION);
                    continue; // don't want to stop the server for a single incorrect input
                }

                if (locationObject.getKey() == null) locationObject.setKey(key);

                try {
                    findAndInsertLocation(locationObject);
                } catch (SQLException e) {
                    logger.log(Level.SEVERE, "Could not insert new location into database.");
                    out.writeObject(NULL_LOCATION);
                    continue; // don't want to stop the server to avoid crashing the client
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
