package ru.nsu.kataeva;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CustomList implements Iterable<String> {
    private final Node dummy = new Node(null);

    public void addFirst(String data) {
        Node newNode = new Node(data);
        dummy.lock.lock();
        try {
            newNode.next = dummy.next;
            dummy.next = newNode;
        } finally {
            dummy.lock.unlock();
        }
    }

    public Node getDummy() {
        return dummy;
    }

    @Override
    public Iterator<String> iterator() {
        List<String> snapshot = new ArrayList<>();
        Node current = dummy;
        current.lock.lock();
        try {
            Node nextNode = current.next;
            while (nextNode != null) {
                nextNode.lock.lock();
                snapshot.add(nextNode.data);
                Node oldCurrent = current;
                current = nextNode;
                nextNode = current.next;
                oldCurrent.lock.unlock();
            }
        } finally {
            current.lock.unlock();
        }
        return snapshot.iterator();
    }
}