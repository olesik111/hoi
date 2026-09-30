package ru.nsu.kataeva;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicLong;

public class Main {
    public static void main(String[] args) {
        CustomList customList = new CustomList();
        List<String> standardList = Collections.synchronizedList(new ArrayList<>());
        AtomicLong customSteps = new AtomicLong(0);
        AtomicLong standardSteps = new AtomicLong(0);

        int threadCount = 3;
        long delay = 1000;

        for (int i = 0; i < threadCount; i++) {
            new CustomSorter(customList, delay, customSteps).start();
            new StandardSorter(standardList, delay, standardSteps).start();
        }

        Scanner scanner = new Scanner(System.in);
        while (true) {
            String line = scanner.nextLine();
            if (line.isEmpty()) {
                for (String s : customList) {
                    System.out.println(s);
                }
                System.out.println(customSteps.get());
                System.out.println(standardSteps.get());
            } else {
                for (int i = line.length(); i > 0; i -= 80) {
                    int start = Math.max(0, i - 80);
                    String part = line.substring(start, i);
                    customList.addFirst(part);
                    standardList.add(0, part);
                }
            }
        }
    }
}
