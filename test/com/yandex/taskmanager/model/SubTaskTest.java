package com.yandex.taskmanager.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SubTaskTest {
    @Test
    public void mustBeEqualWhenIdsAreSame () {
        SubTask subTask1 = new SubTask(10, "Имя1", "Описание1", Status.NEW, 10);
        SubTask subTask2 = new SubTask(10, "Имя2", "Описание2", Status.DONE, 20);

        assertEquals(subTask1.getId(), subTask2.getId(), "Задачи должны быть равны если их id одинаковы.");
        assertEquals(subTask1, subTask2, "Задачи должны быть равны если их id одинаковы.");
    }
}