package com.yandex.taskmanager.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class TaskTest {
    @Test
    public void mustBeEqualWhenIdsAreSame () {
        Task task1 = new Task(10, "Имя1", "Описание1");
        Task task2 = new Task(10, "Имя2", "Описание2");

        assertEquals(task1.getId(), task2.getId(), "Задачи должны быть равны если их id одинаковы.");
        assertEquals(task1, task2, "Задачи должны быть равны если их id одинаковы.");
    }

    @Test
    public void setIdMustNotSetNegativeOrZeroId () {
        Task task1 = new Task("Имя1", "Описание1");

        assertEquals(0 , task1.getId(),
                "id новой задачи должен быть равен 0.");

        task1.setId(999);
        assertEquals(999 , task1.getId(),
                "id задачи должен поменяться на 999.");

        IllegalArgumentException zeroException = assertThrows(IllegalArgumentException.class, () -> task1.setId(0));
        assertTrue(zeroException.getMessage().contains("0"),
                "Сообщение об ошибке должно содержать число 0.");
        assertEquals(999 , task1.getId(),
                "id задачи не должен поменяться.");

        IllegalArgumentException negativeException = assertThrows(IllegalArgumentException.class, () -> task1.setId(-999));
        assertTrue(negativeException.getMessage().contains("отрицательным"),
                "Сообщение об ошибке должно содержать слово 'отрицательным'.");
        assertEquals(999 , task1.getId(),
                "id задачи не должен поменяться.");
    }
}