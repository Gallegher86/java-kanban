package com.yandex.taskmanager.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EpicTest {
    @Test
    public void mustBeEqualWhenIdsAreSame() {
        Epic epic1 = new Epic(10, "Имя1", "Описание1");
        Epic epic2 = new Epic(10, "Имя2", "Описание2");

        assertEquals(epic1.getId(), epic2.getId(), "Задачи должны быть равны если их id одинаковы.");
        assertEquals(epic1, epic2, "Задачи должны быть равны если их id одинаковы.");
    }
}