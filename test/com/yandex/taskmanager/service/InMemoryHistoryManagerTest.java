package com.yandex.taskmanager.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.yandex.taskmanager.model.Task;
import com.yandex.taskmanager.model.Status;

import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class InMemoryHistoryManagerTest {
    HistoryManager manager;
    List<Task> checkList;

    @BeforeEach
    void setManager() {
        manager = Managers.getDefaultHistoryManager();
        checkList = new ArrayList<>();
    }

    @Test
    public void getHistoryReturnsSavedTasks() {
        createTenTaskListForTests();

        checkTasksUnchangedCustom(manager.getHistory(), checkList);
    }

    @Test
    public void addWorksProperly() {
        Task task = new Task(1, "ЗАДАЧА №1", "ОПИСАНИЕ 1");

        manager.add(task);
        Task taskInManager = manager.getHistory().getFirst();

        assertTrue(manager.getHistory().contains(task),
                "Задача должна быть записана в список менеджера.");
        assertEquals(task.getId(), taskInManager.getId(), "Id задачи должен совпадать.");
        assertEquals(task.getName(), taskInManager.getName(), "Имя задачи должно совпадать.");
        assertEquals(task.getDescription(), taskInManager.getDescription(), "Описание задачи должно совпадать.");
        assertEquals(task.getStatus(), taskInManager.getStatus(), "Статус задачи должен совпадать.");

        Task updatedTask = new Task(1, "ЗАДАЧА ИЗМЕНЕНА", "ОПИСАНИЕ ИЗМЕНЕНО", Status.DONE);

        manager.add(updatedTask);
        taskInManager = manager.getHistory().getFirst();

        assertTrue(manager.getHistory().contains(updatedTask), "Задача должна быть записана в список менеджера.");
        assertEquals(1, manager.getHistory().size(), "Задача должна быть записана поверх предыдущего экземпляра.");
        assertEquals(task.getId(), taskInManager.getId(), "Id задачи не должен измениться.");
        assertEquals(updatedTask.getName(), taskInManager.getName(), "Имя задачи должно обновиться.");
        assertEquals(updatedTask.getDescription(), taskInManager.getDescription(), "Описание задачи должно обновиться.");
        assertEquals(updatedTask.getStatus(), taskInManager.getStatus(), "Статус задачи должен обновиться.");
    }

    @Test
    public void addMustMoveUpdatedTaskToTheEndOfList() {
        createTenTaskListForTests();

        assertEquals(10, manager.getHistory().size(), "В менеджере должно быть записано десять задач.");
        assertEquals(1, manager.getHistory().get(0).getId(), "Задача с id = 1 должна быть в начале списка.");
        assertEquals(10, manager.getHistory().get(9).getId(), "Задача с id = 10 должна быть в конце списка.");

        manager.add(checkList.getFirst());
        assertEquals(10, manager.getHistory().size(), "В менеджере должно быть записано десять задач.");
        assertEquals(2, manager.getHistory().get(0).getId(), "Задача с id = 2 должна быть в начале списка.");
        assertEquals(10, manager.getHistory().get(8).getId(), "Задача с id = 10 должна быть предпоследней.");
        assertEquals(1, manager.getHistory().get(9).getId(), "Задача с id = 1 должна быть в конце списка.");
    }

    @Test
    public void addMustThrowExceptionIfAddNullTask() {
        createTenTaskListForTests();

        NullPointerException ex1 = assertThrows(NullPointerException.class,
                () -> manager.add(null));
        assertTrue(ex1.getMessage().contains("null"),
                "Сообщение об ошибке должно содержать слово 'null'.");

        checkTasksUnchangedCustom(manager.getHistory(), checkList);
    }

    @Test
    public void getHistoryMustReturnTasksInOrderOfAdd() {
        createTenTaskListForTests();

        assertEquals(10, manager.getHistory().size(), "В менеджере должно быть записано десять задач.");
        checkTasksUnchangedCustom(manager.getHistory(), checkList);

        List<Task> newCheckList = new ArrayList<>();

        for (int i = 9; i >= 0; i--) {
            manager.add(checkList.get(i));
            newCheckList.add(checkList.get(i));
        }

        assertEquals(10, manager.getHistory().size(), "В менеджере должно быть записано десять задач.");
        checkTasksUnchangedCustom(manager.getHistory(), newCheckList);
    }

    @Test
    public void removeWorksProperly() {
        createTenTaskListForTests();

        manager.remove(5);
        checkList = manager.getHistory();
        assertEquals(9, checkList.size(), "В менеджер должно быть записано девять задач.");
        boolean isTaskFound = false;

        for (Task task : checkList) {
            if (task.getId() == 5) {
                isTaskFound = true;
                break;
            }
        }
        assertFalse(isTaskFound, "Задача с id = 5 должна быть удалена из менеджера.");

        manager.remove(10);
        checkList = manager.getHistory();

        assertEquals(8, checkList.size(), "В менеджер должно быть записано восемь задач.");
        for (Task task : checkList) {
            if (task.getId() == 10) {
                isTaskFound = true;
                break;
            }
        }
        assertFalse(isTaskFound, "Задача с id = 10 должна быть удалена из менеджера.");

        manager.remove(1);
        checkList = manager.getHistory();

        assertEquals(7, checkList.size(), "В менеджер должно быть записано семь задач.");
        for (Task task : checkList) {
            if (task.getId() == 1) {
                isTaskFound = true;
                break;
            }
        }
        assertFalse(isTaskFound, "Задача с id = 1 должна быть удалена из менеджера.");
    }

    @Test
    public void removeMustDoNothingWithWrongId() {
        createTenTaskListForTests();

        manager.remove(-999);
        checkTasksUnchangedCustom(manager.getHistory(), checkList);

        manager.remove(0);
        checkTasksUnchangedCustom(manager.getHistory(), checkList);

        manager.remove(999);
        checkTasksUnchangedCustom(manager.getHistory(), checkList);
    }

    @Test
    public void clearHistoryWorksProperly() {
        createTenTaskListForTests();

        manager.clearHistory();

        assertTrue(manager.getHistory().isEmpty(),
                "История менеджера должна быть пустой.");
    }

    //ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ.
    private void createTenTaskListForTests() {
        for (int i = 1; i <= 10; i++) {
            Task task = new Task(i, ("ЗАДАЧА №" + i), ("ОПИСАНИЕ " + i));
            manager.add(task);
            checkList.add(task);
        }
    }

    private void checkTasksUnchangedCustom(List<? extends Task> tasksToCheck, List<? extends Task> initialTasks) {
        assertEquals(tasksToCheck.size(), initialTasks.size(), "Размер списка на проверку должен " +
                "совпадать с размером списка менеджера.");

        for (int i = 0; i < tasksToCheck.size(); i++) {
            Task managerListTask = initialTasks.get(i);
            Task checkListTask = tasksToCheck.get(i);

            assertEquals(managerListTask.getClass(), checkListTask.getClass(),
                    "Классы задач не совпадают на позиции " + i);
            assertEquals(managerListTask.getId(), checkListTask.getId(),
                    "ID задач не совпадает на позиции " + i);
            assertEquals(managerListTask.getName(), checkListTask.getName(),
                    "Имя задачи не совпадает на позиции " + i);
            assertEquals(managerListTask.getDescription(), checkListTask.getDescription(),
                    "Описание не совпадает на позиции " + i);
            assertEquals(managerListTask.getStatus(), checkListTask.getStatus(),
                    "Статус не совпадает на позиции " + i);
        }
    }
}