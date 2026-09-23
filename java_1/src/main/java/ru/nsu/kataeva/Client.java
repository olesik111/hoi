package ru.nsu.kataeva;

import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Client {
    public static void main(String[] args) throws Exception {

        if (args.length < 3) {
            System.out.println("Usage: <name> <host> <port> [--delay seconds] [--exit]");
            return;
        }

        String name = args[0];
        String host = args[1];
        int port = Integer.parseInt(args[2]);

        int delay = 0;
        boolean crash = false;

        for (int i = 3; i < args.length; i++) {
            if ("--delay".equals(args[i]) && i + 1 < args.length) {
                delay = Integer.parseInt(args[i + 1]);
                i++;
            } else if ("--exit".equals(args[i])) {
                crash = true;
            }
        }

        try (Socket socket = new Socket(host, port);
             OutputStream out = socket.getOutputStream();
             InputStream in = socket.getInputStream()) {

            out.write((name + "\0").getBytes(StandardCharsets.US_ASCII));
            out.flush();

            if (crash) {
                System.out.println("Polomka...");
                System.exit(1);
            }

            if (delay > 0) {
                System.out.println("Zaderzhka " + delay + " sekund...");
                Thread.sleep(delay * 1000L);
            }

            DataInputStream din = new DataInputStream(in);

            int certificate = din.readInt();
            byte[] certData = new byte[certificate];
            din.readFully(certData);

            int key = din.readInt();
            byte[] keyData = new byte[key];
            din.readFully(keyData);

            try (FileOutputStream certOut = new FileOutputStream(name + ".crt");
                 FileOutputStream keyOut = new FileOutputStream(name + ".key")) {
                certOut.write(certData);
                keyOut.write(keyData);
            }
            System.out.println("KEY SAVED.");
        }
    }
}