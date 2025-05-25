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

    public Epic(Epic other) {
        super(other.id, other.name, other.description, other.status);
        this.subTaskIds = other.subTaskIds;
    }

    public void addSubTaskId(int id) {
        if (id != this.id && id > 0) {
            subTaskIds.add(id);
        } else {
            throw new IllegalArgumentException("SubTaskId must not be equal to epicId, zero or be negative: " + id);
        }
    }

    public void removeSubTaskId(int id) {
        subTaskIds.remove((Integer) id);
    }

    public void setSubTaskIdList(List<Integer> newList) {
        if (newList == null) {
            throw new NullPointerException("The List provided to Epic is null.");
        }

        for (Integer checkId : newList) {
            if (checkId == this.id || checkId <= 0) {
                throw new IllegalArgumentException("SubTaskIdList must not contain epicId, zero or negative Id: " + checkId);
            }
        }
        subTaskIds = new ArrayList<>(newList);
    }

    public void setStatus(Status status) {
        if (status == null) {
            throw new NullPointerException("The Status provided to Epic is null.");
        }

        this.status = status;
    }

    public List<Integer> getSubTaskIdList() {
        return new ArrayList<>(subTaskIds);
    }

    @Override
    public String toString() {
        return "Epic{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", status=" + status +
                '}';
    }
}

