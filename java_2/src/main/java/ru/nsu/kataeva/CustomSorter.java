package ru.nsu.kataeva;
import java.util.concurrent.atomic.AtomicLong;

public class CustomSorter extends Thread {
    private final CustomList list;
    private final long delayMs;
    private final AtomicLong stepCounter;

    public CustomSorter(CustomList list, long delayMs, AtomicLong stepCounter) {
        this.list = list;
        this.delayMs = delayMs;
        this.stepCounter = stepCounter;
    }

    @Override
    public void run() {
        while (true) {
            Node prev = list.getDummy();
            prev.lock.lock();
            Node curr = prev.next;

            if (curr == null) {
                prev.lock.unlock();
                try { Thread.sleep(delayMs); } catch (InterruptedException ignored) {}
                continue;
            }

            curr.lock.lock();

            while (curr.next != null) {
                Node nextNode = curr.next;
                nextNode.lock.lock();

                try { Thread.sleep(delayMs); } catch (InterruptedException ignored) {}

                boolean swapped = false;
                if (curr.data.compareTo(nextNode.data) > 0) {
                    curr.next = nextNode.next;
                    nextNode.next = curr;
                    prev.next = nextNode;
                    swapped = true;
                }

                stepCounter.incrementAndGet();

                try { Thread.sleep(delayMs); } catch (InterruptedException ignored) {}

                if (swapped) {
                    Node oldPrev = prev;
                    prev = nextNode;
                    oldPrev.lock.unlock();
                } else {
                    Node oldPrev = prev;
                    prev = curr;
                    curr = nextNode;
                    oldPrev.lock.unlock();
                }
            }
            curr.lock.unlock();
            prev.lock.unlock();
        }
    }
}