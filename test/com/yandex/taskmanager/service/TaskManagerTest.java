package com.yandex.taskmanager.service;

import com.yandex.taskmanager.exceptions.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import com.yandex.taskmanager.model.Task;
import com.yandex.taskmanager.model.Epic;
import com.yandex.taskmanager.model.SubTask;
import com.yandex.taskmanager.model.Status;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

abstract class TaskManagerTest<T extends TaskManager> {
    protected T manager;

    protected Task task1;
    protected Epic epic1;
    protected Epic epic2;
    protected SubTask subTask1;
    protected SubTask subTask2;
    protected SubTask subTask3;

    LocalDateTime now;

    protected abstract T createManager();

    @BeforeEach
    void setUp() {
        manager = createManager();
        now = LocalDate.now().atStartOfDay();
    }

    // ТЕСТЫ addTask.
    @Test
    public void addAndRetrieveTasksWorksCorrectly() {
        createSixTaskListForTests(manager);

        final Optional<Task> optionalTask = manager.findAnyTaskById(task1.getId());
        final Optional<Task> optionalEpic = manager.findAnyTaskById(epic1.getId());
        final Optional<Task> optionalSubTask = manager.findAnyTaskById(subTask1.getId());

        assertTrue(optionalTask.isPresent(), "Задача не находиться по id.");
        assertTrue(optionalEpic.isPresent(), "Эпик не находиться по id.");
        assertTrue(optionalSubTask.isPresent(), "Подзадача не находиться по id.");

        final Task savedTask = optionalTask.get();
        final Epic savedEpic = (Epic) optionalEpic.get();
        final SubTask savedSubTask = (SubTask) optionalSubTask.get();

        assertEquals(task1, savedTask, "Задачи не совпадают по id (хэш определен только по id).");
        assertEquals(epic1, savedEpic, "Эпики не совпадают по id (хэш определен только по id).");
        assertEquals(subTask1, savedSubTask, "Подзадачи не совпадают по id (хэш определен только по id).");

        checkTasksUnchangedForThreeTaskList(manager, task1, epic1, subTask1);

        final List<Task> managerList = manager.getAllTasks();
        final List<Task> tasks = manager.getTasks();
        final List<Epic> epics = manager.getEpics();
        final List<SubTask> subTasks = manager.getSubTasks();

        assertNotNull(managerList, "Список всех сохраненных объектов не возвращается.");
        assertNotNull(tasks, "Задачи не возвращаются.");
        assertNotNull(epics, "Эпики не возвращаются.");
        assertNotNull(subTasks, "Подзадачи не возвращаются.");

        checkTaskCountForSixTasks(manager);
    }

    @Test
    public void addTaskMustThrowExceptionIfCreateSameTaskTwice() {
        createThreeTaskListForTests(manager);

        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class,
                () -> manager.createTask(task1));
        assertTrue(ex1.getMessage().contains("is not new"),
                "Сообщение об ошибке Задачи должно содержать слово 'is not new'.");
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class,
                () -> manager.createEpic(epic1));
        assertTrue(ex2.getMessage().contains("is not new"),
                "Сообщение об ошибке Эпика должно содержать слово 'is not new'.");
        IllegalArgumentException ex3 = assertThrows(IllegalArgumentException.class,
                () -> manager.createSubTask(subTask1));
        assertTrue(ex3.getMessage().contains("is not new"),
                "Сообщение об ошибке Подзадачи должно содержать слово 'is not new'.");

        checkTaskCountForThreeTasks(manager);
    }

    @Test
    public void addTaskMustThrowExceptionIfCreateTaskWithStatusNotNew() {
        Task notNewStatusTask = new Task(0, "Задача", "Описание", Status.IN_PROGRESS);
        SubTask notNewStatusSubTask = new SubTask(0, "Подзадача", "Описание", Status.IN_PROGRESS, 1);
        Epic notNewStatusEpic = new Epic("Эпик", "Описание");
        notNewStatusEpic.setStatus(Status.IN_PROGRESS);

        manager.createEpic(new Epic("Эпик", "Для подзадачи"));

        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class,
                () -> manager.createTask(notNewStatusTask));
        assertTrue(ex1.getMessage().contains("is not new"),
                "Сообщение об ошибке должно содержать слово 'is not new'.");
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class,
                () -> manager.createEpic(notNewStatusEpic));
        assertTrue(ex2.getMessage().contains("is not new"),
                "Сообщение об ошибке должно содержать слово 'is not new'.");
        IllegalArgumentException ex3 = assertThrows(IllegalArgumentException.class,
                () -> manager.createSubTask(notNewStatusSubTask));
        assertTrue(ex3.getMessage().contains("is not new"),
                "Сообщение об ошибке должно содержать слово 'is not new'.");

        checkTaskCountCustom(manager, 0, 1, 0, 1);
    }

    @Test
    public void addTaskMustThrowExceptionIfCreateEpicWithExistingSubTaskIdList() {
        Epic newEpic = new Epic("Эпик", "Описание");
        List<Integer> subTaskIds = new ArrayList<>();
        subTaskIds.add(999);
        Epic notEmptyEpic = new Epic(newEpic, subTaskIds);

        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class,
                () -> manager.createEpic(notEmptyEpic));
        assertTrue(ex1.getMessage().contains("Cannot add Epic with non-empty subTaskIdList"),
                "Сообщение об ошибке должно содержать слово 'Cannot add Epic with non-empty subTaskIdList'.");

        checkTaskCountForEmptyManager(manager);
    }

    @Test
    public void addTaskMustThrowExceptionIfCreateTaskWithExistingId() {
        Task notNewIdTask = new Task(999, "Задача", "Описание");
        Epic notNewIdEpic = new Epic(1000, "Эпик", "Описание");
        SubTask notNewIdSubTask = new SubTask(1001, "Подзадача", "Описание", Status.DONE, 1);

        manager.createEpic(new Epic("Эпик", "Для подзадачи"));

        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class,
                () -> manager.createTask(notNewIdTask));
        assertTrue(ex1.getMessage().contains("is not new"),
                "Сообщение об ошибке должно содержать слово 'is not new'.");
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class,
                () -> manager.createEpic(notNewIdEpic));
        assertTrue(ex2.getMessage().contains("is not new"),
                "Сообщение об ошибке должно содержать слово 'is not new'.");
        IllegalArgumentException ex3 = assertThrows(IllegalArgumentException.class,
                () -> manager.createSubTask(notNewIdSubTask));
        assertTrue(ex3.getMessage().contains("is not new"),
                "Сообщение об ошибке должно содержать слово 'is not new'.");

        checkTaskCountCustom(manager, 0, 1, 0, 1);
    }

    @Test
    public void addTaskMustThrowExceptionIfCreateNullTask() {
        NullPointerException ex1 = assertThrows(NullPointerException.class,
                () -> manager.createTask(null));
        assertTrue(ex1.getMessage().contains("null"),
                "Сообщение об ошибке должно содержать слово 'null'.");
        NullPointerException ex2 = assertThrows(NullPointerException.class,
                () -> manager.createEpic(null));
        assertTrue(ex2.getMessage().contains("null"),
                "Сообщение об ошибке должно содержать слово 'null'.");
        NullPointerException ex3 = assertThrows(NullPointerException.class,
                () -> manager.createSubTask(null));
        assertTrue(ex3.getMessage().contains("null"),
                "Сообщение об ошибке должно содержать слово 'null'.");

        checkTaskCountForEmptyManager(manager);
    }

    @Test
    public void addTaskMustThrowExceptionIfCreateSubTaskWithInvalidEpicId() {
        SubTask invalidEpicIdSubTask = new SubTask("Подзадача", "Описание", 999);

        NotFoundException ex1 = assertThrows(NotFoundException.class,
                () -> manager.createSubTask(invalidEpicIdSubTask));
        assertTrue(ex1.getMessage().contains("Epic with ID"),
                "Сообщение об ошибке должно содержать слово 'Epic with ID'.");

        checkTaskCountForEmptyManager(manager);
    }

    @Test
    public void addTaskMustThrowExceptionIfCreateSubTaskAsItsOwnEpic() {
        Epic epic1 = new Epic("Эпик", "Описание");
        manager.createEpic(epic1);
        SubTask subTask1 = new SubTask("Подзадача1", "Описание", 1);
        manager.createSubTask(subTask1);

        SubTask subTaskAsOwnEpic = new SubTask("Подзадача1", "id подзадачи, вместо epicId", 2);

        NotFoundException ex1 = assertThrows(NotFoundException.class,
                () -> manager.createSubTask(subTaskAsOwnEpic));
        assertTrue(ex1.getMessage().contains("Epic with ID"),
                "Сообщение об ошибке должно содержать слово 'Epic with ID'.");

        checkTaskCountCustom(manager, 0, 1, 1, 2);
    }

    @Test
    public void MustThrowExceptionIfUsingAddTaskMethodToCreateEpicOrSubTask() {
        Epic epic1 = new Epic("Эпик", "Описание");
        SubTask subTask1 = new SubTask("Подзадача1", "Описание", epic1.getId());

        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class,
                () -> manager.createTask(epic1));
        assertTrue(ex1.getMessage().contains("using their own methods"),
                "Сообщение об ошибке должно содержать слово 'using their own methods'.");
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class,
                () -> manager.createTask(subTask1));
        assertTrue(ex2.getMessage().contains("using their own methods"),
                "Сообщение об ошибке должно содержать слово 'using their own methods'.");

        checkTaskCountForEmptyManager(manager);
    }

    //ТЕСТЫ deleteById и deleteAll.
    @Test
    public void deleteEpicByIdMustRemoveItsSubTasks() {
        createSixTaskListForTests(manager);

        manager.deleteAnyTaskById(epic1.getId());

        checkTaskCountCustom(manager, 1, 1, 1, 6);

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
        createSixTaskListForTests(manager);

        assertTrue(epic1.getSubTaskIdList().contains(subTask1.getId()),
                "id подзадачи должна быть в списке эпика.");

        manager.deleteAnyTaskById(subTask1.getId());

        checkTaskCountCustom(manager, 1, 2, 2, 6);

        Epic epicInManager = (Epic) manager.findAnyTaskById(epic1.getId()).orElseThrow();

        assertFalse(manager.getAllTasks().contains(subTask1),
                "Подзадача должна быть удален из списка задач.");
        assertFalse(manager.getSubTasks().contains(subTask1),
                "Подзадача должны быть удалены из списка подзадач.");
        assertFalse(epicInManager.getSubTaskIdList().contains(subTask1.getId()),
                "id подзадачи должно быть удалено из списке эпика.");
    }

    @Test
    public void deleteTaskByIdMustRemoveItFromAnyTaskList() {
        createSixTaskListForTests(manager);

        manager.deleteAnyTaskById(task1.getId());

        checkTaskCountCustom(manager, 0, 2, 3, 6);
    }

    @Test
    public void deleteAnyTaskByIdMustThrowExceptionIfIdInvalid() {
        createSixTaskListForTests(manager);

        NotFoundException ex1 = assertThrows(NotFoundException.class,
                () -> manager.deleteAnyTaskById(-999));
        assertTrue(ex1.getMessage().contains("not found in TaskManager"),
                "Сообщение об ошибке должно содержать слово 'not found in TaskManager'.");
        checkTaskCountForSixTasks(manager);

        NotFoundException ex2 = assertThrows(NotFoundException.class,
                () -> manager.deleteAnyTaskById(0));
        assertTrue(ex2.getMessage().contains("not found in TaskManager"),
                "Сообщение об ошибке должно содержать слово 'not found in TaskManager'.");
        checkTaskCountForSixTasks(manager);

        NotFoundException ex3 = assertThrows(NotFoundException.class,
                () -> manager.deleteAnyTaskById(999));
        assertTrue(ex3.getMessage().contains("not found in TaskManager"),
                "Сообщение об ошибке должно содержать слово 'not found in TaskManager'.");
        checkTaskCountForSixTasks(manager);
    }

    @Test
    public void deleteAllTasksMustClearAllListsAndResetTaskCount() {
        createSixTaskListForTests(manager);

        manager.findAnyTaskById(task1.getId());
        manager.findAnyTaskById(epic1.getId());
        manager.findAnyTaskById(epic2.getId());
        manager.findAnyTaskById(subTask1.getId());
        manager.findAnyTaskById(subTask2.getId());
        manager.findAnyTaskById(subTask3.getId());

        assertEquals(6, manager.getHistory().size(),
                "Количество записей в истории должно быть равно 6.");
        checkTaskCountCustom(manager, 1, 2, 3, 6);

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
    public void updateTaskWorksCorrectly() {
        createSixTaskListForTests(manager);

        Task updateTask = new Task(task1.getId(), "ЗАДАЧА ОБНОВЛЕНА", "ЗАДАЧА ВЫПОЛНЕНА", Status.DONE);
        Epic updateEpic = new Epic(epic1.getId(), "ЭПИК ОБНОВЛЕН", "ЭПИК ОБНОВЛЕН");
        SubTask updateSubTask = new SubTask(subTask3.getId(), "ПОДЗАДАЧА ОБНОВЛЕНА", "ПОДЗАДАЧА ВЫПОЛНЕНА",
                Status.DONE, epic2.getId());

        manager.updateTask(updateTask);
        manager.updateEpic(updateEpic);
        manager.updateSubTask(updateSubTask);

        checkTaskCountForSixTasks(manager);

        final Optional<Task> optionalTask = manager.findAnyTaskById(task1.getId());
        final Optional<Task> optionalEpic = manager.findAnyTaskById(epic1.getId());
        final Optional<Task> optionalSubTask = manager.findAnyTaskById(subTask3.getId());

        assertTrue(optionalTask.isPresent(), "Задача не находиться по id.");
        assertTrue(optionalEpic.isPresent(), "Эпик не находиться по id.");
        assertTrue(optionalSubTask.isPresent(), "Подзадача не находиться по id.");

        checkTasksUnchangedForThreeTaskList(manager, updateTask, updateEpic, updateSubTask);
    }

    @Test
    public void updateTaskMustThrowExceptionIfTaskNull() {
        createThreeTaskListForTests(manager);

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

        checkTaskCountForThreeTasks(manager);
        checkTasksUnchangedForThreeTaskList(manager, task1, epic1, subTask1);
    }

    @Test
    public void MustThrowExceptionIfUsingUpdateTaskMethodToUpdateEpicOrSubTask() {
        createThreeTaskListForTests(manager);

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

        checkTaskCountForThreeTasks(manager);
        checkTasksUnchangedForThreeTaskList(manager, task1, epic1, subTask1);
    }

    @Test
    public void updateTaskMustThrowExceptionIfUpdatingTaskWithInvalidId() {
        createThreeTaskListForTests(manager);

        Task invalidIdTask = new Task(999, "ЗАДАЧА с несуществующим id", "описание", Status.DONE);
        Epic invalidIdEpic = new Epic(1000, "ЭПИК с несуществующим id", "описание");
        SubTask invalidIdSubTask = new SubTask(1001, "ПОДЗАДАЧА с несуществующим id",
                "описание", Status.DONE, epic1.getId());

        NotFoundException ex1 = assertThrows(NotFoundException.class,
                () -> manager.updateTask(invalidIdTask));
        assertTrue(ex1.getMessage().contains("not found in TaskManager"),
                "Сообщение об ошибке должно содержать слово 'not found in TaskManager'.");
        NotFoundException ex2 = assertThrows(NotFoundException.class,
                () -> manager.updateEpic(invalidIdEpic));
        assertTrue(ex2.getMessage().contains("not found in TaskManager"),
                "Сообщение об ошибке должно содержать слово 'not found in TaskManager'.");
        NotFoundException ex3 = assertThrows(NotFoundException.class,
                () -> manager.updateSubTask(invalidIdSubTask));
        assertTrue(ex3.getMessage().contains("not found in TaskManager"),
                "Сообщение об ошибке должно содержать слово 'not found in TaskManager'.");

        checkTaskCountForThreeTasks(manager);
        checkTasksUnchangedForThreeTaskList(manager, task1, epic1, subTask1);
    }

    @Test
    public void updateTaskMustCorrectlyTransferEpicSubTaskIdList() {
        List<Integer> threeSubTaskIdList = new ArrayList<>();
        threeSubTaskIdList.add(2);
        threeSubTaskIdList.add(3);
        threeSubTaskIdList.add(4);
        List<Integer> fourSubTaskIdList = new ArrayList<>(threeSubTaskIdList);
        fourSubTaskIdList.add(5);

        Epic epic1 = new Epic("ЭПИК", "описание");
        manager.createEpic(epic1);
        SubTask subTask1 = new SubTask("ПОДЗАДАЧА", "описание", 1);
        SubTask subTask2 = new SubTask("ПОДЗАДАЧА", "описание", 1);
        SubTask subTask3 = new SubTask("ПОДЗАДАЧА", "описание", 1);
        manager.createSubTask(subTask1);
        manager.createSubTask(subTask2);
        manager.createSubTask(subTask3);

        Epic epicInManager = (Epic) manager.findAnyTaskById(1).orElseThrow();
        assertEquals(threeSubTaskIdList, epicInManager.getSubTaskIdList(),
                "В начальный Epic должны записаться id трех подзадач.");

        Epic updateEpic = new Epic(1, "ОБНОВЛЕНЫЙ ЭПИК", "CHANGED");
        assertTrue(updateEpic.getSubTaskIdList().isEmpty(), "subTaskIdList эпика для передачи на " +
                "обновление должен быть пустым.");

        manager.updateEpic(updateEpic);
        checkTaskCountCustom(manager, 0, 1, 3, 4);

        epicInManager = (Epic) manager.findAnyTaskById(1).orElseThrow();

        assertEquals(threeSubTaskIdList, epicInManager.getSubTaskIdList(),
                "Update Epic должен скопировать в новый эпик subTaskIdList прошлого экземпляра эпика.");
        assertEquals(updateEpic.getName(), epicInManager.getName(),
                "Имя эпика должно измениться.");
        assertEquals(epicInManager.getDescription(), updateEpic.getDescription(),
                "Описание эпика должно измениться.");
        assertEquals(Status.NEW, epicInManager.getStatus(),
                "Статус эпика не должен измениться.");

        SubTask subTask4 = new SubTask("ПОДЗАДАЧА", "описание", 1);

        manager.createSubTask(subTask4);
        checkTaskCountCustom(manager, 0, 1, 4, 5);

        epicInManager = (Epic) manager.findAnyTaskById(1).orElseThrow();

        assertEquals(fourSubTaskIdList, epicInManager.getSubTaskIdList(),
                "В subTaskIdList эпика должна быть записана новая подзадача.");
    }

    @Test
    public void updateTaskMustThrowExceptionIfUpdatingEpicHasSubTaskIdList() {
        Epic epic1 = new Epic("ЭПИК", "описание");
        manager.createEpic(epic1);
        SubTask subTask1 = new SubTask("ПОДЗАДАЧА", "описание", 1);
        SubTask subTask2 = new SubTask("ПОДЗАДАЧА", "описание", 1);
        SubTask subTask3 = new SubTask("ПОДЗАДАЧА", "описание", 1);
        manager.createSubTask(subTask1);
        manager.createSubTask(subTask2);
        manager.createSubTask(subTask3);

        final List<Task> tasksToCheck = manager.getAllTasks();

        Epic updatingEpic = new Epic(1, "НОВЫЙ ЭПИК со своим subTaskIdList", "CHANGED");
        List<Integer> updatingEpicSubTaskIdList = new ArrayList<>();
        updatingEpicSubTaskIdList.add(999);
        updatingEpicSubTaskIdList.add(1000);
        updatingEpicSubTaskIdList.add(1001);
        Epic updatingEpicWithSubTaskIds = new Epic(updatingEpic, updatingEpicSubTaskIdList);

        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class,
                () -> manager.updateEpic(updatingEpicWithSubTaskIds));
        assertTrue(ex1.getMessage().contains("subTaskIdList must be empty"),
                "Сообщение об ошибке должно содержать слово 'subTaskIdList must be empty'.");

        checkTaskCountCustom(manager, 0, 1, 3, 4);
        checkTasksUnchangedCustom(tasksToCheck, manager.getAllTasks());
    }

    @Test
    public void updateTaskMustThrowExceptionIfUpdatingSubTaskHasNoEpic() {
        createThreeTaskListForTests(manager);

        SubTask noEpicIdSubTask = new SubTask(subTask1.getId(), "epicId ПОДЗАДАЧИ ссылается на id ЗАДАЧИ вместо ЭПИКА",
                "CHANGED", Status.DONE, task1.getId());

        NotFoundException ex1 = assertThrows(NotFoundException.class,
                () -> manager.updateSubTask(noEpicIdSubTask));
        assertTrue(ex1.getMessage().contains("EpicId"),
                "Сообщение об ошибке должно содержать слово 'EpicId'.");

        checkTaskCountForThreeTasks(manager);
        checkTasksUnchangedForThreeTaskList(manager, task1, epic1, subTask1);
    }

    @Test
    public void updateTaskMustThrowExceptionIfSubTaskNotInThisEpicList() {
        createThreeTaskListForTests(manager);
        Epic epic2 = new Epic("ЭПИК2", "описание");
        manager.createEpic(epic2);
        SubTask subTask2 = new SubTask("ПОДЗАДАЧА 2 ЭПИКА", "описание", 4);
        manager.createSubTask(subTask2);

        final List<Task> tasksToCheck = manager.getAllTasks();

        SubTask wrongEpicIdSubTask = new SubTask(3,
                "id ПОДЗАДАЧИ ссылается на ПЕРВЫЙ ЭПИК, epicId ПОДЗАДАЧИ ссылается на id ВТОРОГО ЭПИКА",
                "CHANGED", Status.DONE, 4);

        NotFoundException ex1 = assertThrows(NotFoundException.class,
                () -> manager.updateSubTask(wrongEpicIdSubTask));
        assertTrue(ex1.getMessage().contains("there is no SubTask with Id"),
                "Сообщение об ошибке должно содержать слово 'there is no SubTask with Id'.");

        checkTaskCountCustom(manager, 1, 2, 2, 5);
        checkTasksUnchangedCustom(tasksToCheck, manager.getAllTasks());
    }

    //ТЕСТЫ getHistory.
    @Test
    public void getHistoryMustUpdateTasksIfTaskManagerUpdatesThem() {
        createThreeTaskListForTests(manager);

        final List<Task> checkList = new ArrayList<>();
        checkList.add(manager.findAnyTaskById(task1.getId()).orElseThrow());
        checkList.add(manager.findAnyTaskById(epic1.getId()).orElseThrow());
        checkList.add(manager.findAnyTaskById(subTask1.getId()).orElseThrow());

        checkTasksUnchangedCustom(checkList, manager.getHistory());

        Task updateTask = new Task(task1.getId(), "ЗАДАЧА ИЗМЕНЕНА", "ЗАДАЧА ИЗМЕНЕНА", Status.DONE);
        Epic updateEpic = new Epic(epic1.getId(), "ЭПИК ИЗМЕНЕН", "ЭПИК ИЗМЕНЕН");
        SubTask updateSubTask = new SubTask(subTask1.getId(),
                "ПОДЗАДАЧА ИЗМЕНЕНА", "ПОДЗАДАЧА ИЗМЕНЕНА", Status.DONE, epic1.getId());

        manager.updateTask(updateTask);
        manager.updateEpic(updateEpic);
        manager.updateSubTask(updateSubTask);

        checkList.clear();

        checkList.add(manager.findAnyTaskById(updateTask.getId()).orElseThrow());
        checkList.add(manager.findAnyTaskById(updateEpic.getId()).orElseThrow());
        checkList.add(manager.findAnyTaskById(updateSubTask.getId()).orElseThrow());

        checkTasksUnchangedCustom(checkList, manager.getHistory());
    }

    @Test
    public void getHistoryMustDeleteTasksIfTaskManagerDeletesThem() {
        createSixTaskListForTests(manager);

        manager.findAnyTaskById(task1.getId()).orElseThrow();
        manager.findAnyTaskById(epic1.getId()).orElseThrow();
        manager.findAnyTaskById(epic2.getId()).orElseThrow();
        manager.findAnyTaskById(subTask1.getId()).orElseThrow();
        manager.findAnyTaskById(subTask2.getId()).orElseThrow();
        manager.findAnyTaskById(subTask3.getId()).orElseThrow();

        manager.deleteAnyTaskById(task1.getId());
        List<Task> history = manager.getHistory();
        assertFalse(history.contains(task1), "Задача должна быть удалена из истории.");

        manager.deleteAnyTaskById(subTask3.getId());
        history = manager.getHistory();
        assertFalse(history.contains(subTask3), "Подзадача должна быть удалена из HistoryManager.");
        assertTrue(history.contains(epic2), "Эпик подзадачи не должен быть удален из HistoryManager.");

        manager.deleteAnyTaskById(epic1.getId());
        history = manager.getHistory();
        assertFalse(history.contains(epic1), "Эпик должен быть удален из HistoryManager.");
        assertFalse(history.contains(subTask1), "Подзадача эпика должна быть удалена из HistoryManager.");
        assertFalse(history.contains(subTask2), "Подзадача эпика должна быть удалена из HistoryManager.");

        manager.deleteAnyTaskById(epic2.getId());
        history = manager.getHistory();
        assertTrue(history.isEmpty(), "Все задачи должны быть удалены из HistoryManager.");
    }

    //ТЕСТЫ resetEpicStatus.
    @Test
    public void updateEpicStatusWorksCorrectly() {
        Epic epic1 = new Epic("ЭПИК", "описание");
        manager.createEpic(epic1);


        Epic epicInManager = (Epic) manager.findAnyTaskById(1).orElseThrow();
        assertEquals(Status.NEW, epicInManager.getStatus(),
                "Статус нового эпика должен быть NEW.");

        SubTask subTask1 = new SubTask("ПОДЗАДАЧА1", "описание", 1);
        SubTask subTask2 = new SubTask("ПОДЗАДАЧА2", "описание", 1);

        manager.createSubTask(subTask1);
        manager.createSubTask(subTask2);
        epicInManager = (Epic) manager.findAnyTaskById(1).orElseThrow();
        assertEquals(Status.NEW, epicInManager.getStatus(),
                "Статус эпика после добавления новых подзадач должен быть NEW.");

        SubTask updateTask1 = new SubTask(2,
                "ПОДЗАДАЧА1 ВЫПОЛНЕНА", "описание", Status.DONE, 1);
        SubTask updateTask2 = new SubTask(3,
                "ПОДЗАДАЧА2 ОБНОВЛЕНА", "описание", Status.IN_PROGRESS, 1);

        manager.updateSubTask(updateTask1);
        epicInManager = (Epic) manager.findAnyTaskById(1).orElseThrow();
        assertEquals(Status.IN_PROGRESS, epicInManager.getStatus(),
                "Статус эпика после должен быть IN_PROGRESS пока все задачи не NEW и не DONE.");
        manager.updateSubTask(updateTask2);
        epicInManager = (Epic) manager.findAnyTaskById(1).orElseThrow();
        assertEquals(Status.IN_PROGRESS, epicInManager.getStatus(),
                "Статус эпика после должен быть IN_PROGRESS пока все задачи не NEW и не DONE.");

        SubTask updateSubTask2_2 = new SubTask(3,
                "ПОДЗАДАЧА2 ВЫПОЛНЕНА", "описание", Status.DONE, 1);

        manager.updateSubTask(updateSubTask2_2);
        epicInManager = (Epic) manager.findAnyTaskById(1).orElseThrow();
        assertEquals(Status.DONE, epicInManager.getStatus(),
                "Статус эпика должен быть DONE если все задачи DONE.");

        SubTask newSubTask = new SubTask("ПОДЗАДАЧА3", "описание", 1);

        manager.createSubTask(newSubTask);
        epicInManager = (Epic) manager.findAnyTaskById(1).orElseThrow();
        assertEquals(Status.IN_PROGRESS, epicInManager.getStatus(),
                "Статус эпика DONE должен смениться на IN_PROGRESS при добавлении NEW задачи.");

        manager.deleteAnyTaskById(4);
        epicInManager = (Epic) manager.findAnyTaskById(1).orElseThrow();
        assertEquals(Status.DONE, epicInManager.getStatus(),
                "Статус эпика IN_PROGRESS должен смениться на DONE при удалении NEW задачи.");

        manager.deleteAnyTaskById(2);
        manager.deleteAnyTaskById(3);
        epicInManager = (Epic) manager.findAnyTaskById(1).orElseThrow();
        assertEquals(Status.NEW, epicInManager.getStatus(),
                "Статус эпика DONE должен смениться на NEW при удалении всех задач.");
    }

    //ТЕСТЫ getPrioritizedTasks.

    @Test
    public void getPrioritizedTasksWorksCorrectly() {
        createSixTaskListForTimeTests(manager);

        List<Task> checkList = new ArrayList<>();
        checkList.add(subTask2);
        checkList.add(subTask3);
        checkList.add(subTask1);
        checkList.add(task1);

        List<Task> testList = new ArrayList<>(manager.getPrioritizedTasks());

        checkTasksUnchangedCustom(checkList, testList);

        task1 = new Task(1, "ОБНОВЛЕННАЯ ЗАДАЧА", "ОПИСАНИЕ", now.plusYears(1).minusMinutes(15),
                Duration.ofMinutes(15));
        subTask1 = new SubTask(4, "ПоДЗАДАЧА ОБНОВЛЕНА", "ОПИСАНИЕ", 2,
                now.plusMinutes(30), Duration.ofMinutes(120));

        manager.updateTask(task1);
        manager.updateSubTask(subTask1);
        checkList.removeLast();
        checkList.removeLast();
        checkList.add(subTask1);
        checkList.add(task1);

        testList = new ArrayList<>(manager.getPrioritizedTasks());

        checkTasksUnchangedCustom(checkList, testList);

        manager.deleteAnyTaskById(1);
        manager.deleteAnyTaskById(4);
        checkList.removeLast();
        checkList.removeLast();

        testList = new ArrayList<>(manager.getPrioritizedTasks());

        checkTasksUnchangedCustom(checkList, testList);

        manager.deleteAllTasks();

        assertTrue(manager.getPrioritizedTasks().isEmpty(),
                "Список приоритетных задач должен быть пуст после очистки менеджера.");
    }

    @Test
    public void setEpicTimeWorksCorrectly() {
        createSixTaskListForTimeTests(manager);

        assertEquals(now.plusMinutes(10), epic1.getStartTime(),
                "Начало эпика должно быть равно 00:10.");
        assertEquals(Duration.ofMinutes(19), epic1.getDuration(),
                "Длительность эпика должна быть равна 19 минутам.");
        assertEquals(now.plusMinutes(45), epic1.getEndTime(),
                "Конец эпика должен быть равен 00:45.");

        subTask1 = new SubTask(4, "ПоДЗАДАЧА ОБНОВЛЕНА", "ОПИСАНИЕ", 2,
                now.plusMinutes(30), Duration.ofMinutes(120));

        manager.deleteAnyTaskById(1);
        manager.updateSubTask(subTask1);

        assertEquals(now.plusMinutes(10), epic1.getStartTime(),
                "Начало эпика должно быть равно 00:10.");
        assertEquals(Duration.ofMinutes(124), epic1.getDuration(),
                "Длительность эпика должна быть равна 124 минутам.");
        assertEquals(now.plusMinutes(150), epic1.getEndTime(),
                "Конец эпика должен быть равен 02:30.");

        manager.deleteAnyTaskById(4);
        manager.deleteAnyTaskById(5);

        assertNull(epic1.getStartTime(),
                "У эпика без задач начало должно быть null.");
        assertNull(epic1.getDuration(),
                "У эпика без задач длительность должна быть null.");
        assertNull(epic1.getEndTime(),
                "У эпика без задач конец должен быть null.");
    }

    @Test
    public void calendarWorksCorrectly() {
        task1 = new Task("ЗАДАЧА", "ОПИСАНИЕ", now.plusMinutes(15), Duration.ofMinutes(15));
        Task wrongTask1 = new Task("1", "ВРЕМЯ НАЧАЛА В ЗАНЯТОМ ИНТЕРВАЛЕ", now.plusMinutes(16), Duration.ofMinutes(5));
        Task wrongTask2 = new Task("2", "ДЛИТЕЛЬНОСТЬ В ЗАНЯТОМ ИНТЕРВАЛЕ", now, Duration.ofMinutes(16));
        Task wrongTask3 = new Task("3", "НАЧАЛО НА ГРАНИЦЕ НОВОГО ИНТЕРВАЛА", now.plusMinutes(29), Duration.ofMinutes(15));
        Task wrongTask4 = new Task("4", "ЗА ПРЕДЕЛАМИ КАЛЕНДАРЯ", now.plusYears(1), Duration.ofMinutes(15));

        manager.createTask(task1);

        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> manager.createTask(wrongTask1));
        assertTrue(ex1.getMessage().contains("interval is occupied"),
                "Сообщение об ошибке должно содержать слово 'interval is occupied'.");
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> manager.createTask(wrongTask2));
        assertTrue(ex2.getMessage().contains("interval is occupied"),
                "Сообщение об ошибке должно содержать слово 'interval is occupied'.");
        IllegalArgumentException ex3 = assertThrows(IllegalArgumentException.class, () -> manager.createTask(wrongTask3));
        assertTrue(ex3.getMessage().contains("interval is occupied"),
                "Сообщение об ошибке должно содержать слово 'interval is occupied'.");
        IllegalArgumentException ex4 = assertThrows(IllegalArgumentException.class, () -> manager.createTask(wrongTask4));
        assertTrue(ex4.getMessage().contains("calendar range"),
                "Сообщение об ошибке должно содержать слово 'calendar range'.");
    }

    //ТЕСТЫ остальных методов.
    @Test
    public void getTasksWorksCorrectly() {
        createSixTaskListForTests(manager);

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
    public void findAnyTaskByIdMustReturnEmptyOptionalIfIdIsWrong() {
        Task task1 = new Task("Задача", "Описание");
        manager.createTask(task1);

        Optional<Task> wrongTask = manager.findAnyTaskById(-999);
        assertFalse(wrongTask.isPresent(),
                "Должно возвращаться Optional.empty() при неправильном id.");

        wrongTask = manager.findAnyTaskById(0);
        assertFalse(wrongTask.isPresent(),
                "Должно возвращаться Optional.empty() при неправильном id.");

        wrongTask = manager.findAnyTaskById(999);
        assertFalse(wrongTask.isPresent(),
                "Должно возвращаться Optional.empty() при неправильном id.");
    }

    @Test
    public void getEpicSubTasksMustReturnCorrectSubTasksOrThrowException() {
        createSixTaskListForTests(manager);

        final List<SubTask> testSubTaskList = new ArrayList<>();
        testSubTaskList.add(subTask1);
        testSubTaskList.add(subTask2);

        final List<SubTask> epicSubTasks = manager.getEpicSubTasks(epic1.getId());
        assertEquals(2, epicSubTasks.size(), "Эпик должен содержать 2 подзадачи.");
        assertEquals(testSubTaskList, epicSubTasks, "Выданные методом подзадачи не совпадают с ожидаемыми.");

        NotFoundException ex = assertThrows(NotFoundException.class, () -> manager.getEpicSubTasks(999));
        assertTrue(ex.getMessage().contains("not found"),
                "Сообщение об ошибке должно содержать слово 'not found'.");

        Epic emptyEpic = new Epic("Пустой эпик", "Описание");
        emptyEpic = manager.createEpic(emptyEpic);

        final List<SubTask> emptyEpicSubTasks = manager.getEpicSubTasks(emptyEpic.getId());
        assertNotNull(emptyEpicSubTasks,
                "При возвращении списка подзадач пустого эпика не должен возвращаться null.");
        assertTrue(emptyEpicSubTasks.isEmpty(),
                "При возвращении списка подзадач пустого эпика должен возвращаться пустой список.");
    }

    //ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ.
    protected void createSixTaskListForTests(TaskManager manager) {
        Task newTask = new Task("ЗАДАЧА", "Описание", now, Duration.ofMinutes(30));
        Epic newEpic1 = new Epic("ЭПИК1", "Описание");
        Epic newEpic2 = new Epic("ЭПИК2", "Описание");
        manager.createTask(newTask);
        manager.createEpic(newEpic1);
        manager.createEpic(newEpic2);
        SubTask newSubTask1 = new SubTask("ПОДЗАДАЧА1-1", "Описание", 2, now.plusHours(1), Duration.ofMinutes(30));
        SubTask newSubTask2 = new SubTask("ПОДЗАДАЧА1-2", "Описание", 2, now.plusHours(2), Duration.ofMinutes(30));
        SubTask newSubTask3 = new SubTask("ПОДЗАДАЧА2-1", "Описание", 3, now.plusHours(3), Duration.ofMinutes(30));
        manager.createSubTask(newSubTask1);
        manager.createSubTask(newSubTask2);
        manager.createSubTask(newSubTask3);

        task1 = manager.findAnyTaskById(1).orElseThrow();
        epic1 = (Epic) manager.findAnyTaskById(2).orElseThrow();
        epic2 = (Epic) manager.findAnyTaskById(3).orElseThrow();
        subTask1 = (SubTask) manager.findAnyTaskById(4).orElseThrow();
        subTask2 = (SubTask) manager.findAnyTaskById(5).orElseThrow();
        subTask3 = (SubTask) manager.findAnyTaskById(6).orElseThrow();

        checkTaskCountForSixTasks(manager);
    }

    protected void createSixTaskListForTimeTests(TaskManager manager) {
        task1 = new Task("ПОСЛЕДНЯЯ ЗАДАЧА", "Описание", now.plusMinutes(45), Duration.ofMinutes(60));
        epic1 = new Epic("ЭПИК1", "Описание");
        epic2 = new Epic("ЭПИК2", "Описание");
        subTask1 = new SubTask("ПОДЗАДАЧА1-1", "Описание", 2, now.plusMinutes(30), Duration.ofMinutes(15));
        subTask2 = new SubTask("ПОДЗАДАЧА1-1", "Описание", 2, now.plusMinutes(10), Duration.ofMinutes(4));
        subTask3 = new SubTask("ПОДЗАДАЧА1-1", "Описание", 3, now.plusMinutes(20), Duration.ofMinutes(9));

        task1 = manager.createTask(task1);
        epic1 = manager.createEpic(epic1);
        epic2 = manager.createEpic(epic2);
        subTask1 = manager.createSubTask(subTask1);
        subTask2 = manager.createSubTask(subTask2);
        subTask3 = manager.createSubTask(subTask3);

        checkTaskCountForSixTasks(manager);
    }

    protected void createThreeTaskListForTests(TaskManager manager) {
        task1 = new Task("ЗАДАЧА", "ОПИСАНИЕ ЗАДАЧИ", now, Duration.ofMinutes(30));
        epic1 = new Epic("ЭПИК", "ОПИСАНИЕ ЭПИКА");
        subTask1 = new SubTask("ПОДЗАДАЧА", "ОПИСАНИЕ ПОДЗАДАЧИ", 2, now.plusHours(1), Duration.ofMinutes(30));

        task1 = manager.createTask(task1);
        epic1 = manager.createEpic(epic1);
        subTask1 = manager.createSubTask(subTask1);

        checkTaskCountForThreeTasks(manager);
    }

    protected void checkTaskCountForSixTasks(TaskManager manager) {
        assertEquals(1, manager.getTasks().size(), "В списке задач должна быть 1 задача.");
        assertEquals(2, manager.getEpics().size(), "В списке эпиков должно быть 2 эпика.");
        assertEquals(3, manager.getSubTasks().size(), "В списке подзадач должно быть 3 подзадачи.");
        assertEquals(6, manager.getIdCounter(), "Счетчик менеджера должен быть равен 6.");
    }

    protected void checkTaskCountForThreeTasks(TaskManager manager) {
        assertEquals(1, manager.getTasks().size(), "В списке задач должна быть 1 задача.");
        assertEquals(1, manager.getEpics().size(), "В списке эпиков должен быть 1 эпик.");
        assertEquals(1, manager.getSubTasks().size(), "В списке подзадач должна быть 1 подзадача.");
        assertEquals(3, manager.getIdCounter(), "Счетчик менеджера должен быть равен 3.");
    }

    protected void checkTaskCountCustom(TaskManager manager, int tasks, int epics, int subTasks, int taskCount) {
        assertEquals(tasks, manager.getTasks().size(), "В списке задач должно быть " + tasks + " задач.");
        assertEquals(epics, manager.getEpics().size(), "В списке эпиков должно быть " + epics + " эпиков.");
        assertEquals(subTasks, manager.getSubTasks().size(), "В списке подзадач должно быть " + subTasks + " подзадач.");
        assertEquals(taskCount, manager.getIdCounter(), "Счетчик менеджера должен быть равен " + taskCount + ".");
    }

    protected void checkTaskCountForEmptyManager(TaskManager manager) {
        assertEquals(0, manager.getTasks().size(), "Список задач должен быть пуст.");
        assertEquals(0, manager.getEpics().size(), "Список эпиков должен быть пуст.");
        assertEquals(0, manager.getSubTasks().size(), "Список подзадач должен быть пуст.");
        assertEquals(0, manager.getIdCounter(), "Счетчик менеджера должен быть равен 0.");
    }

    protected void checkTasksUnchangedForThreeTaskList(TaskManager manager, Task initialTask, Epic initialEpic, SubTask initialSubTask) {
        Task taskToCheck = manager.findAnyTaskById(initialTask.getId()).orElseThrow();
        Epic epicToCheck = (Epic) manager.findAnyTaskById(initialEpic.getId()).orElseThrow();
        SubTask subTaskToCheck = (SubTask) manager.findAnyTaskById(initialSubTask.getId()).orElseThrow();

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

    protected void checkTasksUnchangedCustom(List<? extends Task> tasksToCheck, List<? extends Task> initialTasks) {
        assertEquals(tasksToCheck.size(), initialTasks.size(), "Размер списка на проверку должен " +
                "совпадать с размером списка менеджера.");

        for (int i = 0; i < tasksToCheck.size(); i++) {
            Task initialTask = initialTasks.get(i);
            Task taskToCheck = tasksToCheck.get(i);

            assertEquals(initialTask.getClass(), taskToCheck.getClass(),
                    "Классы задач не совпадают на позиции " + i);
            assertEquals(initialTask.getId(), taskToCheck.getId(),
                    "ID задач не совпадает на позиции " + i);
            assertEquals(initialTask.getName(), taskToCheck.getName(),
                    "Имя задачи не совпадает на позиции " + i);
            assertEquals(initialTask.getDescription(), taskToCheck.getDescription(),
                    "Описание не совпадает на позиции " + i);
            assertEquals(initialTask.getStatus(), taskToCheck.getStatus(),
                    "Статус не совпадает на позиции " + i);
            assertEquals(initialTask.getStartTime(), taskToCheck.getStartTime(),
                    "Начало задачи не совпадает на позиции " + i);
            assertEquals(initialTask.getDuration(), taskToCheck.getDuration(),
                    "Длительность задачи не совпадает на позиции " + i);
            assertEquals(initialTask.getEndTime(), taskToCheck.getEndTime(),
                    "Конец задачи не совпадает на позиции " + i);
            if (initialTask instanceof Epic initialEpic && taskToCheck instanceof Epic epicToCheck) {
                assertEquals(initialEpic.getSubTaskIdList(), epicToCheck.getSubTaskIdList(),
                        "Список subTaskIdList эпика не совпадает на позиции " + i);
            }
            if (initialTask instanceof SubTask initialSubTask && taskToCheck instanceof SubTask subTaskToCheck) {
                assertEquals(initialSubTask.getEpicId(), subTaskToCheck.getEpicId(),
                        "epicId подзадачи не совпадает на позиции " + i);
            }
        }
    }
}
