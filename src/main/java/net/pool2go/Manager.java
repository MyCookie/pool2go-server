package net.pool2go;

import java.util.Scanner;

/**
 * Main
 */
public class Manager {

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
                    "Server running on port:  " + Integer.toString(port) + "\n");
        }

        // Start the runner
        Runner runner = null;

        try {
            runner = new Runner(path, filename, port);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Could not instantiate Runner.");
            return;
        }

        Scanner in = new Scanner(System.in);
        System.out.println("Type 'quit' to stop server");

        boolean done = false;
        while(!done && in.hasNextLine()) {
            if (in.nextLine().toLowerCase().trim().equals("quit"))
                done = true;
        }

        runner.stopServer();
    }
}
