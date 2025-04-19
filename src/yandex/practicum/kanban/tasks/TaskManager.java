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
            taskCount++;
            task.setId(taskCount);
            taskList.put(task.getId(), task);

            if (task instanceof SubTask) {
                SubTask subTask = (SubTask) task;
                Epic epic = epicList.get(subTask.getEpicId());

                if (epic != null) {
                    subTaskList.put(subTask.getId(), subTask);
                    epic.addSubTaskId(subTask.getId());
                    resetEpicStatus(subTask.getEpicId());
                } else {
                    System.out.println("В метод addNewTask передана подзадача с некорректным epicId: " +
                            subTask.getEpicId() + ".");
                }
            } else if (task instanceof Epic) {
                Epic epic = (Epic) task;

                epicList.put(epic.getId(), epic);
            }
        } else if (task != null) {
            System.out.println("В метод addNewTask передан объект с уже заданным id: " + task.getId() + ", применен " +
                    "неверный конструктор.");
        } else {
            System.out.println("В метод addNewTask передан некорректный объект.");
        }
    }

    public ArrayList<Task> getAllTasks() {
        ArrayList<Task> allTasks = new ArrayList<>(taskList.values());

        return allTasks;
    }

    public ArrayList<Epic> getAllEpics() {
        ArrayList<Epic> allEpics = new ArrayList<>(epicList.values());

        return allEpics;
    }

    public ArrayList<SubTask> getAllSubTasks() {
        ArrayList<SubTask> allSubTasks = new ArrayList<>(subTaskList.values());

        return allSubTasks;
    }

    public void deleteAllTasks() {
        taskList.clear();
        epicList.clear();
        subTaskList.clear();
        int finalCount = taskCount;
        taskCount = 0;
        System.out.println("Все задачи удалены. Всего удалено " + finalCount + " задач.");
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
            System.out.println("У эпика №" + epic.getId() + " " + epic.getName() + " статус обновлен до " + epic.getStatus());
        }
    }

    public void updateTask(Task task) {
        if (task != null && task.getId() != 0 && taskList.containsKey(task.getId())) {

            taskList.put(task.getId(), task);
            if (task instanceof SubTask) {
                SubTask subTask = (SubTask) task;
                subTaskList.put(subTask.getId(), subTask);
                resetEpicStatus(subTask.getEpicId());
            } else if (task instanceof Epic) {
                Epic newEpic = (Epic) task;
                Epic oldEpic = epicList.get(task.getId());

                ArrayList<Integer> newIdList = newEpic.getSubTaskIdList();
                ArrayList<Integer> oldIdList = oldEpic.getSubTaskIdList();

                for (Integer subTaskId : oldIdList) {
                    newIdList.add(subTaskId);
                }

                epicList.put(newEpic.getId(), newEpic);
                resetEpicStatus(task.getId());
            }
            System.out.println("Задача с id: " + task.getId() + " " + task.getName() + " обновлена. Статус: " + task.getStatus());
        } else {
            if (task != null) {
                System.out.println("Id задачи " + task.getId() + ", переданной в метод, нет в списке задач.");
            } else {
                System.out.println("В метод updateTask передан некорректный объект.");
            }
        }
    }

    public static int getTaskCount() {
        return taskCount;
    }
}
