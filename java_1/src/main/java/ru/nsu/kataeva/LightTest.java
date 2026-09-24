package ru.nsu.kataeva;

import java.io.DataInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LightTest {
    public static void main(String[] args) throws InterruptedException {

        int clientsCount = 100;


        ExecutorService executor = Executors.newFixedThreadPool(clientsCount);

        System.out.println("STARTING " + clientsCount + " CLIENTS...");

        for (int i = 1; i <= clientsCount; i++) {
            final int clientId = i;
            executor.submit(() -> runClient(clientId));
        }


        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.MINUTES);
        System.out.println("\nFINISH");
    }

    private static void runClient(int id) {
        String name = "client" + id + ".example.com";
        String host = "localhost";
        int port = 8081;


        boolean crash = (id % 10 == 0);
        int delay = 0;
        if (id % 5 == 0) {
            delay = 10;
        } else if (id % 3 == 0) {
            delay = 5;
        }


        try (Socket socket = new Socket(host, port);
             OutputStream out = socket.getOutputStream();
             InputStream in = socket.getInputStream()) {


            out.write((name + "\0").getBytes(StandardCharsets.US_ASCII));
            out.flush();


            if (crash) {
                System.out.println(name + ": Polomka... (--exit)");
                return;
            }


            if (delay > 0) {
                System.out.println(name + ": Zaderzhka " + delay + " sekund...");
                Thread.sleep(delay * 1000L);
            }

            DataInputStream din = new DataInputStream(in);


            int certificateLen = din.readInt();
            byte[] certData = new byte[certificateLen];
            din.readFully(certData);


            int keyLen = din.readInt();
            byte[] keyData = new byte[keyLen];
            din.readFully(keyData);

            System.out.println(name + ": KEY SAVED IN MEMORY (cert: " + certificateLen + "b, key: " + keyLen + "b)");


        } catch (Exception e) {
            System.out.println(name + ": ERROR - " + e.getMessage());
        }
    }
}