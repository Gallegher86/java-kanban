package yandex.practicum.kanban.tasks;
import java.util.ArrayList;
import java.util.HashMap;

public class TaskManager {
    static int taskCount = 0;
    HashMap<Integer, Task> taskList = new HashMap<>();
    HashMap<Integer, Epic> epicList = new HashMap<>();
    HashMap<Integer, SubTask> subTaskList = new HashMap<>();

    public void addNewTask(Task task) {
        if (task != null && task.getId() == 0) {
            if (task instanceof SubTask subTask) {
                Epic epic = epicList.get(subTask.getEpicId());

                if (epic != null) {
                    addToTaskList(task);
                    subTaskList.put(subTask.getId(), subTask);
                    epic.addSubTaskId(subTask.getId());
                    resetEpicStatus(subTask.getEpicId());
                } else {
                    System.out.println("В метод addNewTask передана подзадача с некорректным epicId: " +
                            subTask.getEpicId() + ".");
                }
            } else if (task instanceof Epic epic) {
                addToTaskList(task);
                epicList.put(epic.getId(), epic);
            } else {
                addToTaskList(task);
            }
        } else if (task != null) {
            System.out.println("В метод addNewTask передан объект с уже заданным id: " + task.getId() + ", применен " +
                    "неверный конструктор.");
        } else {
            System.out.println("В метод addNewTask передан некорректный объект.");
        }
    }

    private void addToTaskList(Task task) {
        taskCount++;
        task.setId(taskCount);
        taskList.put(task.getId(), task);
        System.out.println("Задача добавлена, присвоен id: " + task.getId());
    }

    public ArrayList<Task> getAllTasks() {
        return new ArrayList<>(taskList.values());
    }

    public ArrayList<Epic> getAllEpics() {
        return new ArrayList<>(epicList.values());
    }

    public ArrayList<SubTask> getAllSubTasks() {
       return new ArrayList<>(subTaskList.values());
    }

    public void deleteAllTasks() {
        taskList.clear();
        epicList.clear();
        subTaskList.clear();
        taskCount = 0;
        System.out.println("Все задачи удалены.");
    }

    public Task getTaskById(int id) {
        if (taskList.containsKey(id)) {
            return taskList.get(id);
        } else {
            System.out.println("Введенный id: " + id + " не найден в списке задач. Возвращена пустая задача.");
            return new Task("", "");
        }
    }

    public ArrayList<SubTask> getEpicSubTasks(int id) {
        if (epicList.containsKey(id)) {
            Epic epic = epicList.get(id);
            ArrayList<Integer> subTaskIds = epic.getSubTaskIdList();
            ArrayList<SubTask> subTasks = new ArrayList<>();

            for (int subTaskId : subTaskIds) {
                SubTask subTask = subTaskList.get(subTaskId);
                subTasks.add(subTask);
            }
            return subTasks;
        } else {
            System.out.println("Введенный id: " + id + " не найден в списке эпиков. Возвращен пустой список.");
            return new ArrayList<>();
        }
    }

    public void deleteTaskById(int id) {
        if (epicList.containsKey(id)) {
            Epic epic = epicList.get(id);

            for (int subTaskId : epic.getSubTaskIdList()) {
                subTaskList.remove(subTaskId);
                taskList.remove(subTaskId);
            }
            epicList.remove(id);
            taskList.remove(id);
            System.out.println("Задача по id: " + id + " удалена");

        } else if (subTaskList.containsKey(id)) {
            SubTask subTask = subTaskList.get(id);
            int epicId = subTask.getEpicId();

            Epic epic = epicList.get(epicId);
            epic.getSubTaskIdList().remove((Integer) id);
            resetEpicStatus(epicId);
            subTaskList.remove(id);
            taskList.remove(id);
            System.out.println("Задача по id: " + id + " удалена");

        } else if (taskList.containsKey(id)) {
            taskList.remove(id);
            System.out.println("Задача по id: " + id + " удалена");

        } else {
            System.out.println("Введенный id: " + id + " не найден в списках менеджера.");
        }
    }

    private void resetEpicStatus(int id) {
        Epic epic = epicList.get(id);
        ArrayList<Integer> SubTaskIds = epic.getSubTaskIdList();

        if (SubTaskIds.isEmpty()) {
            epic.setStatus(Status.NEW);
        } else {
            boolean isNew = true;
            boolean isDone = true;

            for (int subTaskId : SubTaskIds) {
                SubTask subTask = subTaskList.get(subTaskId);
                Status status = subTask.getStatus();

                if (status != Status.NEW) {
                    isNew = false;
                }
                if (status != Status.DONE) {
                    isDone = false;
                }
            }

            if (isNew) {
                epic.setStatus(Status.NEW);
            } else if (isDone) {
                epic.setStatus(Status.DONE);
            } else {
                epic.setStatus(Status.IN_PROGRESS);
            }
        }
        System.out.println("У эпика id " + epic.getId() + " " + epic.getName() + " статус обновлен до " + epic.getStatus());
    }

    public void updateTask(Task newTask) {
        Task existingTask = taskList.get(newTask.getId());

        if (existingTask != null && existingTask.getClass() == newTask.getClass()) {
            if (newTask instanceof SubTask newSubTask) {
                updateSubTask(newSubTask);
            } else if (newTask instanceof Epic newEpic) {
                updateEpic(newEpic);
            } else {
                updateGenericTask(newTask);
            }
        } else {
            System.out.println("В метод updateTask передан некорректный объект.");
            if (existingTask != null) {
                System.out.println("Класс переданной задачи: " + newTask.getClass() + " не совпадает с классом существующей " +
                        "задачи: " + existingTask.getClass() + ".");
            } else {
                System.out.println("Задачи с id: " + newTask.getId() + " нет в списке.");
            }
        }
    }

    private void updateEpic(Epic newEpic) {
        Epic oldEpic = epicList.get(newEpic.getId());

        if (oldEpic != null) {
            ArrayList<Integer> newIdList = newEpic.getSubTaskIdList();
            ArrayList<Integer> oldIdList = oldEpic.getSubTaskIdList();
            newIdList.addAll(oldIdList);

            epicList.put(newEpic.getId(), newEpic);
            resetEpicStatus(newEpic.getId());
            taskList.put(newEpic.getId(), newEpic);
            System.out.println("Эпик с id: " + newEpic.getId() + " обновлен.");
        } else {
            System.out.println("Эпик с id: " + newEpic.getId() + " не найден.");
        }
    }

    private void updateSubTask(SubTask newSubTask) {
        Epic epic = epicList.get(newSubTask.getEpicId());

        if (epic == null) {
            System.out.println("Эпик с id: " + newSubTask.getEpicId() + " не найден.");
            return;
        }
        ArrayList<Integer> checkIds = epic.getSubTaskIdList();

        if (checkIds.contains(newSubTask.getId())) {
            subTaskList.put(newSubTask.getId(), newSubTask);
            taskList.put(newSubTask.getId(), newSubTask);
            System.out.println("Подзадача с id: " + newSubTask.getId() + " обновлена.");
            resetEpicStatus(newSubTask.getEpicId());
        } else {
            System.out.println("Задачи с id: " + newSubTask.getId() + " нет в эпике с epicId: " + epic.getId() + ".");
        }
    }

    private void updateGenericTask(Task newTask) {
        taskList.put(newTask.getId(), newTask);
        System.out.println("Задача с id: " + newTask.getId() + " обновлена.");
    }
}
