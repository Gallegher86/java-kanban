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

    @Test
    public void setIdMustNotSetNegativeOrZeroIdOrEpicId () {
        int epicId = 10;
        SubTask subTask1 = new SubTask("Имя1", "Описание1", epicId);

        assertEquals(0 , subTask1.getId(),
                "id новой подзадачи должен быть равен 0.");

        subTask1.setId(999);
        assertEquals(999 , subTask1.getId(),
                "id подзадачи должен поменяться на 999.");

        subTask1.setId(0);
        assertEquals(999 , subTask1.getId(),
                "id не должен меняться при попытке установить 0.");

        subTask1.setId(-999);
        assertEquals(999 , subTask1.getId(),
                "id не должен меняться при попытке установить отрицательное значение.");

        subTask1.setId(epicId);
        assertEquals(999 , subTask1.getId(),
                "id не должен меняться при попытке установить id равным epicId.");
    }
}