package com.yandex.taskmanager.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EpicTest {
    @Test
    public void mustBeEqualWhenIdsAreSame () {
        Epic epic1 = new Epic(10, "Имя1", "Описание1");
        Epic epic2 = new Epic(10, "Имя2", "Описание2");

        assertEquals(epic1.getId(), epic2.getId(), "Задачи должны быть равны если их id одинаковы.");
        assertEquals(epic1, epic2, "Задачи должны быть равны если их id одинаковы.");
    }

    @Test
    public void setIdMustNotSetNegativeOrZeroId () {
        Epic epic1 = new Epic("Имя1", "Описание1");

        assertEquals(0 , epic1.getId(),
                "id нового эпика должен быть равен 0.");

        epic1.setId(999);
        assertEquals(999 , epic1.getId(),
                "id эпика должен поменяться на 999.");

        epic1.setId(0);
        assertEquals(999 , epic1.getId(),
                "id не должен меняться при попытке установить 0.");

        epic1.setId(-999);
        assertEquals(999 , epic1.getId(),
                "id не должен меняться при попытке установить отрицательное значение.");
    }

    @Test
    public void mustNotAddItselfAsSubTaskId () {
        Epic epic1 = new Epic(10, "Имя1", "Описание1");

        epic1.addSubTaskId(epic1.getId());
        assertTrue(epic1.getSubTaskIdList().isEmpty(), "SubTaskIdList эпика должен быть пустым при " +
                "добавлении в него его собственного id");
    }

    @Test
    public void mustNotAddNegativeIdToSubTaskIdList () {
        Epic epic1 = new Epic(10, "Имя1", "Описание1");

        epic1.addSubTaskId(-999);
        assertTrue(epic1.getSubTaskIdList().isEmpty(), "SubTaskIdList эпика должен быть пустым при " +
                "добавлении в него отрицательного id");
    }

    @Test
    public void mustNotSetSubTaskIdListWithItsOwnId() {
        Epic epic1 = new Epic(10, "Имя1", "Описание1");

        List<Integer> subTaskIdList = new ArrayList<>();
        subTaskIdList.add(epic1.getId());

        epic1.setSubTaskIdList(subTaskIdList);
        assertTrue(epic1.getSubTaskIdList().isEmpty(), "SubTaskIdList эпика должен быть пустым при " +
                "замене его на SubTaskIdList с id эпика");
    }

    @Test
    public void mustNotSetSubTaskIdListWithNegativeId() {
        Epic epic1 = new Epic(10, "Имя1", "Описание1");

        List<Integer> subTaskIdList = new ArrayList<>();
        subTaskIdList.add(-999);

        epic1.setSubTaskIdList(subTaskIdList);
        assertTrue(epic1.getSubTaskIdList().isEmpty(), "SubTaskIdList эпика должен быть пустым при " +
                "замене его на SubTaskIdList с отрицательным id");
    }
}