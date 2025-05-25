package com.yandex.taskmanager.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import com.yandex.taskmanager.model.Task;
import com.yandex.taskmanager.model.Epic;
import com.yandex.taskmanager.model.SubTask;
import com.yandex.taskmanager.model.Status;

import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class InMemoryTaskManagerTest {
    TaskManager manager;
    private Task task1;
    private Epic epic1;
    private Epic epic2;
    private SubTask subTask1;
    private SubTask subTask2;
    private SubTask subTask3;

    @BeforeEach
    void setManager() {
        manager = Managers.getDefaultTaskManager();
    }

    // ТЕСТЫ addTask.
    @Test
    public void addAndRetrieveTasksWorksCorrectly() {
        createSixTaskListForTests();

        final Optional<Task> optionalTask = manager.getTaskById(task1.getId());
        final Optional<Task> optionalEpic = manager.getTaskById(epic1.getId());
        final Optional<Task> optionalSubTask = manager.getTaskById(subTask1.getId());

        assertTrue(optionalTask.isPresent(), "Задача не находиться по id.");
        assertTrue(optionalEpic.isPresent(), "Эпик не находиться по id.");
        assertTrue(optionalSubTask.isPresent(), "Подзадача не находиться по id.");

        final Task savedTask = optionalTask.get();
        final Epic savedEpic = (Epic) optionalEpic.get();
        final SubTask savedSubTask = (SubTask) optionalSubTask.get();

        assertEquals(task1, savedTask, "Задачи не совпадают по id (хэш определен только по id).");
        assertEquals(epic1, savedEpic, "Эпики не совпадают по id (хэш определен только по id).");
        assertEquals(subTask1, savedSubTask, "Подзадачи не совпадают по id (хэш определен только по id).");

        checkTasksUnchangedForThreeTaskList(task1, epic1, subTask1);

        final List<Task> managerList = manager.getAllTasks();
        final List<Task> tasks = manager.getTasks();
        final List<Epic> epics = manager.getEpics();
        final List<SubTask> subTasks = manager.getSubTasks();

        assertNotNull(managerList, "Список всех сохраненных объектов не возвращается.");
        assertNotNull(tasks, "Задачи не возвращаются.");
        assertNotNull(epics, "Эпики не возвращаются.");
        assertNotNull(subTasks, "Подзадачи не возвращаются.");

        checkTaskCountForSixTasks();
    }

    @Test
    public void addTaskMustThrowExceptionIfAddSameTaskTwice() {
        createThreeTaskListForTests();

        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class,
                () -> manager.addTask(task1));
        assertTrue(ex1.getMessage().contains("is not new"),
                "Сообщение об ошибке должно содержать слово 'is not new'.");
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class,
                () -> manager.addEpic(epic1));
        assertTrue(ex2.getMessage().contains("is not new"),
                "Сообщение об ошибке должно содержать слово 'is not new'.");
        IllegalArgumentException ex3 = assertThrows(IllegalArgumentException.class,
                () -> manager.addSubTask(subTask1));
        assertTrue(ex3.getMessage().contains("is not new"),
                "Сообщение об ошибке должно содержать слово 'is not new'.");

        checkTaskCountForThreeTasks();
    }

    @Test
    public void addTaskMustThrowExceptionIfAddTaskWithStatusNotNew() {
        Task notNewStatusTask = new Task(0, "Задача", "Описание", Status.IN_PROGRESS);
        Epic notNewStatusEpic = new Epic("Эпик", "Описание");
        SubTask notNewStatusSubTask = new SubTask(0, "Подзадача", "Описание", Status.IN_PROGRESS, 1);
        notNewStatusEpic.setStatus(Status.IN_PROGRESS);

        manager.addEpic(new Epic("Эпик", "Для подзадачи"));

        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class,
                () -> manager.addTask(notNewStatusTask));
        assertTrue(ex1.getMessage().contains("is not new"),
                "Сообщение об ошибке должно содержать слово 'is not new'.");
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class,
                () -> manager.addEpic(notNewStatusEpic));
        assertTrue(ex2.getMessage().contains("is not new"),
                "Сообщение об ошибке должно содержать слово 'is not new'.");
        IllegalArgumentException ex3 = assertThrows(IllegalArgumentException.class,
                () -> manager.addSubTask(notNewStatusSubTask));
        assertTrue(ex3.getMessage().contains("is not new"),
                "Сообщение об ошибке должно содержать слово 'is not new'.");

        checkTaskCountCustom(0, 1, 0, 1);
    }

    @Test
    public void addTaskMustThrowExceptionIfAddEpicWithExistingSubTaskIdList() {
        Epic notEmptyEpic = new Epic("Эпик", "Описание");
        List<Integer> subTaskIds = new ArrayList<>();
        subTaskIds.add(999);
        notEmptyEpic.setSubTaskIdList(subTaskIds);

        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class,
                () -> manager.addEpic(notEmptyEpic));
        assertTrue(ex1.getMessage().contains("Cannot add Epic with non-empty subTaskIdList"),
                "Сообщение об ошибке должно содержать слово 'Cannot add Epic with non-empty subTaskIdList'.");

        checkTaskCountForEmptyManager();
    }

    @Test
    public void addTaskMustThrowExceptionIfAddTaskWithExistingId() {
        Task notNewIdTask = new Task(999, "Задача", "Описание");
        Epic notNewIdEpic = new Epic(1000, "Эпик", "Описание");
        SubTask notNewIdSubTask = new SubTask(1001, "Подзадача", "Описание", Status.DONE, 1);

        manager.addEpic(new Epic("Эпик", "Для подзадачи"));

        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class,
                () -> manager.addTask(notNewIdTask));
        assertTrue(ex1.getMessage().contains("is not new"),
                "Сообщение об ошибке должно содержать слово 'is not new'.");
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class,
                () -> manager.addEpic(notNewIdEpic));
        assertTrue(ex2.getMessage().contains("is not new"),
                "Сообщение об ошибке должно содержать слово 'is not new'.");
        IllegalArgumentException ex3 = assertThrows(IllegalArgumentException.class,
                () -> manager.addSubTask(notNewIdSubTask));
        assertTrue(ex3.getMessage().contains("is not new"),
                "Сообщение об ошибке должно содержать слово 'is not new'.");

        checkTaskCountCustom(0, 1, 0, 1);
    }

    @Test
    public void addTaskMustThrowExceptionIfAddNullTask() {
        NullPointerException ex1 = assertThrows(NullPointerException.class,
                () -> manager.addTask(null));
        assertTrue(ex1.getMessage().contains("null"),
                "Сообщение об ошибке должно содержать слово 'null'.");
        NullPointerException ex2 = assertThrows(NullPointerException.class,
                () -> manager.addEpic(null));
        assertTrue(ex2.getMessage().contains("null"),
                "Сообщение об ошибке должно содержать слово 'null'.");
        NullPointerException ex3 = assertThrows(NullPointerException.class,
                () -> manager.addSubTask(null));
        assertTrue(ex3.getMessage().contains("null"),
                "Сообщение об ошибке должно содержать слово 'null'.");

        checkTaskCountForEmptyManager();
    }

    @Test
    public void addTaskMustThrowExceptionIfAddSubTaskWithInvalidEpicId() {
        SubTask invalidEpicIdSubTask = new SubTask("Подзадача", "Описание", 999);

        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class,
                () -> manager.addSubTask(invalidEpicIdSubTask));
        assertTrue(ex1.getMessage().contains("Epic with ID"),
                "Сообщение об ошибке должно содержать слово 'Epic with ID'.");

        checkTaskCountForEmptyManager();
    }

    @Test
    public void addTaskMustThrowExceptionIfAddSubTaskAsItsOwnEpic() {
        Epic epic1 = new Epic("Эпик", "Описание");
        manager.addEpic(epic1);
        SubTask subTask1 = new SubTask("Подзадача1", "Описание", 1);
        manager.addSubTask(subTask1);

        SubTask subTaskAsOwnEpic = new SubTask("Подзадача1", "id подзадачи, вместо epicId", 2);

        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class,
                () -> manager.addSubTask(subTaskAsOwnEpic));
        assertTrue(ex1.getMessage().contains("Epic with ID"),
                "Сообщение об ошибке должно содержать слово 'Epic with ID'.");

        checkTaskCountCustom(0, 1, 1, 2);
    }

    @Test
    public void MustThrowExceptionIfUsingAddTaskMethodToAddEpicOrSubTask() {
        Epic epic1 = new Epic("Эпик", "Описание");
        SubTask subTask1 = new SubTask("Подзадача1", "Описание", epic1.getId());

        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class,
                () -> manager.addTask(epic1));
        assertTrue(ex1.getMessage().contains("using their own methods"),
                "Сообщение об ошибке должно содержать слово 'using their own methods'.");
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class,
                () -> manager.addTask(subTask1));
        assertTrue(ex2.getMessage().contains("using their own methods"),
                "Сообщение об ошибке должно содержать слово 'using their own methods'.");

        checkTaskCountForEmptyManager();
    }

    //ТЕСТЫ deleteById и deleteAll.
    @Test
    public void deleteEpicByIdMustRemoveItsSubTasks() {
        createSixTaskListForTests();

        manager.deleteTaskById(epic1.getId());

        checkTaskCountCustom(1, 1, 1, 6);

        assertFalse(manager.getAllTasks().contains(epic1),
                "Эпик должен быть удален из списка задач.");
        assertFalse(manager.getAllTasks().contains(subTask1),
                "Подзадачи должны быть удалены из списка задач.");
        assertFalse(manager.getAllTasks().contains(subTask2),
                "Подзадачи должны быть удалены из списка задач.");

        assertFalse(manager.getEpics().contains(epic1),
                "Эпик должен быть удален из списка эпиков.");
        assertFalse(manager.getSubTasks().contains(subTask1),
                "Подзадачи должны быть удалены из списка подзадач.");
        assertFalse(manager.getSubTasks().contains(subTask2),
                "Подзадачи должны быть удалены из списка подзадач.");
    }

    @Test
    public void deleteSubTaskByIdMustRemoveItFromEpic() {
        createSixTaskListForTests();

        assertTrue(epic1.getSubTaskIdList().contains(subTask1.getId()),
                "id подзадачи должна быть в списке эпика.");

        manager.deleteTaskById(subTask1.getId());

        checkTaskCountCustom(1, 2, 2, 6);

        assertFalse(manager.getAllTasks().contains(subTask1),
                "Подзадача должна быть удален из списка задач.");
        assertFalse(manager.getSubTasks().contains(subTask1),
                "Подзадача должны быть удалены из списка подзадач.");
        assertFalse(epic1.getSubTaskIdList().contains(subTask1.getId()),
                "id подзадачи должно быть удалено из списке эпика.");
    }

    @Test
    public void deleteTaskByIdMustRemoveItFromTaskList() {
        createSixTaskListForTests();

        manager.deleteTaskById(task1.getId());

        checkTaskCountCustom(0, 2, 3, 6);
    }

    @Test
    public void deleteTaskByIdMustThrowExceptionIfIdInvalid() {
        createSixTaskListForTests();

        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class,
                () -> manager.deleteTaskById(-999));
        assertTrue(ex1.getMessage().contains("not found in TaskManager"),
                "Сообщение об ошибке должно содержать слово 'not found in TaskManager'.");
        checkTaskCountForSixTasks();

        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class,
                () -> manager.deleteTaskById(0));
        assertTrue(ex2.getMessage().contains("not found in TaskManager"),
                "Сообщение об ошибке должно содержать слово 'not found in TaskManager'.");
        checkTaskCountForSixTasks();

        IllegalArgumentException ex3 = assertThrows(IllegalArgumentException.class,
                () -> manager.deleteTaskById(999));
        assertTrue(ex3.getMessage().contains("not found in TaskManager"),
                "Сообщение об ошибке должно содержать слово 'not found in TaskManager'.");
        checkTaskCountForSixTasks();
    }

    @Test
    public void deleteAllTasksMustClearAllListsAndResetTaskCount() {
        createSixTaskListForTests();

        manager.getTaskById(task1.getId());
        manager.getTaskById(epic1.getId());
        manager.getTaskById(epic2.getId());
        manager.getTaskById(subTask1.getId());
        manager.getTaskById(subTask2.getId());
        manager.getTaskById(subTask3.getId());

        assertEquals(6, manager.getHistory().size(),
                "Количество записей в истории должно быть равно 6.");
        checkTaskCountCustom(1, 2, 3, 6);

        manager.deleteAllTasks();

        assertTrue(manager.getAllTasks().isEmpty(),
                "Общий список менеджера должен быть пустым после удаления всех задач.");
        assertTrue(manager.getTasks().isEmpty(),
                "Список задач должен быть пустым после удаления всех задач.");
        assertTrue(manager.getEpics().isEmpty(),
                "Список эпиков должен быть пустым после удаления всех задач.");
        assertTrue(manager.getSubTasks().isEmpty(),
                "Список подзадач должен быть пустым после удаления всех задач.");
        assertTrue(manager.getHistory().isEmpty(),
                "Список истории должен быть пустым после удаления всех задач.");
        assertEquals(0, manager.getIdCounter(),
                "Счетчик менеджера должен обнуляться после удаления всех задач.");
    }

    //ТЕСТЫ updateTask.
    @Test
    public void updateTaskWorksProperly() {
        createSixTaskListForTests();

        Task updateTask = new Task(task1.getId(), "ЗАДАЧА ОБНОВЛЕНА", "ЗАДАЧА ВЫПОЛНЕНА", Status.DONE);
        Epic updateEpic = new Epic(epic1.getId(), "ЭПИК ОБНОВЛЕН", "ЭПИК ОБНОВЛЕН");
        SubTask updateSubTask = new SubTask(subTask3.getId(), "ПОДЗАДАЧА ОБНОВЛЕНА", "ПОДЗАДАЧА ВЫПОЛНЕНА",
                Status.DONE, epic2.getId());

        manager.updateTask(updateTask);
        manager.updateEpic(updateEpic);
        manager.updateSubTask(updateSubTask);

        checkTaskCountForSixTasks();

        final Optional<Task> optionalTask = manager.getTaskById(task1.getId());
        final Optional<Task> optionalEpic = manager.getTaskById(epic1.getId());
        final Optional<Task> optionalSubTask = manager.getTaskById(subTask3.getId());

        assertTrue(optionalTask.isPresent(), "Задача не находиться по id.");
        assertTrue(optionalEpic.isPresent(), "Эпик не находиться по id.");
        assertTrue(optionalSubTask.isPresent(), "Подзадача не находиться по id.");

        checkTasksUnchangedForThreeTaskList(updateTask, updateEpic, updateSubTask);
    }

    @Test
    public void updateTaskMustThrowExceptionIfTaskNull() {
        createThreeTaskListForTests();

        NullPointerException ex1 = assertThrows(NullPointerException.class,
                () -> manager.updateTask(null));
        assertTrue(ex1.getMessage().contains("null"),
                "Сообщение об ошибке должно содержать слово 'null'.");
        NullPointerException ex2 = assertThrows(NullPointerException.class,
                () -> manager.updateEpic(null));
        assertTrue(ex2.getMessage().contains("null"),
                "Сообщение об ошибке должно содержать слово 'null'.");
        NullPointerException ex3 = assertThrows(NullPointerException.class,
                () -> manager.updateSubTask(null));
        assertTrue(ex3.getMessage().contains("null"),
                "Сообщение об ошибке должно содержать слово 'null'.");

        checkTaskCountForThreeTasks();
        checkTasksUnchangedForThreeTaskList(task1, epic1, subTask1);
    }

    @Test
    public void MustThrowExceptionIfUsingUpdateTaskMethodToUpdateEpicOrSubTask() {
        createThreeTaskListForTests();

        Epic epicInsteadOfTask = new Epic(task1.getId(), "Эпик вместо задачи", "Описание");
        SubTask subTaskInsteadOfTask = new SubTask(task1.getId(), "Подзадача вместо задачи",
                "Описание", Status.DONE, epic1.getId());

        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class,
                () -> manager.updateTask(epicInsteadOfTask));
        assertTrue(ex1.getMessage().contains("must be updated using their own methods"),
                "Сообщение об ошибке должно содержать слово 'must be updated using their own methods'.");
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class,
                () -> manager.updateTask(subTaskInsteadOfTask));
        assertTrue(ex2.getMessage().contains("must be updated using their own methods"),
                "Сообщение об ошибке должно содержать слово 'must be updated using their own methods'.");

        checkTaskCountForThreeTasks();
        checkTasksUnchangedForThreeTaskList(task1, epic1, subTask1);
    }

    @Test
    public void updateTaskMustThrowExceptionIfUpdatingTaskWithInvalidId() {
        createThreeTaskListForTests();

        Task invalidIdTask = new Task(999, "ЗАДАЧА с несуществующим id", "описание", Status.DONE);
        Epic invalidIdEpic = new Epic(1000, "ЭПИК с несуществующим id", "описание");
        SubTask invalidIdSubTask = new SubTask(1001, "ПОДЗАДАЧА с несуществующим id",
                "описание", Status.DONE, epic1.getId());

        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class,
                () -> manager.updateTask(invalidIdTask));
        assertTrue(ex1.getMessage().contains("not found in TaskManager"),
                "Сообщение об ошибке должно содержать слово 'not found in TaskManager'.");
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class,
                () -> manager.updateEpic(invalidIdEpic));
        assertTrue(ex2.getMessage().contains("not found in TaskManager"),
                "Сообщение об ошибке должно содержать слово 'not found in TaskManager'.");
        IllegalArgumentException ex3 = assertThrows(IllegalArgumentException.class,
                () -> manager.updateSubTask(invalidIdSubTask));
        assertTrue(ex3.getMessage().contains("not found in TaskManager"),
                "Сообщение об ошибке должно содержать слово 'not found in TaskManager'.");

        checkTaskCountForThreeTasks();
        checkTasksUnchangedForThreeTaskList(task1, epic1, subTask1);
    }

    @Test
    public void updateTaskMustCorrectlyTransferEpicSubTaskIdList() {
        Epic epic1 = new Epic("ЭПИК", "описание");
        manager.addEpic(epic1);
        SubTask subTask1 = new SubTask("ПОДЗАДАЧА", "описание", 1);
        SubTask subTask2 = new SubTask("ПОДЗАДАЧА", "описание", 1);
        SubTask subTask3 = new SubTask("ПОДЗАДАЧА", "описание", 1);
        manager.addSubTask(subTask1);
        manager.addSubTask(subTask2);
        manager.addSubTask(subTask3);

        final Epic initialEpic = (Epic) manager.getTaskById(1).orElseThrow();

        Epic updateEpic = new Epic(1, "НОВЫЙ ЭПИК", "CHANGED");
        assertTrue(updateEpic.getSubTaskIdList().isEmpty(), "subTaskIdList эпика для передачи на " +
                "обновление должен быть пустым.");

        manager.updateEpic(updateEpic);

        final Epic epicToCheck = (Epic) manager.getTaskById(1).orElseThrow();

        assertEquals(updateEpic.getSubTaskIdList(), initialEpic.getSubTaskIdList(),
                "Update Epic должен скопировать в новый эпик subTaskIdList прошлого экземпляра эпика.");
        assertEquals(epicToCheck.getSubTaskIdList(), initialEpic.getSubTaskIdList(),
                "Update Epic должен скопировать в новый эпик subTaskIdList прошлого экземпляра эпика.");
        assertEquals(epicToCheck.getName(), updateEpic.getName(),
                "Имя эпика должно измениться.");
        assertEquals(epicToCheck.getDescription(), updateEpic.getDescription(),
                "Описание эпика должно измениться.");

        checkTaskCountCustom(0, 1, 3, 4);
    }

    @Test
    public void updateTaskMustThrowExceptionIfUpdatingEpicHasSubTaskIdList() {
        Epic epic1 = new Epic("ЭПИК", "описание");
        manager.addEpic(epic1);
        SubTask subTask1 = new SubTask("ПОДЗАДАЧА", "описание", 1);
        SubTask subTask2 = new SubTask("ПОДЗАДАЧА", "описание", 1);
        SubTask subTask3 = new SubTask("ПОДЗАДАЧА", "описание", 1);
        manager.addSubTask(subTask1);
        manager.addSubTask(subTask2);
        manager.addSubTask(subTask3);

        final List<Task> tasksToCheck = manager.getAllTasks();

        Epic updateEpicWithSubTaskIds = new Epic(1, "НОВЫЙ ЭПИК со своим subTaskIdList", "CHANGED");
        List<Integer> updateEpicSubTaskIdList = new ArrayList<>();
        updateEpicSubTaskIdList.add(999);
        updateEpicSubTaskIdList.add(1000);
        updateEpicSubTaskIdList.add(1001);
        updateEpicWithSubTaskIds.setSubTaskIdList(updateEpicSubTaskIdList);

        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class,
                () -> manager.updateEpic(updateEpicWithSubTaskIds));
        assertTrue(ex1.getMessage().contains("subTaskIdList must be empty"),
                "Сообщение об ошибке должно содержать слово 'subTaskIdList must be empty'.");

        checkTaskCountCustom(0, 1, 3, 4);
        checkTasksUnchangedCustom(tasksToCheck, manager.getAllTasks());
    }

    @Test
    public void updateTaskMustThrowExceptionIfUpdatingSubTaskHasNoEpic() {
        createThreeTaskListForTests();

        SubTask noEpicIdSubTask = new SubTask(subTask1.getId(), "epicId ПОДЗАДАЧИ ссылается на id ЗАДАЧИ вместо ЭПИКА",
                "CHANGED", Status.DONE, task1.getId());

        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class,
                () -> manager.updateSubTask(noEpicIdSubTask));
        assertTrue(ex1.getMessage().contains("EpicId"),
                "Сообщение об ошибке должно содержать слово 'EpicId'.");

        checkTaskCountForThreeTasks();
        checkTasksUnchangedForThreeTaskList(task1, epic1, subTask1);
    }

    @Test
    public void updateTaskMustThrowExceptionIfSubTaskNotInThisEpicList() {
        createThreeTaskListForTests();
        Epic epic2 = new Epic("ЭПИК2", "описание");
        manager.addEpic(epic2);
        SubTask subTask2 = new SubTask("ПОДЗАДАЧА 2 ЭПИКА", "описание", 4);
        manager.addSubTask(subTask2);

        final List<Task> tasksToCheck = manager.getAllTasks();

        SubTask wrongEpicIdSubTask = new SubTask(3,
                "id ПОДЗАДАЧИ ссылается на ПЕРВЫЙ ЭПИК, epicId ПОДЗАДАЧИ ссылается на id ВТОРОГО ЭПИКА",
                "CHANGED", Status.DONE, 4);

        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class,
                () -> manager.updateSubTask(wrongEpicIdSubTask));
        assertTrue(ex1.getMessage().contains("there is no SubTask with Id"),
                "Сообщение об ошибке должно содержать слово 'there is no SubTask with Id'.");

        checkTaskCountCustom(1, 2, 2, 5);
        checkTasksUnchangedCustom(tasksToCheck, manager.getAllTasks());
    }

    //ТЕСТЫ getHistory.
    @Test
    public void getHistoryMustUpdateTasksIfTaskManagerUpdatesThem() {
        createThreeTaskListForTests();

        final List<Task> checkList = new ArrayList<>();
        checkList.add(manager.getTaskById(task1.getId()).orElseThrow());
        checkList.add(manager.getTaskById(epic1.getId()).orElseThrow());
        checkList.add(manager.getTaskById(subTask1.getId()).orElseThrow());

        checkTasksUnchangedCustom(checkList, manager.getHistory());

        Task updateTask = new Task(task1.getId(), "ЗАДАЧА ИЗМЕНЕНА", "ЗАДАЧА ИЗМЕНЕНА", Status.DONE);
        Epic updateEpic = new Epic(epic1.getId(), "ЭПИК ИЗМЕНЕН", "ЭПИК ИЗМЕНЕН");
        SubTask updateSubTask = new SubTask(subTask1.getId(),
                "ПОДЗАДАЧА ИЗМЕНЕНА", "ПОДЗАДАЧА ИЗМЕНЕНА", Status.DONE, epic1.getId());

        manager.updateTask(updateTask);
        manager.updateEpic(updateEpic);
        manager.updateSubTask(updateSubTask);

        checkList.clear();

        checkList.add(manager.getTaskById(updateTask.getId()).orElseThrow());
        checkList.add(manager.getTaskById(updateEpic.getId()).orElseThrow());
        checkList.add(manager.getTaskById(updateSubTask.getId()).orElseThrow());

        checkTasksUnchangedCustom(checkList, manager.getHistory());
    }

    @Test
    public void getHistoryMustDeleteTasksIfTaskManagerDeletesThem() {
        createSixTaskListForTests();

        manager.getTaskById(task1.getId()).orElseThrow();
        manager.getTaskById(epic1.getId()).orElseThrow();
        manager.getTaskById(epic2.getId()).orElseThrow();
        manager.getTaskById(subTask1.getId()).orElseThrow();
        manager.getTaskById(subTask2.getId()).orElseThrow();
        manager.getTaskById(subTask3.getId()).orElseThrow();

        manager.deleteTaskById(task1.getId());
        List<Task> history = manager.getHistory();
        assertFalse(history.contains(task1), "Задача должна быть удалена из истории.");

        manager.deleteTaskById(subTask3.getId());
        history = manager.getHistory();
        assertFalse(history.contains(subTask3), "Подзадача должна быть удалена из HistoryManager.");
        assertTrue(history.contains(epic2), "Эпик подзадачи не должен быть удален из HistoryManager.");

        manager.deleteTaskById(epic1.getId());
        history = manager.getHistory();
        assertFalse(history.contains(epic1), "Эпик должен быть удален из HistoryManager.");
        assertFalse(history.contains(subTask1), "Подзадача эпика должна быть удалена из HistoryManager.");
        assertFalse(history.contains(subTask2), "Подзадача эпика должна быть удалена из HistoryManager.");

        manager.deleteTaskById(epic2.getId());
        history = manager.getHistory();
        assertTrue(history.isEmpty(), "Все задачи должны быть удалены из HistoryManager.");
    }

    //ТЕСТЫ resetEpicStatus.
    @Test
    public void resetEpicStatusWorksProperly() {
        Epic epic1 = new Epic("ЭПИК", "описание");
        manager.addEpic(epic1);


        Epic savedEpic = (Epic) manager.getTaskById(1).orElseThrow();
        assertEquals(Status.NEW, savedEpic.getStatus(),
                "Статус нового эпика должен быть NEW.");

        SubTask subTask1 = new SubTask("ПОДЗАДАЧА1", "описание", 1);
        SubTask subTask2 = new SubTask("ПОДЗАДАЧА2", "описание", 1);

        manager.addSubTask(subTask1);
        manager.addSubTask(subTask2);
        savedEpic = (Epic) manager.getTaskById(1).orElseThrow();
        assertEquals(Status.NEW, savedEpic.getStatus(),
                "Статус эпика после добавления новых подзадач должен быть NEW.");

        SubTask updateTask1 = new SubTask(2,
                "ПОДЗАДАЧА1 ВЫПОЛНЕНА", "описание", Status.DONE, 1);
        SubTask updateTask2 = new SubTask(3,
                "ПОДЗАДАЧА2 ОБНОВЛЕНА", "описание", Status.IN_PROGRESS, 1);

        manager.updateSubTask(updateTask1);
        savedEpic = (Epic) manager.getTaskById(1).orElseThrow();
        assertEquals(Status.IN_PROGRESS, savedEpic.getStatus(),
                "Статус эпика после должен быть IN_PROGRESS пока все задачи не NEW и не DONE.");
        manager.updateSubTask(updateTask2);
        savedEpic = (Epic) manager.getTaskById(1).orElseThrow();
        assertEquals(Status.IN_PROGRESS, savedEpic.getStatus(),
                "Статус эпика после должен быть IN_PROGRESS пока все задачи не NEW и не DONE.");

        SubTask updateSubTask2_2 = new SubTask(3,
                "ПОДЗАДАЧА2 ВЫПОЛНЕНА", "описание", Status.DONE, 1);

        manager.updateSubTask(updateSubTask2_2);
        savedEpic = (Epic) manager.getTaskById(1).orElseThrow();
        assertEquals(Status.DONE, savedEpic.getStatus(),
                "Статус эпика должен быть DONE если все задачи DONE.");

        SubTask newSubTask = new SubTask("ПОДЗАДАЧА3", "описание", 1);

        manager.addSubTask(newSubTask);
        savedEpic = (Epic) manager.getTaskById(1).orElseThrow();
        assertEquals(Status.IN_PROGRESS, savedEpic.getStatus(),
                "Статус эпика DONE должен смениться на IN_PROGRESS при добавлении NEW задачи.");

        manager.deleteTaskById(4);
        savedEpic = (Epic) manager.getTaskById(1).orElseThrow();
        assertEquals(Status.DONE, savedEpic.getStatus(),
                "Статус эпика IN_PROGRESS должен смениться на DONE при удалении NEW задачи.");

        manager.deleteTaskById(2);
        manager.deleteTaskById(3);
        savedEpic = (Epic) manager.getTaskById(1).orElseThrow();
        assertEquals(Status.NEW, savedEpic.getStatus(),
                "Статус эпика DONE должен смениться на NEW при удалении всех задач.");
    }

    //ТЕСТЫ на инкапсуляцию
    @Test
    public void changesInEpicToAddMustNotAffectEpicInTaskManager() {
        Epic externalEpic = new Epic("ИМЯ", "ОПИСАНИЕ");
        manager.addEpic(externalEpic);

        externalEpic.setStatus(Status.DONE);
        List<Integer> subTaskIds = new ArrayList<>();
        subTaskIds.add(2);
        subTaskIds.add(3);
        subTaskIds.add(999);
        externalEpic.setSubTaskIdList(subTaskIds);

        Epic inManagerEpic = (Epic) manager.getTaskById(1).orElseThrow();

        assertNotSame(externalEpic, inManagerEpic, "Эпики не должны совпадать по хэшу.");
        assertEquals(0, externalEpic.getId(),
                "До добавления в менеджер id должно быть равно 0.");
        assertEquals(1, inManagerEpic.getId(),
                "При добавлении в менеджер id должно быть равно 1.");

        assertEquals(externalEpic.getName(), inManagerEpic.getName(),
                "Имя эпика должно совпадать.");
        assertEquals(externalEpic.getDescription(), inManagerEpic.getDescription(),
                "Описание эпика должно совпадать.");

        assertEquals(Status.DONE, externalEpic.getStatus(),
                "Статус эпика вне менеджера должен измениться на DONE.");
        assertEquals(Status.NEW, inManagerEpic.getStatus(),
                "Статус эпика в менеджере должен быть NEW.");

        assertFalse(externalEpic.getSubTaskIdList().isEmpty(),
                "В список subTaskIds эпика вне менеджера должны быть добавлены id.");
        assertTrue(inManagerEpic.getSubTaskIdList().isEmpty(),
                "Список subTaskIds эпика в менеджере должен быть пустым.");
    }

    @Test
    public void changesInEpicToUpdateMustNotAffectEpicInTaskManager() {
        manager.addEpic(new Epic("ИМЯ", "ОПИСАНИЕ"));

        Epic externalEpic = new Epic(1, "ИМЯ ИЗМЕНЕНО", "ОПИСАНИЕ ИЗМЕНЕНО");
        manager.updateEpic(externalEpic);

        externalEpic.setStatus(Status.DONE);
        List<Integer> subTaskIds = new ArrayList<>();
        subTaskIds.add(2);
        subTaskIds.add(3);
        subTaskIds.add(999);
        externalEpic.setSubTaskIdList(subTaskIds);

        Epic inManagerEpic = (Epic) manager.getTaskById(1).orElseThrow();

        assertEquals(externalEpic, inManagerEpic, "Эпики должны совпадать по хэшу.");
        assertEquals(externalEpic.getId(), inManagerEpic.getId(),
                "id эпика должно совпадать.");
        assertEquals(externalEpic.getName(), inManagerEpic.getName(),
                "Имя эпика должно совпадать.");
        assertEquals(externalEpic.getDescription(), inManagerEpic.getDescription(),
                "Описание эпика должно совпадать.");

        assertEquals(Status.DONE, externalEpic.getStatus(),
                "Статус эпика вне менеджера должен измениться на DONE.");
        assertEquals(Status.NEW, inManagerEpic.getStatus(),
                "Статус эпика в менеджере должен быть NEW.");

        assertFalse(externalEpic.getSubTaskIdList().isEmpty(),
                "В список subTaskIds эпика вне менеджера должны быть добавлены id.");
        assertTrue(inManagerEpic.getSubTaskIdList().isEmpty(),
                "Список subTaskIds эпика в менеджере должен быть пустым.");
    }

    @Test
    public void changesInReturnedEpicMustNotAffectEpicInTaskManager() {
        manager.addEpic(new Epic("ИМЯ", "ОПИСАНИЕ"));

        Epic externalEpic = (Epic) manager.getTaskById(1).orElseThrow();
        externalEpic.setStatus(Status.DONE);
        List<Integer> subTaskIds = new ArrayList<>();
        subTaskIds.add(2);
        subTaskIds.add(3);
        subTaskIds.add(999);
        externalEpic.setSubTaskIdList(subTaskIds);

        Epic inManagerEpic = (Epic) manager.getTaskById(1).orElseThrow();

        assertEquals(externalEpic, inManagerEpic, "Эпики должны совпадать по хэшу.");
        assertEquals(externalEpic.getId(), inManagerEpic.getId(),
                "id эпика должно совпадать.");
        assertEquals(externalEpic.getName(), inManagerEpic.getName(),
                "Имя эпика должно совпадать.");
        assertEquals(externalEpic.getDescription(), inManagerEpic.getDescription(),
                "Описание эпика должно совпадать.");

        assertEquals(Status.DONE, externalEpic.getStatus(),
                "Статус эпика вне менеджера должен измениться на DONE.");
        assertEquals(Status.NEW, inManagerEpic.getStatus(),
                "Статус эпика в менеджере должен быть NEW.");

        assertFalse(externalEpic.getSubTaskIdList().isEmpty(),
                "В список subTaskIds эпика вне менеджера должны быть добавлены id.");
        assertTrue(inManagerEpic.getSubTaskIdList().isEmpty(),
                "Список subTaskIds эпика в менеджере должен быть пустым.");
    }

    @Test
    public void changesInEpicTakenFromHistoryManagerMustNotAffectEpicInTaskManager() {
        manager.addEpic(new Epic("ИМЯ", "ОПИСАНИЕ"));
        manager.getTaskById(1);

        Epic externalEpic = (Epic) manager.getHistory().getFirst();
        externalEpic.setStatus(Status.DONE);
        List<Integer> subTaskIds = new ArrayList<>();
        subTaskIds.add(2);
        subTaskIds.add(3);
        subTaskIds.add(999);
        externalEpic.setSubTaskIdList(subTaskIds);

        Epic inManagerEpic = (Epic) manager.getTaskById(1).orElseThrow();

        assertEquals(externalEpic, inManagerEpic, "Эпики должны совпадать по хэшу.");
        assertEquals(externalEpic.getId(), inManagerEpic.getId(),
                "id эпика должно совпадать.");
        assertEquals(externalEpic.getName(), inManagerEpic.getName(),
                "Имя эпика должно совпадать.");
        assertEquals(externalEpic.getDescription(), inManagerEpic.getDescription(),
                "Описание эпика должно совпадать.");

        assertEquals(Status.DONE, externalEpic.getStatus(),
                "Статус эпика вне менеджера должен измениться на DONE.");
        assertEquals(Status.NEW, inManagerEpic.getStatus(),
                "Статус эпика в менеджере должен быть NEW.");

        assertFalse(externalEpic.getSubTaskIdList().isEmpty(),
                "В список subTaskIds эпика вне менеджера должны быть добавлены id.");
        assertTrue(inManagerEpic.getSubTaskIdList().isEmpty(),
                "Список subTaskIds эпика в менеджере должен быть пустым.");
    }

    //ТЕСТЫ остальных методов.
    @Test
    public void getTasksWorksProperly() {
        createSixTaskListForTests();

        List<Task> allTasks = new ArrayList<>();
        allTasks.add(task1);
        allTasks.add(epic1);
        allTasks.add(epic2);
        allTasks.add(subTask1);
        allTasks.add(subTask2);
        allTasks.add(subTask3);

        checkTasksUnchangedCustom(allTasks, manager.getAllTasks());

        List<Task> tasks = new ArrayList<>();
        tasks.add(task1);

        checkTasksUnchangedCustom(tasks, manager.getTasks());

        List<Epic> epics = new ArrayList<>();
        epics.add(epic1);
        epics.add(epic2);

        checkTasksUnchangedCustom(epics, manager.getEpics());

        List<SubTask> subTasks = new ArrayList<>();
        subTasks.add(subTask1);
        subTasks.add(subTask2);
        subTasks.add(subTask3);

        checkTasksUnchangedCustom(subTasks, manager.getSubTasks());
    }

    @Test
    public void getTaskByIdMustReturnEmptyOptionalIfIdIsWrong() {
        Task task1 = new Task("Задача", "Описание");
        manager.addTask(task1);

        Optional<Task> wrongTask = manager.getTaskById(-999);
        assertFalse(wrongTask.isPresent(),
                "Должно возвращаться Optional.empty() при неправильном id.");

        wrongTask = manager.getTaskById(0);
        assertFalse(wrongTask.isPresent(),
                "Должно возвращаться Optional.empty() при неправильном id.");

        wrongTask = manager.getTaskById(999);
        assertFalse(wrongTask.isPresent(),
                "Должно возвращаться Optional.empty() при неправильном id.");
    }

    @Test
    public void getEpicSubTasksMustReturnCorrectSubTasksOrEmptyList() {
        createSixTaskListForTests();

        final List<SubTask> testSubTaskList = new ArrayList<>();
        testSubTaskList.add(subTask1);
        testSubTaskList.add(subTask2);

        final List<SubTask> epicSubTasks = manager.getEpicSubTasks(epic1.getId());
        assertEquals(2, epicSubTasks.size(), "Эпик должен содержать 2 подзадачи.");
        assertEquals(testSubTaskList, epicSubTasks, "Выданные методом подзадачи не совпадают с ожидаемыми.");

        final List<SubTask> wrongIdSubTasks = manager.getEpicSubTasks(999);
        assertNotNull(wrongIdSubTasks, "При вводе неверного id не должен возвращаться null.");
        assertTrue(wrongIdSubTasks.isEmpty(), "При вводе неверного id должен возвращаться пустой список.");

        Epic emptyEpic = new Epic("Пустой эпик", "Описание");
        manager.addEpic(emptyEpic);

        final List<SubTask> emptyEpicSubTasks = manager.getEpicSubTasks(emptyEpic.getId());
        assertNotNull(emptyEpicSubTasks,
                "При возвращении списка подзадач пустого эпика не должен возвращаться null.");
        assertTrue(emptyEpicSubTasks.isEmpty(),
                "При возвращении списка подзадач пустого эпика должен возвращаться пустой список.");
    }

    //ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ.
    private void createSixTaskListForTests() {
        Task newTask = new Task("ЗАДАЧА", "Описание");
        Epic newEpic1 = new Epic("ЭПИК1", "Описание");
        Epic newEpic2 = new Epic("ЭПИК2", "Описание");
        manager.addTask(newTask);
        manager.addEpic(newEpic1);
        manager.addEpic(newEpic2);
        SubTask newSubTask1 = new SubTask("ПОДЗАДАЧА1-1", "Описание", 2);
        SubTask newSubTask2 = new SubTask("ПОДЗАДАЧА1-2", "Описание", 2);
        SubTask newSubTask3 = new SubTask("ПОДЗАДАЧА2-1", "Описание", 3);
        manager.addSubTask(newSubTask1);
        manager.addSubTask(newSubTask2);
        manager.addSubTask(newSubTask3);

        task1 = manager.getTaskById(1).orElseThrow();
        epic1 = (Epic) manager.getTaskById(2).orElseThrow();
        epic2 = (Epic) manager.getTaskById(3).orElseThrow();
        subTask1 = (SubTask) manager.getTaskById(4).orElseThrow();
        subTask2 = (SubTask) manager.getTaskById(5).orElseThrow();
        subTask3 = (SubTask) manager.getTaskById(6).orElseThrow();

        checkTaskCountForSixTasks();
    }

    private void createThreeTaskListForTests() {
        Task newTask = new Task("ЗАДАЧА", "ОПИСАНИЕ ЗАДАЧИ");
        Epic newEpic = new Epic("ЭПИК", "ОПИСАНИЕ ЭПИКА");
        manager.addTask(newTask);
        manager.addEpic(newEpic);
        SubTask newSubTask = new SubTask("ПОДЗАДАЧА", "ОПИСАНИЕ ПОДЗАДАЧИ", 2);
        manager.addSubTask(newSubTask);

        task1 = manager.getTaskById(1).orElseThrow();
        epic1 = (Epic) manager.getTaskById(2).orElseThrow();
        subTask1 = (SubTask) manager.getTaskById(3).orElseThrow();

        checkTaskCountForThreeTasks();
    }

    private void checkTaskCountForSixTasks() {
        assertEquals(1, manager.getTasks().size(), "В списке задач должна быть 1 задача.");
        assertEquals(2, manager.getEpics().size(), "В списке эпиков должно быть 2 эпика.");
        assertEquals(3, manager.getSubTasks().size(), "В списке подзадач должно быть 3 подзадачи.");
        assertEquals(6, manager.getIdCounter(), "Счетчик менеджера должен быть равен 6.");
    }

    private void checkTaskCountForThreeTasks() {
        assertEquals(1, manager.getTasks().size(), "В списке задач должна быть 1 задача.");
        assertEquals(1, manager.getEpics().size(), "В списке эпиков должен быть 1 эпик.");
        assertEquals(1, manager.getSubTasks().size(), "В списке подзадач должна быть 1 подзадача.");
        assertEquals(3, manager.getIdCounter(), "Счетчик менеджера должен быть равен 3.");
    }

    private void checkTaskCountCustom(int tasks, int epics, int subTasks, int taskCount) {
        assertEquals(tasks, manager.getTasks().size(), "В списке задач должно быть " + tasks + " задач.");
        assertEquals(epics, manager.getEpics().size(), "В списке эпиков должно быть " + epics + " эпиков.");
        assertEquals(subTasks, manager.getSubTasks().size(), "В списке подзадач должно быть " + subTasks + " подзадач.");
        assertEquals(taskCount, manager.getIdCounter(), "Счетчик менеджера должен быть равен " + taskCount + ".");
    }

    private void checkTaskCountForEmptyManager() {
        assertEquals(0, manager.getTasks().size(), "Список задач должен быть пуст.");
        assertEquals(0, manager.getEpics().size(), "Список эпиков должен быть пуст.");
        assertEquals(0, manager.getSubTasks().size(), "Список подзадач должен быть пуст.");
        assertEquals(0, manager.getIdCounter(), "Счетчик менеджера должен быть равен 0.");
    }

    private void checkTasksUnchangedForThreeTaskList(Task initialTask, Epic initialEpic, SubTask initialSubTask) {
        Task taskToCheck = manager.getTaskById(initialTask.getId()).orElseThrow();
        Epic epicToCheck = (Epic) manager.getTaskById(initialEpic.getId()).orElseThrow();
        SubTask subTaskToCheck = (SubTask) manager.getTaskById(initialSubTask.getId()).orElseThrow();

        assertEquals(initialTask.getName(), taskToCheck.getName(), "Имя задачи изменилось.");
        assertEquals(initialEpic.getName(), epicToCheck.getName(), "Имя эпика изменилось.");
        assertEquals(initialSubTask.getName(), subTaskToCheck.getName(), "Имя подзадачи изменилось.");

        assertEquals(initialTask.getDescription(), taskToCheck.getDescription(), "Описание задачи изменилось.");
        assertEquals(initialEpic.getDescription(), epicToCheck.getDescription(), "Описание эпика изменилось.");
        assertEquals(initialSubTask.getDescription(), subTaskToCheck.getDescription(), "Описание подзадачи изменилось.");

        assertEquals(initialTask.getStatus(), taskToCheck.getStatus(), "Статус задачи изменился.");
        assertEquals(initialEpic.getStatus(), epicToCheck.getStatus(), "Статус эпика изменился.");
        assertEquals(initialSubTask.getStatus(), subTaskToCheck.getStatus(), "Статус подзадачи изменился.");
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