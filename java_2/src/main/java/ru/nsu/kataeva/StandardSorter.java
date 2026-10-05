package ru.nsu.kataeva;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class StandardSorter extends Thread {
    private final List<String> list;
    private final long delayMs;
    private final AtomicLong stepCounter;

    public StandardSorter(List<String> list, long delayMs, AtomicLong stepCounter) {
        this.list = list;
        this.delayMs = delayMs;
        this.stepCounter = stepCounter;
    }

    @Override
    public void run() {
        while (true) {
            int size = list.size();
            for (int i = 0; i < size - 1; i++) {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    return;
                }
                synchronized (list) {
                    String a = list.get(i);
                    String b = list.get(i + 1);

                    if (a.compareTo(b) > 0) {
                        list.set(i, b);
                        list.set(i + 1, a);
                    }
                }

                stepCounter.incrementAndGet();

                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    return;
                }
            }


            if (list.size() < 2) {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }
}
