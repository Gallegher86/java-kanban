package com.yandex.taskmanager.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EpicTest {
    @Test
    public void mustBeEqualWhenIdsAreSame () {
        Epic epic1 = new Epic(10, "Имя1", "Описание1");
        Epic epic2 = new Epic(10, "Имя2", "Описание2");

        assertEquals(epic1.getId(), epic2.getId(), "Задачи должны быть равны если их id одинаковы.");
        assertEquals(epic1, epic2, "Задачи должны быть равны если их id одинаковы.");
    }

    @Test
    public void mustNotAddItselfAsSubTaskId () {
        Epic epic1 = new Epic(10, "Имя1", "Описание1");

        IllegalArgumentException ownIdException = assertThrows(IllegalArgumentException.class,
                () -> epic1.addSubTaskId(epic1.getId()));
        assertTrue(ownIdException.getMessage().contains("must not be equal to epicId"),
                "Сообщение об ошибке должно содержать слово 'must not be equal to epicId'.");
        assertTrue(epic1.getSubTaskIdList().isEmpty(), "SubTaskIdList эпика должен быть пустым при " +
                "добавлении в него его собственного id");
    }

    @Test
    public void mustNotAddNegativeOrZeroIdToSubTaskIdList() {
        Epic epic1 = new Epic(10, "Имя1", "Описание1");

        IllegalArgumentException negativeIdException = assertThrows(IllegalArgumentException.class,
                () -> epic1.addSubTaskId(-999));
        assertTrue(negativeIdException.getMessage().contains("be negative"),
                "Сообщение об ошибке должно содержать слово 'be negative'.");
        assertTrue(epic1.getSubTaskIdList().isEmpty(), "SubTaskIdList эпика должен быть пустым при " +
                "добавлении в него отрицательного id.");

        IllegalArgumentException zeroIdException = assertThrows(IllegalArgumentException.class,
                () -> epic1.addSubTaskId(0));
        assertTrue(zeroIdException.getMessage().contains("zero"),
                "Сообщение об ошибке должно содержать слово 'zero'.");
        assertTrue(epic1.getSubTaskIdList().isEmpty(), "SubTaskIdList эпика должен быть пустым при " +
                "добавлении в него id равного 0.");
    }

    @Test
    public void mustNotSetSubTaskIdListWithItsOwnId() {
        Epic epic1 = new Epic(10, "Имя1", "Описание1");

        List<Integer> subTaskIdList = new ArrayList<>();
        subTaskIdList.add(epic1.getId());

        IllegalArgumentException ownIdException = assertThrows(IllegalArgumentException.class,
                () -> epic1.setSubTaskIdList(subTaskIdList));
        assertTrue(ownIdException.getMessage().contains("epicId"),
                "Сообщение об ошибке должно содержать слово 'epicId'.");
        assertTrue(epic1.getSubTaskIdList().isEmpty(), "SubTaskIdList эпика должен быть пустым при " +
                "замене его на SubTaskIdList с id эпика");
    }

    @Test
    public void mustNotSetSubTaskIdListWithNegativeId() {
        Epic epic1 = new Epic(10, "Имя1", "Описание1");

        List<Integer> negativeSubTaskIdList = new ArrayList<>();
        negativeSubTaskIdList.add(-999);

        IllegalArgumentException negativeIdException = assertThrows(IllegalArgumentException.class,
                () -> epic1.setSubTaskIdList(negativeSubTaskIdList));
        assertTrue(negativeIdException.getMessage().contains("negative"),
                "Сообщение об ошибке должно содержать слово 'negative'.");
        assertTrue(epic1.getSubTaskIdList().isEmpty(), "SubTaskIdList эпика должен быть пустым при " +
                "замене его на SubTaskIdList с отрицательным id.");
    }

    @Test
    public void mustNotSetSubTaskIdListWithZeroId() {
        Epic epic1 = new Epic(10, "Имя1", "Описание1");

        List<Integer> zeroSubTaskIdList = new ArrayList<>();
        zeroSubTaskIdList.add(0);

        IllegalArgumentException zeroIdException = assertThrows(IllegalArgumentException.class,
                () -> epic1.setSubTaskIdList(zeroSubTaskIdList));
        assertTrue(zeroIdException.getMessage().contains("zero"),
                "Сообщение об ошибке должно содержать слово 'zero'.");
        assertTrue(epic1.getSubTaskIdList().isEmpty(), "SubTaskIdList эпика должен быть пустым при " +
                "замене его на SubTaskIdList с id равным 0.");
    }
}