package net.pool2go;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

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
    }

    @BeforeEach
    void setUp() {
        try {
            server = new Thread(new Server(TEST_PORT, currentWorkingDirectory + databaseFileName));
        } catch (IOException e) {
            System.out.println(e.getCause());
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
            in = new ObjectInputStream(client.getInputStream());
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
            System.out.println(e.getCause());
        } catch (IOException e) {
            System.out.println(e.getCause());
        }

        assertTrue(locationObject.getKey().contains(ip));
    }

    @AfterEach
    void tearDown() {
    }
}