package yandex.practicum.kanban.tasks;
import java.util.ArrayList;
import java.util.HashMap;

public class TaskManager {
    static int taskCount = 0;
    HashMap<Integer, Task> taskList = new HashMap<>();
    HashMap<Integer, Epic> epicList = new HashMap<>();
    HashMap<Integer, SubTask> subTaskList = new HashMap<>();

    public void addNewTask(Task task) {
        if (task != null) {
            taskCount++;
            task.setId(taskCount);
            taskList.put(task.getId(), task);

            if (task instanceof SubTask) {
                SubTask subTask = (SubTask) task;

                subTaskList.put(subTask.getId(), subTask);
                resetEpicStatus(subTask.getId());

                Integer epicId = subTask.getEpicId();
                Epic epic = epicList.get(epicId);
                epic.addSubTaskId(subTask.getId());
            } else if (task instanceof Epic) {
                Epic epic = (Epic) task;

                epicList.put(epic.getId(), epic);
                resetEpicStatus(epic.getId());
            }
        } else {
            System.out.println("В метод addNewTask передан некорректный объект.");
        }
    }

    public ArrayList<Task> getAllTasks() {
        ArrayList<Task> allTasks = new ArrayList<>(taskList.values());

        return allTasks;
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
        return taskList.get(id);
    }

    public ArrayList<SubTask> getEpicSubTasks(int id) {
        Epic epic = epicList.get(id);
        ArrayList<Integer> subTaskIds = epic.getSubTaskIdList();
        ArrayList <SubTask> subTasks = new ArrayList<>();

        for (int subTaskId : subTaskIds) {
            SubTask subTask = subTaskList.get(subTaskId);
            subTasks.add(subTask);
        }
        return subTasks;
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

        } else if (subTaskList.containsKey(id)) {
            SubTask subTask = subTaskList.get(id);
            int epicId = subTask.getEpicId();

            Epic epic = epicList.get(epicId);
            epic.getSubTaskIdList().remove((Integer) id);
            resetEpicStatus(epicId);
            subTaskList.remove(id);
            taskList.remove(id);

        } else if (taskList.containsKey(id)) {
            taskList.remove(id);
        }
        System.out.println("Задача по id " + id + " удалена");
    }

    private void resetEpicStatus(int id) {
        Epic epic = epicList.get(id);
        boolean isNew = true;
        boolean isDone = true;

        for (int subTaskId : new ArrayList<> (epic.getSubTaskIdList())) {
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

    public void updateTask(Task task) {
        if (task != null && task.getId() != 0 && taskList.containsKey(task.getId())) {

            taskList.put(task.getId(), task);
            if (task instanceof SubTask) {
                SubTask subTask = (SubTask) task;
                subTaskList.put(subTask.getId(), subTask);
                resetEpicStatus(subTask.getId());
            } else if (task instanceof Epic) {
                epicList.put(task.getId(), (Epic) task);
            }
            System.out.println("Задача №" + task.getId() + " " + task.getName() + " обновлена. Статус: " + task.getStatus());
        } else {
            if (task != null) {
                System.out.println("Id задачи " + task.getId() + ", переданной в метод нет в списке задач.");
            } else {
                System.out.println("В метод updateTask передан некорректный объект.");
            }
        }
    }

    public static int getTaskCount() {
        return taskCount;
    }
}
