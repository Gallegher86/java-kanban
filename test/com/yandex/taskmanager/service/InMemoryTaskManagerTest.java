package com.yandex.taskmanager.service;

import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import com.yandex.taskmanager.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
        final Optional<Task> optionalSubTask= manager.getTaskById(subTask1.getId());

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

        final List<Task> tasks = manager.getAllTasks();
        final List<Epic> epics = manager.getAllEpics();
        final List<SubTask> subTasks = manager.getAllSubTasks();

        assertNotNull(tasks, "Задачи не возвращаются.");
        assertNotNull(epics, "Эпики не возвращаются.");
        assertNotNull(subTasks, "Подзадачи не возвращаются.");

        checkTaskCountForSixTasks();
    }

    @Test
    public void addTaskMustNotAddSameTaskTwice() {
        createThreeTaskListForTests();

        manager.addNewTask(task1);
        manager.addNewTask(epic1);
        manager.addNewTask(subTask1);

        checkTaskCountForThreeTasks();
    }

    @Test
    public void addTaskMustNotAddTaskWithNotNewStatus () {
        Task notNewStatusTask = new Task("Задача", "Описание");
        Epic notNewStatusEpic = new Epic("Эпик", "Описание");
        SubTask notNewStatusSubTask = new SubTask("Подзадача", "Описание", 1);
        notNewStatusTask.setStatus(Status.IN_PROGRESS);
        notNewStatusEpic.setStatus(Status.IN_PROGRESS);
        notNewStatusSubTask.setStatus(Status.IN_PROGRESS);

        manager.addNewTask(new Epic("Эпик", "Для подзадачи"));
        manager.addNewTask(notNewStatusTask);
        manager.addNewTask(notNewStatusEpic);
        manager.addNewTask(notNewStatusSubTask);

        assertEquals(1, manager.getAllTasks().size(), "В списке задач должна быть 1 задача.");
        assertEquals(1, manager.getAllEpics().size(), "В списке эпиков должен быть 1 эпик.");
        assertEquals(0, manager.getAllSubTasks().size(), "Список подзадач должен быть пуст.");

        assertFalse(manager.getAllTasks().contains(notNewStatusTask),
                "Задача со статусом не NEW не должна добавляться в список задач.");
        assertFalse(manager.getAllTasks().contains(notNewStatusEpic),
                "Эпик со статусом не NEW не должен добавляться в список задач.");
        assertFalse(manager.getAllTasks().contains(notNewStatusSubTask),
                "Подзадача со статусом не NEW не должна добавляться в список задач.");
        assertFalse(manager.getAllEpics().contains(notNewStatusEpic),
                "Эпик со статусом не NEW не должен добавляться в список эпиков.");
        assertTrue(manager.getAllSubTasks().isEmpty(),
                "Список подзадач должен быть пустым при добавлении подзадачи со статусом не NEW.");

        assertEquals(TaskManagerStatus.NOT_NEW_TASK, manager.getTaskManagerStatus(),
                "Статус менеджера должен измениться на NOT_NEW_TASK.");
        assertEquals(1, manager.getTaskCount(),
                "Счетчик менеджера не должен изменяться.");
    }

    @Test
    public void addTaskMustNotAddEpicWithNotEmptySubTaskIdList() {
        Epic notEmptyEpic = new Epic("Эпик", "Описание");
        List<Integer> subTaskIds = new ArrayList<>();
        subTaskIds.add(999);
        notEmptyEpic.setSubTaskIdList(subTaskIds);

        manager.addNewTask(notEmptyEpic);

        assertTrue(manager.getAllTasks().isEmpty(),
                "Список задач должен быть пустым при добавлении эпика с не пустым SubTaskIdList.");
        assertTrue(manager.getAllEpics().isEmpty(),
                "Список эпиков должен быть пустым при добавлении эпика с не пустым SubTaskIdList.");

        assertEquals(TaskManagerStatus.NOT_NEW_TASK, manager.getTaskManagerStatus(),
                "Статус менеджера должен измениться на NOT_NEW_TASK.");
        assertEquals(0, manager.getTaskCount(),
                "Счетчик менеджера не должен изменяться.");
    }

    @Test
    public void addTaskMustNotAddTaskWithExistingId() {
        Task wrongTask = new Task(999,"Задача", "Описание");
        Epic wrongEpic = new Epic(1000,"Эпик", "Описание");
        SubTask wrongSubTask = new SubTask(1001,"Подзадача", "Описание", Status.DONE, 1);

        manager.addNewTask(new Epic("Эпик", "Для подзадачи"));
        manager.addNewTask(wrongTask);
        manager.addNewTask(wrongEpic);
        manager.addNewTask(wrongSubTask);

        assertEquals(1, manager.getAllTasks().size(), "В списке задач должна быть 1 задача.");
        assertEquals(1, manager.getAllEpics().size(), "В списке эпиков должен быть 1 эпик.");
        assertEquals(0, manager.getAllSubTasks().size(), "Список подзадач должен быть пуст.");

        assertFalse(manager.getAllTasks().contains(wrongTask),
                "Задача с уже заданным Id не должна добавляться в список задач.");
        assertFalse(manager.getAllTasks().contains(wrongEpic),
                "Эпик с уже заданным Id не должен добавляться в список задач.");
        assertFalse(manager.getAllTasks().contains(wrongSubTask),
                "Подзадача с уже заданным Id не должна добавляться в список задач.");
        assertFalse(manager.getAllEpics().contains(wrongEpic),
                "Эпик с уже заданным Id не должен добавляться в список эпиков.");

        assertEquals(TaskManagerStatus.NOT_NEW_TASK, manager.getTaskManagerStatus(),
                "Статус менеджера должен измениться на NOT_NEW_TASK.");
        assertEquals(1, manager.getTaskCount(),
                "Счетчик менеджера не должен изменяться.");
    }

    @Test
    public void addTaskMustNotAddNullTask() {
        manager.addNewTask(null);

        final List<Task> allTasks = manager.getAllTasks();
        final List<Epic> allEpics = manager.getAllEpics();
        final List<SubTask> allSubTasks = manager.getAllSubTasks();

        assertNotNull(allTasks, "Список задач не должен быть null.");
        assertNotNull(allEpics, "Список эпиков не должен быть null.");
        assertNotNull(allSubTasks, "Список подзадач не должен быть null.");

        assertTrue(allTasks.isEmpty(), "Список задач должен быть пустым при добавлении null-задачи.");
        assertTrue(allEpics.isEmpty(), "Список эпиков должен быть пустым при добавлении null-задачи.");
        assertTrue(allSubTasks.isEmpty(), "Список подзадач должен быть пустым при добавлении null-задачи.");

        assertEquals(TaskManagerStatus.NULL, manager.getTaskManagerStatus(),
                "Статус менеджера должен измениться на NULL.");
        assertEquals(0, manager.getTaskCount(),
                "Счетчик менеджера не должен изменяться.");
    }

    @Test
    public void addTaskMustNotAddSubTaskWithInvalidEpicId() {
        SubTask invalidIdSubTask = new SubTask ("Подзадача", "Описание", 999);
        manager.addNewTask(invalidIdSubTask);

        assertTrue(manager.getAllTasks().isEmpty(),
                "Список задач должен быть пустым при добавлении подзадачи с несуществующим epicId.");
        assertTrue(manager.getAllSubTasks().isEmpty(),
                "Список подзадач должен быть пустым при добавлении подзадачи с несуществующим epicId.");

        assertEquals(TaskManagerStatus.WRONG_EPIC_ID, manager.getTaskManagerStatus(),
                "Статус менеджера должен измениться на WRONG_EPIC_ID.");
        assertEquals(0, manager.getTaskCount(),
                "Счетчик менеджера не должен изменяться.");
    }

    @Test
    public void addTaskMustNotAddSubTaskAsItsOwnEpic() {
        Epic epic1 = new Epic("Эпик", "Описание");
        manager.addNewTask(epic1);
        SubTask subTask1 = new SubTask ("Подзадача1", "Описание", epic1.getId());
        manager.addNewTask(subTask1);

        SubTask wrongSubTask = new SubTask("Подзадача2", "Описание", subTask1.getId());
        manager.addNewTask(wrongSubTask);

        assertFalse(manager.getAllTasks().contains(wrongSubTask),
                "Подзадача с epicId, равным id подзадачи не должна добавляться в список задач.");
        assertFalse(manager.getAllSubTasks().contains(wrongSubTask),
                "Подзадача с epicId, равным id подзадачи не должна добавляться в список подзадач.");

        assertEquals(TaskManagerStatus.WRONG_EPIC_ID, manager.getTaskManagerStatus(),
                "Статус менеджера должен измениться на WRONG_EPIC_ID.");
        assertEquals(2, manager.getTaskCount(),
               "Счетчик менеджера не должен изменяться.");
    }

    //ТЕСТЫ deleteById и deleteAll.
    @Test
    public void deleteEpicByIdMustRemoveItsSubTasks() {
        createSixTaskListForTests();

        manager.deleteTaskById(epic1.getId());

        assertEquals(3, manager.getAllTasks().size(), "В списке задач должно быть 3 задачи.");
        assertEquals(1, manager.getAllEpics().size(), "В списке эпиков должен быть 1 эпик.");
        assertEquals(1, manager.getAllSubTasks().size(), "В списке подзадач должна быть 1 подзадача.");

        assertFalse(manager.getAllTasks().contains(epic1),
                "Эпик должен быть удален из списка задач.");
        assertFalse(manager.getAllTasks().contains(subTask1),
                "Подзадачи должны быть удалены из списка задач.");
        assertFalse(manager.getAllTasks().contains(subTask2),
                "Подзадачи должны быть удалены из списка задач.");

        assertFalse(manager.getAllEpics().contains(epic1),
                "Эпик должен быть удален из списка эпиков.");
        assertFalse(manager.getAllSubTasks().contains(subTask1),
                "Подзадачи должны быть удалены из списка подзадач.");
        assertFalse(manager.getAllSubTasks().contains(subTask2),
                "Подзадачи должны быть удалены из списка подзадач.");
    }

    @Test
    public void deleteSubTaskByIdMustRemoveItFromEpic() {
        createSixTaskListForTests();

        assertTrue(epic1.getSubTaskIdList().contains(subTask1.getId()),
                "id подзадачи должна быть в списке эпика.");

        manager.deleteTaskById(subTask1.getId());

        assertEquals(5, manager.getAllTasks().size(), "В списке задач должно быть 5 задач.");
        assertEquals(2, manager.getAllEpics().size(), "В списке эпиков должно быть 2 эпика.");
        assertEquals(2, manager.getAllSubTasks().size(), "В списке подзадач должно быть 2 подзадачи.");

        assertFalse(manager.getAllTasks().contains(subTask1),
                "Подзадача должна быть удален из списка задач.");
        assertFalse(manager.getAllSubTasks().contains(subTask1),
                "Подзадача должны быть удалены из списка подзадач.");
        assertFalse(epic1.getSubTaskIdList().contains(subTask1.getId()),
                "id подзадачи должно быть удалено из списке эпика.");
    }

    @Test
    public void deleteTaskByIdMustRemoveItFromTaskList() {
        createSixTaskListForTests();

        manager.deleteTaskById(task1.getId());

        assertEquals(5, manager.getAllTasks().size(), "В списке задач должно быть 5 задач.");
        assertEquals(2, manager.getAllEpics().size(), "В списке эпиков должно быть 2 эпика.");
        assertEquals(3, manager.getAllSubTasks().size(), "В списке подзадач должно быть 3 подзадачи.");
        assertFalse(manager.getAllTasks().contains(task1), "Задача должна быть удалена из списка задач.");
    }

    @Test
    public void deleteTaskByIdMustNotAcceptInvalidId() {
        createSixTaskListForTests();

        manager.deleteTaskById(-999);
        checkTaskCountForSixTasks();

        manager.deleteTaskById(0);
        checkTaskCountForSixTasks();

        manager.deleteTaskById(999);
        checkTaskCountForSixTasks();

        assertEquals(TaskManagerStatus.WRONG_ID, manager.getTaskManagerStatus(),
                "Статус менеджера должен измениться на WRONG_ID.");
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
        assertEquals(6, manager.getTaskCount(),
                "Счетчик менеджера должен быть равен 6.");

        manager.deleteAllTasks();

        assertTrue(manager.getAllTasks().isEmpty(),
                "Список задач должен быть пустым после удаления всех задач.");
        assertTrue(manager.getAllEpics().isEmpty(),
                "Список эпиков должен быть пустым после удаления всех задач.");
        assertTrue(manager.getAllSubTasks().isEmpty(),
                "Список подзадач должен быть пустым после удаления всех задач.");
        assertTrue(manager.getHistory().isEmpty(),
                "Список истории должен быть пустым после удаления всех задач.");
        assertEquals(0, manager.getTaskCount(),
                "Счетчик менеджера должен обнуляться после удаления всех задач.");
    }

    //ТЕСТЫ updateTask.
    @Test
    public void updateTaskWorksProperly() {
        createSixTaskListForTests();

        Task updateTask = new Task(task1.getId(), "ЗАДАЧА ОБНОВЛЕНА", "ЗАДАЧА ВЫПОЛНЕНА", Status.DONE);
        Epic updateEpic = new Epic(epic2.getId(),"ЭПИК ОБНОВЛЕН", "ЭПИК ВЫПОЛНЕН");
        SubTask updateSubTask = new SubTask(subTask3.getId(), "ПОДЗАДАЧА ОБНОВЛЕНА", "ПОДЗАДАЧА ВЫПОЛНЕНА",
                Status.DONE, epic2.getId());

        manager.updateTask(updateTask);
        manager.updateTask(updateEpic);
        manager.updateTask(updateSubTask);

        checkTaskCountForSixTasks();

        final Optional <Task> optionalTask = manager.getTaskById(task1.getId());
        final Optional <Task> optionalEpic = manager.getTaskById(epic2.getId());
        final Optional <Task> optionalSubTask = manager.getTaskById(subTask3.getId());

        assertTrue(optionalTask.isPresent(), "Задача не находиться по id.");
        assertTrue(optionalEpic.isPresent(), "Эпик не находиться по id.");
        assertTrue(optionalSubTask.isPresent(), "Подзадача не находиться по id.");

        checkTasksUnchangedForThreeTaskList(updateTask, updateEpic, updateSubTask);
    }

    @Test
    public void updateTaskMustNotAcceptNullTask() {
        createThreeTaskListForTests();

        manager.updateTask(null);

        assertEquals(3, manager.getAllTasks().size(), "В списке задач должно быть 3 задачи.");
        assertEquals(1, manager.getAllEpics().size(), "В списке эпиков должен быть 1 эпик.");
        assertEquals(1, manager.getAllSubTasks().size(), "В списке подзадач должно быть 1 подзадача.");
        assertEquals(3, manager.getTaskCount(), "Счетчик менеджера должен быть равен 3.");

        checkTaskCountForThreeTasks();
        checkTasksUnchangedForThreeTaskList(task1, epic1, subTask1);

        assertEquals(TaskManagerStatus.NULL, manager.getTaskManagerStatus(),
                "Статус менеджера должен измениться на NULL.");
    }

    @Test
    public void updateTaskMustNotAcceptTaskWithWrongClass() {
        createThreeTaskListForTests();

        Epic wrongClassTask = new Epic(task1.getId(), "Эпик вместо задачи", "Описание");
        Task wrongClassEpic = new Task(epic1.getId(), "Задача вместо эпика", "Описание");
        Task wrongClassSubTask = new Task(subTask1.getId(), "Задача вместо подзадачи", "Описание");

        manager.updateTask(wrongClassTask);
        manager.updateTask(wrongClassEpic);
        manager.updateTask(wrongClassSubTask);

        checkTaskCountForThreeTasks();
        checkTasksUnchangedForThreeTaskList(task1, epic1, subTask1);

        assertEquals(TaskManagerStatus.WRONG_CLASS, manager.getTaskManagerStatus(),
                "Статус менеджера должен измениться на WRONG_CLASS.");
    }

    @Test
    public void updateTaskMustNotAcceptTaskWithInvalidId() {
        createThreeTaskListForTests();

        Task invalidIdTask = new Task(999,"ЗАДАЧА с несуществующим id", "описание", Status.DONE);
        Epic invalidIdEpic = new Epic(1000, "ЭПИК с несуществующим id", "описание");
        SubTask invalidIdSubTask = new SubTask(1001, "ПОДЗАДАЧА с несуществующим id",
                "описание", Status.DONE, epic1.getId());

        manager.updateTask(invalidIdTask);
        manager.updateTask(invalidIdEpic);
        manager.updateTask(invalidIdSubTask);

        checkTaskCountForThreeTasks();
        checkTasksUnchangedForThreeTaskList(task1, epic1, subTask1);

        assertEquals(TaskManagerStatus.WRONG_ID, manager.getTaskManagerStatus(),
                "Статус менеджера должен измениться на WRONG_ID.");
    }

    @Test
    public void updateTaskMustCorrectlyTransferEpicSubTaskIdList() {
        Epic initialEpic = new Epic("ЭПИК", "описание");
        manager.addNewTask(initialEpic);
        SubTask subTask1 = new SubTask("ПОДЗАДАЧА", "описание", initialEpic.getId());
        SubTask subTask2 = new SubTask("ПОДЗАДАЧА", "описание", initialEpic.getId());
        SubTask subTask3 = new SubTask("ПОДЗАДАЧА", "описание", initialEpic.getId());
        manager.addNewTask(subTask1);
        manager.addNewTask(subTask2);
        manager.addNewTask(subTask3);

        Epic updateEpic = new Epic(1,"НОВЫЙ ЭПИК со своим subTaskIdList", "CHANGED");
        List<Integer> updateEpicSubTaskIdList = new ArrayList<>();
        updateEpicSubTaskIdList.add(999);
        updateEpicSubTaskIdList.add(1000);
        updateEpicSubTaskIdList.add(1001);
        updateEpic.setSubTaskIdList(updateEpicSubTaskIdList);

        manager.updateTask(updateEpic);

        final Epic epicToCheck = (Epic) manager.getTaskById(initialEpic.getId()).orElseThrow();
        assertEquals(epicToCheck.getSubTaskIdList(), initialEpic.getSubTaskIdList(),
                "Update Epic должен скопировать в новый эпик subTaskIdList прошлого экземпляра эпика.");
        assertEquals(epicToCheck.getName(), updateEpic.getName(),
                "Имя эпика должно измениться.");
        assertEquals(epicToCheck.getDescription(), updateEpic.getDescription(),
                "Описание эпика должно измениться.");
    }

    @Test
    public void updateTaskMustRejectSubTaskWithoutEpic() {
        createThreeTaskListForTests();

        SubTask wrongEpicIdSubTask = new SubTask(subTask1.getId(), "epicId ПОДЗАДАЧИ ссылается на id ЗАДАЧИ вместо ЭПИКА",
                "CHANGED", Status.DONE, task1.getId());
        manager.updateTask(wrongEpicIdSubTask);

        checkTaskCountForThreeTasks();
        checkTasksUnchangedForThreeTaskList(task1, epic1, subTask1);

        assertEquals(TaskManagerStatus.WRONG_EPIC_ID, manager.getTaskManagerStatus(),
                "Статус менеджера должен измениться на WRONG_EPIC_ID.");
    }

    @Test
    public void updateTaskMustRejectSubTaskNotInEpicList() {
        createThreeTaskListForTests();
        Epic epic2 = new Epic("ЭПИК2", "описание");
        manager.addNewTask(epic2);
        SubTask subTask2 = new SubTask("ПОДЗАДАЧА 2 ЭПИКА","описание", epic2.getId());
        manager.addNewTask(subTask2);

        SubTask wrongEpicListSubTask = new SubTask(subTask1.getId(),
                "id ПОДЗАДАЧИ ссылается на ПЕРВЫЙ ЭПИК, epicId ПОДЗАДАЧИ ссылается на id ВТОРОГО ЭПИКА",
                "CHANGED", Status.DONE, epic2.getId());

        manager.updateTask(wrongEpicListSubTask);

        checkTasksUnchangedForThreeTaskList(task1, epic1, subTask1);

        assertEquals(TaskManagerStatus.WRONG_EPIC_ID, manager.getTaskManagerStatus(),
                "Статус менеджера должен измениться на WRONG_EPIC_ID.");
    }

    //ТЕСТЫ getHistory.
    @Test
    public void getHistoryShouldKeepOldCopyOfTasks() {
        createThreeTaskListForTests();

        final List<Task> checkList = new ArrayList<>();
        checkList.add(manager.getTaskById(task1.getId()).orElseThrow());
        checkList.add(manager.getTaskById(epic1.getId()).orElseThrow());
        checkList.add(manager.getTaskById(subTask1.getId()).orElseThrow());

        Task updateTask = new Task(task1.getId(), "ЗАДАЧА ИЗМЕНЕНА", "ЗАДАЧА ИЗМЕНЕНА", Status.DONE);
        Epic updateEpic = new Epic(epic1.getId(), "ЭПИК ИЗМЕНЕН", "ЭПИК ИЗМЕНЕН");
        SubTask updateSubTask = new SubTask(subTask1.getId(),
                "ПОДЗАДАЧА ИЗМЕНЕНА", "ПОДЗАДАЧА ИЗМЕНЕНА", Status.DONE, epic1.getId());

        manager.updateTask(updateTask);
        manager.updateTask(updateEpic);
        manager.updateTask(updateSubTask);

        checkList.add(manager.getTaskById(updateTask.getId()).orElseThrow());
        checkList.add(manager.getTaskById(updateEpic.getId()).orElseThrow());
        checkList.add(manager.getTaskById(updateSubTask.getId()).orElseThrow());

        final List<Task> historyList = manager.getHistory();

        for (int i = 0; i < historyList.size(); i++) {
            Task historyListTask = historyList.get(i);
            Task checkListTask = checkList.get(i);

            assertEquals(historyListTask.getClass(), checkListTask.getClass(),
                    "Классы задач не совпадают на позиции " + i);
            assertEquals(historyListTask.getId(), checkListTask.getId(),
                    "ID задач не совпадает на позиции " + i);
            assertEquals(historyListTask.getName(), checkListTask.getName(),
                    "Имя задачи не совпадает на позиции " + i);
            assertEquals(historyListTask.getDescription(), checkListTask.getDescription(),
                    "Описание не совпадает на позиции " + i);
            assertEquals(historyListTask.getStatus(), checkListTask.getStatus(),
                    "Статус не совпадает на позиции " + i);
        }
    }
    //ТЕСТЫ resetEpicStatus.
    @Test
    public void resetEpicStatusWorksProperly() {
        Epic epic1 = new Epic("ЭПИК", "описание");
        manager.addNewTask(epic1);

        final Epic savedEpic = (Epic) manager.getTaskById(epic1.getId()).orElseThrow();
        assertEquals(Status.NEW, savedEpic.getStatus(),
                "Статус нового эпика должен быть NEW.");

        SubTask subTask1 = new SubTask("ПОДЗАДАЧА1", "описание", epic1.getId());
        SubTask subTask2 = new SubTask("ПОДЗАДАЧА2", "описание", epic1.getId());

        manager.addNewTask(subTask1);
        manager.addNewTask(subTask2);
        assertEquals(Status.NEW, savedEpic.getStatus(),
                "Статус эпика после добавления новых подзадач должен быть NEW.");

        SubTask updateTask1 = new SubTask(subTask1.getId(),
                "ПОДЗАДАЧА1 ВЫПОЛНЕНА", "описание", Status.DONE, epic1.getId());
        SubTask updateTask2 = new SubTask(subTask2.getId(),
                "ПОДЗАДАЧА2 ОБНОВЛЕНА", "описание", Status.IN_PROGRESS, epic1.getId());

        manager.updateTask(updateTask1);
        assertEquals(Status.IN_PROGRESS, savedEpic.getStatus(),
                "Статус эпика после должен быть IN_PROGRESS пока все задачи не NEW и не DONE.");
        manager.updateTask(updateTask2);
        assertEquals(Status.IN_PROGRESS, savedEpic.getStatus(),
                "Статус эпика после должен быть IN_PROGRESS пока все задачи не NEW и не DONE.");

        SubTask updateTask2_2 = new SubTask(subTask2.getId(),
                "ПОДЗАДАЧА2 ВЫПОЛНЕНА", "описание", Status.DONE, epic1.getId());

        manager.updateTask(updateTask2_2);
        assertEquals(Status.DONE, savedEpic.getStatus(),
                "Статус эпика должен быть DONE если все задачи DONE.");

        SubTask newSubTask = new SubTask("ПОДЗАДАЧА3", "описание", epic1.getId());

        manager.addNewTask(newSubTask);
        assertEquals(Status.IN_PROGRESS, savedEpic.getStatus(),
                "Статус эпика DONE должен смениться на IN_PROGRESS при добавлении NEW задачи.");

        manager.deleteTaskById(newSubTask.getId());
        assertEquals(Status.DONE, savedEpic.getStatus(),
                "Статус эпика IN_PROGRESS должен смениться на DONE при удалении NEW задачи.");

        manager.deleteTaskById(subTask1.getId());
        manager.deleteTaskById(subTask2.getId());
        assertEquals(Status.NEW, savedEpic.getStatus(),
                "Статус эпика DONE должен смениться на NEW при удалении всех задач.");
    }
    //ТЕСТЫ остальных методов.
    @Test
    public void getTaskByIdMustReturnEmptyOptionalIfIdIsWrong() {
        Task task1 = new Task("Задача", "Описание");
        manager.addNewTask(task1);

        Optional<Task> wrongTask = manager.getTaskById(999);

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
        manager.addNewTask(emptyEpic);

        final List<SubTask> emptyEpicSubTasks = manager.getEpicSubTasks(emptyEpic.getId());
        assertNotNull(emptyEpicSubTasks,
                "При возвращении списка подзадач пустого эпика не должен возвращаться null.");
        assertTrue(emptyEpicSubTasks.isEmpty(),
                "При возвращении списка подзадач пустого эпика должен возвращаться пустой список.");
    }

    //ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ.
    private void createSixTaskListForTests() {
        task1 = new Task ("ЗАДАЧА", "Описание");
        epic1 = new Epic("ЭПИК1", "Описание");
        epic2 = new Epic("ЭПИК2", "Описание");
        manager.addNewTask(task1);
        manager.addNewTask(epic1);
        manager.addNewTask(epic2);
        subTask1 = new SubTask("ПОДЗАДАЧА1-1", "Описание", epic1.getId());
        subTask2 = new SubTask("ПОДЗАДАЧА1-2", "Описание", epic1.getId());
        subTask3 = new SubTask("ПОДЗАДАЧА2-1", "Описание", epic2.getId());
        manager.addNewTask(subTask1);
        manager.addNewTask(subTask2);
        manager.addNewTask(subTask3);

        checkTaskCountForSixTasks();
    }

    private void createThreeTaskListForTests() {
        task1 = new Task("ЗАДАЧА", "ОПИСАНИЕ ЗАДАЧИ");
        epic1 = new Epic("ЭПИК", "ОПИСАНИЕ ЭПИКА");
        manager.addNewTask(task1);
        manager.addNewTask(epic1);
        subTask1 = new SubTask("ПОДЗАДАЧА", "ОПИСАНИЕ ПОДЗАДАЧИ", epic1.getId());
        manager.addNewTask(subTask1);

        checkTaskCountForThreeTasks();
    }

    private void checkTaskCountForSixTasks() {
        assertEquals(6, manager.getAllTasks().size(), "В списке задач должно быть 6 задач.");
        assertEquals(2, manager.getAllEpics().size(), "В списке эпиков должно быть 2 эпика.");
        assertEquals(3, manager.getAllSubTasks().size(), "В списке подзадач должно быть 3 подзадачи.");
        assertEquals(6, manager.getTaskCount(), "Счетчик менеджера должен быть равен 6.");
    }

    private void checkTaskCountForThreeTasks() {
        assertEquals(3, manager.getAllTasks().size(), "В списке задач должно быть 3 задачи.");
        assertEquals(1, manager.getAllEpics().size(), "В списке эпиков должен быть 1 эпик.");
        assertEquals(1, manager.getAllSubTasks().size(), "В списке подзадач должна быть 1 подзадача.");
        assertEquals(3, manager.getTaskCount(), "Счетчик менеджера должен быть равен 3.");
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
}