package ru.nsu.kataeva;

import java.util.concurrent.locks.ReentrantLock;

public class Node {
    String data;
    Node next;
    final ReentrantLock lock = new ReentrantLock();

    public Node(String data) {
        this.data = data;
    }
}