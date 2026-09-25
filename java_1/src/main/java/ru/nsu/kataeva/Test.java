package ru.nsu.kataeva;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Test {
    public static void main(String[] args) throws Exception {
        int clients = 6;
        List<Process> processes = new ArrayList<>();

        String java = System.getProperty("java.home")
                + System.getProperty("file.separator")
                + "bin"
                + System.getProperty("file.separator")
                + "java";

        String classpath = System.getProperty("java.class.path");

        for (int i = 1; i <= clients; i++) {
            String name = "client" + i + ".example.com";

            List<String> command = new ArrayList<>();
            command.add(java);
            command.add("-cp");
            command.add(classpath);
            command.add("ru.nsu.kataeva.Client");
            command.add(name);
            command.add("localhost");
            command.add("8081");

            if (i % 6 == 0) {
                command.add("--exit");
            } else if (i % 5 == 0) {
                command.add("--delay");
                command.add("10");
            } else if (i % 3 == 0) {
                command.add("--delay");
                command.add("5");
            }

            System.out.println("START " + name);

            try {
                processes.add(new ProcessBuilder(command)
                        .inheritIO()
                        .start());
            } catch (IOException e) {
                System.out.println("ERROR " + name + ": " + e.getMessage());
            }
        }

        for (Process process : processes) {
            process.waitFor();
        }

        System.out.println();
        System.out.println("FINISH");
    }

}