package com.yandex.taskmanager.service;

import com.yandex.taskmanager.model.Task;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class InMemoryHistoryManager implements HistoryManager {
    private Node<Task> head;
    private Node<Task> tail;
    private final Map<Integer, Node<Task>> nodesMap = new HashMap<>();

    public void add(Task task) {
        if (task == null) {
            throw new NullPointerException("The Task provided to History Manager is null.");
        }

        if (nodesMap.containsKey(task.getId())) {
            removeNode(nodesMap.get(task.getId()));
        }

        linkLast(task);
        nodesMap.put(task.getId(), tail);
    }

    public List<Task> getHistory() {
        List<Task> historyList = new ArrayList<>();

        Node<Task> currentNode = head;
        while (currentNode != null) {
            historyList.add(currentNode.data);
            currentNode = currentNode.next;
        }
        return historyList;
    }

    public void remove(int id) {
        Node<Task> node = nodesMap.remove(id);

        if (node != null) {
            removeNode(node);
        }
    }

    public void clearHistory() {
        Node<Task> currentNode = head;

        while (currentNode != null) {
            Node<Task> nextNode = currentNode.next;

            currentNode.data = null;
            currentNode.prev = null;
            currentNode.next = null;

            currentNode = nextNode;
        }

        head = null;
        tail = null;
        nodesMap.clear();
    }

    private void linkLast(Task task) {
        final Node<Task> newNode = new Node<>(tail, task, null);
        if (tail != null) {
            tail.next = newNode;
        } else {
            head = newNode;
        }
        tail = newNode;
    }

    private void removeNode(Node<Task> node) {
        final Node<Task> nextNode = node.next;
        final Node<Task> prevNode = node.prev;

        if (prevNode == null) {
            head = nextNode;
            if (nextNode != null) {
                nextNode.prev = null;
            }
        } else {
            prevNode.next = nextNode;
        }

        if (nextNode == null) {
            tail = prevNode;
        } else {
            nextNode.prev = prevNode;
        }

        node.next = null;
        node.prev = null;
        node.data = null;
    }

    private static class Node<T> {
        T data;
        Node<T> next;
        Node<T> prev;

        Node(Node<T> prev, T data, Node<T> next) {
            this.data = data;
            this.next = next;
            this.prev = prev;
        }
    }
}