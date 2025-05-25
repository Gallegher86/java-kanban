package com.yandex.taskmanager.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TaskTest {
    @Test
    public void mustBeEqualWhenIdsAreSame () {
        Task task1 = new Task(10, "Имя1", "Описание1");
        Task task2 = new Task(10, "Имя2", "Описание2");

        assertEquals(task1.getId(), task2.getId(), "Задачи должны быть равны если их id одинаковы.");
        assertEquals(task1, task2, "Задачи должны быть равны если их id одинаковы.");
    }
}