package net.pool2go;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

class ServerTest {

    private static String currentWorkingDirectory;
    private static String databaseFileName = "/pool2go_test.sqlite";

    private final int TEST_PORT = 8082;

    private Thread server;

    @BeforeAll
    static void buildDepends() {
        try {
            currentWorkingDirectory = new File(".").getCanonicalPath();
        } catch (IOException e) {
            System.out.println("Do not have read/write permissions in " + currentWorkingDirectory);
        }

        // clear the database before running a new batch of tests
        try {
            Connection connection = DriverManager.getConnection("jdbc:sqlite:" + currentWorkingDirectory + databaseFileName);
            String sqlDropTable = "DROP TABLE IF EXISTS Locations";
            connection.createStatement().execute(sqlDropTable);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @BeforeEach
    void setUp() {
        try {
            server = new Thread(new Server(TEST_PORT, currentWorkingDirectory + databaseFileName));
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        server.start();
    }

    @Test
    void performHandshakeWithServer() {
        Socket client = null;
        ObjectInputStream in = null;
        ObjectOutputStream out = null;
        LocationObject locationObject = null;
        String ip = null;

        try {
            client = new Socket("localhost", TEST_PORT);
            in = new ObjectInputStream(client.getInputStream()); // start handshake
            locationObject = (LocationObject) in.readObject();
            out = new ObjectOutputStream(client.getOutputStream());
            out.writeObject(locationObject);

            StringBuilder stringBuilder = new StringBuilder();
            for (byte b : client.getInetAddress().getAddress()) {
                if (stringBuilder.length() > 0) stringBuilder.append(".");
                stringBuilder.append(b);
            }
            ip = stringBuilder.toString();

            server.interrupt();
            client.close();
        } catch (ClassNotFoundException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        assertTrue(locationObject.getKey().contains(ip));
    }

    /**
     * To test "different" IPs, we'll exploit how keys are generated on the server with no knowledge of what or where
     * the client is.
     */
    @Test
    void simulateTwoClientsWithinBounds() {
        Socket client = null;
        ObjectInputStream in = null;
        ObjectOutputStream out = null;
        LocationObject locationObject = null;
        LocationObject clientOneLocationObject = new LocationObject(5.001, 5.001);
        LocationObject clientTwoLocationObject = new LocationObject(5.003, 5.003);
        String clientOneIp = null;
        String clientTwoIp = null;
        String clientOneKey = null;
        String clientTwoKey = null;

        // client one communicates to server
        try {
            client = new Socket("localhost", TEST_PORT);
            in = new ObjectInputStream(client.getInputStream());
            locationObject = (LocationObject) in.readObject();
            clientOneKey = locationObject.getKey(); // store client key
            out = new ObjectOutputStream(client.getOutputStream());
            out.writeObject(locationObject);
            out.flush();

            StringBuilder stringBuilder = new StringBuilder();
            for (byte b : client.getInetAddress().getAddress()) {
                if (stringBuilder.length() > 0) stringBuilder.append(".");
                stringBuilder.append(b);
            }
            clientOneIp = stringBuilder.toString();

            // send client's location
            clientOneLocationObject.setKey(clientTwoKey);
            out.writeObject(clientOneLocationObject);
            out.flush();

            // server interacts with the database

            // receive the location from the server
            locationObject = (LocationObject) in.readObject();

            // client has finished communicating with the server
            client.close();
        } catch (UnknownHostException e) {
            System.out.println(e.getMessage());
        } catch (ClassNotFoundException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } finally {
            // there should be no records in the database, so server sends the out-of-bounds location
            assertTrue(clientOneKey.contains(clientOneIp));
            assertTrue(locationObject.getLatitude() == 360 && locationObject.getLongitude() == 360);
        }

        // wait for the server to generate a new timestamp-based key
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }

        // client two communicates with server
        try {
            client = new Socket("localhost", TEST_PORT);
            in = new ObjectInputStream(client.getInputStream());
            locationObject = (LocationObject) in.readObject();
            clientTwoKey = locationObject.getKey(); // store client key
            out = new ObjectOutputStream(client.getOutputStream());
            out.writeObject(locationObject);
            out.flush();

            StringBuilder stringBuilder = new StringBuilder();
            for (byte b : client.getInetAddress().getAddress()) {
                if (stringBuilder.length() > 0) stringBuilder.append(".");
                stringBuilder.append(b);
            }
            clientTwoIp = stringBuilder.toString();

            // send client's location
            clientTwoLocationObject.setKey(clientTwoKey);
            out.writeObject(clientTwoLocationObject);
            out.flush();

            // server interacts with the database

            // receive the location from the server
            locationObject = (LocationObject) in.readObject();

            // client has finished communicating with the server
            client.close();
        } catch (UnknownHostException e) {
            System.out.println(e.getMessage());
        } catch (ClassNotFoundException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } finally {
            // don't need the server anymore
            server.interrupt();

            assertTrue(clientTwoKey.contains(clientTwoIp));
            assertEquals(clientOneLocationObject.getLatitude(), locationObject.getLatitude());
            assertEquals(clientOneLocationObject.getLongitude(), locationObject.getLongitude());
        }
    }

    @AfterEach
    void tearDown() {
    }
}