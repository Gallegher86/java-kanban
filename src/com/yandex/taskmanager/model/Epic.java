package com.yandex.taskmanager.model;

import java.util.List;
import java.util.ArrayList;

public class Epic extends Task {
    private List<Integer> subTaskIds = new ArrayList<>();

    public Epic(String name, String description) {
        super(name, description);
    }

    public Epic(int id, String name, String description) {
        super(id, name, description);
    }

    public Epic(Epic other, List<Integer> subTaskIds) {
        super(other.id, other.name, other.description, other.status);
        this.subTaskIds = new ArrayList<>(subTaskIds);
    }

    public Epic(Epic other, Status status) {
        super(other.id, other.name, other.description, status);
        this.subTaskIds = new ArrayList<>(other.subTaskIds);
    }

    public List<Integer> getSubTaskIdList() {
        return new ArrayList<>(subTaskIds);
    }

    public void addSubTaskId(Integer subTaskId) {
        subTaskIds.add(subTaskId);
    }

    @Override
    public String toString() {
        return String.format("%s,%s,%s,%s,%s", id, TaskType.EPIC, name, status, description);
    }
}

