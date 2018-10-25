import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.logging.*;

/**
 * <p>A simple I/O server that communicates in LocationObjects and writes updates to a SQLite database.</p>
 *
 * <p>If there are any runtime exceptions while the server is running, it will do it's best to continue execution. If it
 * needs to communicate to a client during this, it will send an out-of-bounds location of (360, 360) with a null key.</p>
 *
 * <p>The server will require the full path and filename for the SQLite database, see the constructor for details.</p>
 *
 * <p>It also requires read/write permissions for the directory it's in for it's log file.</p>
 *
 * <p>The server will require the client to perform a handshake first, in the form of:
 * <ul>
 *     <li>Client opens a socket</li>
 *     <li>Server sends an empty LocationObject with the client's unique key</li>
 *     <li>Client sends a LocationObject with it's unique key provided by the server</li>
 *     <ul>
 *         <li>Notice the server does not check for the other fields of the Location Object send, only the key</li>
 *         <li>If the client sends the wrong key, it gets an additional 5 attempts to send the right key</li>
 *     </ul>
 * </ul></p>
 *
 * <p>This is not meant to ensure a private connection, but merely a mutually assured stable one.</p>
 *
 * <p>The client is then required to do the following:
 * <ul>
 *     <li>Send it's updated location in a LocationObject with it's provided key</li>
 *     <li>The server does its job: updating the client's location in the database, and searching for any other locations
 *     with a different key within a 200 meter radius.</li>
 *     <ul>
 *         <li>If at any point during this the server encounters an error, it will record it and send an out-of-bounds
 *         location (as described above) to the client.</li>
 *         <li>If the client receives this location, it is to assume that the server has stopped execution on it's end,
 *         and is expected to sever the connection until it's next update.</li>
 *     </ul>
 *     <li>Currently the server will finally send one of two locations:</li>
 *     <ul>
 *         <li>A 'null location' if it cannot find any nearby locations.</li>
 *         <li>A single location within 200 meters of the client's reported location.</li>
 *     </ul>
 * </ul></p>
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

    private static final LocationObject OUT_OF_BOUNDS_LOCATION = new LocationObject(OUT_OF_BOUNDS_LATITUDE, OUT_OF_BOUNDS_LONGITUDE);

    /**
     * <p>Create a ServerSocket on a given port, and use a given database.</p>
     *
     * <p>A full path and file name is required for the database location. For example: C:\ServerFiles\Databases\Database.sqlite,
     * or: /server_raid/databases/database.sqlite.</p>
     *
     * <p>An IOException is thrown if there was any problems/Exceptions thrown when building the Server. See logs for details.</p>
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
     * <p>Find the nearest locations within 200 meters of a given location. Does a simple, and very inefficient, check over
     * all locations stored in the database that do not have the same key as the location to compare to.</p>
     *
     * <p>If no locations are found, clear the list.</p>
     *
     * <p>TODO: given time, find a way to speed up this process.</p>
     *
     * @param locationObject the location to compare to
     * @param locationObjects if a close location is found, put it in here
     * @throws SQLException if the database cannot be accessed
     */
    private void findNearestLocations(LocationObject locationObject,
                                      ArrayList<LocationObject> locationObjects) throws SQLException {
        String sqlGetAllRecordsNotOfClientKey = "SELECT key, latitude, longitude FROM Locations WHERE key <> ?";
        PreparedStatement statement = connection.prepareStatement(sqlGetAllRecordsNotOfClientKey);
        statement.setString(1, locationObject.getKey());
        ResultSet resultSet = statement.executeQuery();

        double resultLat = 0;
        double resultLng = 0;

        if (resultSet.isBeforeFirst()) {
            // no other clients exist, empty the list
            locationObjects.clear();
        } else {
            while (resultSet.next()) {
                // https://en.wikipedia.org/wiki/Decimal_degrees
                // 0.005 ~ 200 meters
                resultLat = resultSet.getDouble("latitude");
                if (Math.abs(resultLat - locationObject.getLatitude()) < 0.005) {
                    // within latitude bounds
                    resultLng = resultSet.getDouble("longitude");
                    if (Math.abs(resultLng - locationObject.getLongitude()) < 0.005) {
                        // within longitude bounds
                        locationObjects.add(new LocationObject(locationObject.getKey(), resultLat, resultLng));
                    }
                }
            }
        }
    }

    /**
     * <p>Start the server in another thread.</p>
     *
     * <p>A runtime exception should not cause the thread to stop, but that means that the interrupt check will need to
     * happen first.</p>
     */
    public void run() {
        try {
            while (true) {
                // cannot check at the end if an exception is thrown and execution continues
                if (Thread.interrupted()) {
                    logger.log(Level.WARNING, "Server interrupted in loop.");
                    listener.close(); // close the socket when stopping
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
                    logger.log(Level.WARNING, "Client " + ip + " sent wrong object type.");
                    out.writeObject(OUT_OF_BOUNDS_LOCATION);
                    socket.close();
                    continue;
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Failed handshake with " + ip);
                    out.writeObject(OUT_OF_BOUNDS_LOCATION);
                    socket.close();
                    continue;
                }

                try {
                    locationObject = (LocationObject) in.readObject();
                } catch (ClassNotFoundException e) {
                    logger.log(Level.WARNING, "Client sent wrong object type.");
                    out.writeObject(OUT_OF_BOUNDS_LOCATION);
                    socket.close();
                    continue; // don't want to stop the server for a single incorrect input
                }

                if (locationObject.getKey() == null) locationObject.setKey(key);

                try {
                    findAndInsertLocation(locationObject);
                } catch (SQLException e) {
                    logger.log(Level.SEVERE, "Could not insert new location into database.");
                    out.writeObject(OUT_OF_BOUNDS_LOCATION);
                    socket.close();
                    continue; // don't want to stop the server to avoid crashing the client
                }

                // TODO: send first location found for now, come back after designing the way to send all
                ArrayList<LocationObject> locationObjects = new ArrayList<>(1);
                findNearestLocations(locationObject, locationObjects);
                if (locationObjects.isEmpty())
                    out.writeObject(OUT_OF_BOUNDS_LOCATION);
                else
                    out.writeObject(locationObjects.get(1));
            }
        } catch (IOException e) {
            e.getMessage();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Could not close connection to SQLite DB.");
        }
    }
}
