package com.yandex.taskmanager.service;

import com.yandex.taskmanager.model.*;
import java.util.List;
import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryHistoryManagerTest {
    HistoryManager manager;
    List<Task> checkList;
    int historyManagerMaxSize = 10;

    @BeforeEach
    void setManager() {
        manager = Managers.getDefaultHistoryManager();
        checkList = new ArrayList<>();
    }

    @Test
    public void getHistoryWorksProperly() {
        createNineTaskListForTests();

        assertEquals(manager.getHistory(), checkList,
                "Должны возвращаться сохраненные задачи.");
    }

    @Test
    public void addHistoryWorksProperly() {
        Task task = new Task(("ЗАДАЧА №1"),("ОПИСАНИЕ 1"));
        task.setId(1);

        manager.addHistory(task);
        assertTrue(manager.getHistory().contains(task),
                "Задача должна быть записана в список менеджера.");
    }

    @Test
    public void clearHistoryWorksProperly() {
        createNineTaskListForTests();

        manager.clearHistory();

        assertTrue(manager.getHistory().isEmpty(),
                "История менеджера должна быть пустой.");
    }

    @Test
    public void mustRemoveOldestTaskWhenHistoryExceedsLimit() {
        createNineTaskListForTests();
        final Task firstTask = manager.getHistory().get(0);
        final Task secondTask = manager.getHistory().get(1);

        Task task10 = new Task(("ЗАДАЧА №10"),("ОПИСАНИЕ 10"));
        task10.setId(historyManagerMaxSize);
        Task task11 = new Task(("ЗАДАЧА №11"),("ОПИСАНИЕ 11"));
        task11.setId(historyManagerMaxSize + 1);

        manager.addHistory(task10);
        Task currentFirstTask = manager.getHistory().getFirst();
        assertEquals(firstTask, currentFirstTask,
                "После добавления 10-й задачи история не должна удалять старые задачи.");

        manager.addHistory(task11);
        currentFirstTask = manager.getHistory().getFirst();
        assertNotEquals(firstTask, currentFirstTask,
                "После добавления 11-й задачи история должна удалить самую старую задачу.");
        assertEquals(secondTask, currentFirstTask,
                "История должна начинаться со второй самой старой задачи после удаления первой.");
    }

    //ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ.
    private void createNineTaskListForTests() {
        for (int i = 1; i < historyManagerMaxSize; i++) {
            Task task = new Task(("ЗАДАЧА №" + i),("ОПИСАНИЕ " + i));
            task.setId(i);
            manager.addHistory(task);
            checkList.add(task);
        }
    }
}