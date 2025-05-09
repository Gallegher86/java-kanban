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

    public void addSubTaskId(int id) {
        if (id != this.id && id > 0) {
            subTaskIds.add(id);
        } else {
            throw new IllegalArgumentException("Переданный subTaskId, равен ID эпика, 0 или отрицателен: " + id);
        }
    }

    public void removeSubTaskId(int id) {
        subTaskIds.remove((Integer) id);
    }

    public void setSubTaskIdList(List<Integer> newList) {
        for (Integer checkId : newList) {
            if (checkId == this.id || checkId <= 0) {
                throw new IllegalArgumentException("В переданном subTaskIds, есть ID эпика или 0, или отрицательный ID: " + checkId);
            }
        }
        subTaskIds = new ArrayList<>(newList);
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

