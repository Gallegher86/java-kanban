import com.yandex.taskmanager.model.*;
import com.yandex.taskmanager.service.TaskManager;

import java.util.Scanner;
import java.util.ArrayList;

public class Main {
    static Scanner scanner;

    public static void main(String[] args) {
        TaskManager manager = new TaskManager();
        scanner = new Scanner(System.in);

        Task testTask1 = new Task("Get something 1", "Do something 1 to get something 1");
        Task testTask2 = new Task("Get something 2", "Do something 2 to get something 2");
        Epic testEpic1 = new Epic("Great plan", "PROFIT");
        Epic testEpic2 = new Epic("Small plan", "small PROFIT");

        SubTask testSubTask1 = new SubTask("Phase 1", "Steal socks", 3);
        SubTask testSubTask2 = new SubTask("Phase 2", "???", 3);
        SubTask testSubTask3 = new SubTask("Phase 3", "PROFIT!", 3);
        SubTask testSubTask4 = new SubTask("Make nanoSoft", "small PROFIT", 4);

        manager.addNewTask(testTask1);
        manager.addNewTask(testTask2);
        manager.addNewTask(testEpic1);
        manager.addNewTask(testEpic2);
        manager.addNewTask(testSubTask1);
        manager.addNewTask(testSubTask2);
        manager.addNewTask(testSubTask3);
        manager.addNewTask(testSubTask4);

        while (true) {
            printMenu();
            System.out.print("Введите команду: ");
            String command = scanner.nextLine();

            switch (command) {
                case "1":
                    manager.addNewTask(addNewTask());
                    switch (manager.getTaskManagerStatus()) {
                        case OK -> System.out.println("Задача добавлена в менеджер, присвоен id: " + manager.getTaskCount());
                        case WRONG_EPIC_ID -> System.out.println("Ошибка. В метод addNewTask передана подзадача с некорректным " +
                                "epicId.");
                        case WRONG_ID -> System.out.println("Ошибка. В метод addNewTask передан объект с уже заданным id, " +
                                "применен неверный конструктор.");
                        case NULL -> System.out.println("Ошибка. В метод addNewTask передан null-объект.");
                    }
                    break;
                case "2": {
                    ArrayList<Task> tasks = manager.getAllTasks();
                    if (tasks.isEmpty()) {
                        System.out.println("Список задач пуст.");
                    } else {
                        for (Task task : tasks) {
                            System.out.println(task);
                            System.out.println("-".repeat(100));
                        }
                    }
                    break;
                }
                case "3": {
                    ArrayList<Epic> epics = manager.getAllEpics();
                    if (epics.isEmpty()) {
                        System.out.println("Список эпиков пуст.");
                    } else {
                        for (Epic epic : epics) {
                            System.out.println(epic);
                            System.out.println("-".repeat(100));
                        }
                    }
                    break;
                }
                case "4": {
                    ArrayList<SubTask> subTasks = manager.getAllSubTasks();
                    if (subTasks.isEmpty()) {
                        System.out.println("Список подзадач пуст.");
                    } else {
                        for (SubTask subTask : subTasks) {
                            System.out.println(subTask);
                            System.out.println("-".repeat(100));
                        }
                    }
                    break;
                }
                case "5":
                    printTaskInfo(manager.getAllTasks());
                    System.out.print("Введите идентификатор задачи: ");
                    int id = scanner.nextInt();
                    scanner.nextLine();
                    Task task = manager.getTaskById(id);
                    if (task.getId() == 0) {
                        System.out.println("Введенный id: " + id + " не найден в списке задач. Возвращена пустая задача.");
                    } else {
                        System.out.println(task);
                    }
                    break;
                case "6":
                    printTaskInfo(manager.getAllTasks());
                    manager.updateTask(updateTask());
                    switch (manager.getTaskManagerStatus()) {
                        case OK -> System.out.println("Задача обновлена.");
                        case WRONG_CLASS -> System.out.println("Ошибка. Класс переданной в метод updateTask задачи не " +
                                "совпадает с классом существующей задачи.");
                        case WRONG_ID -> System.out.println("Задачи с переданным id нет в списке менеджера.");
                        case WRONG_EPIC_ID -> System.out.println("Эпик с указанным в передаваемой подзадаче epicId не " +
                                "существует, или у задачи указан epicId неверного эпика.");
                    }
                    break;
                case "7":
                    printTaskInfo(manager.getAllEpics());
                    System.out.print("Введите идентификатор эпика для получения подзадач: ");
                    int epicId = scanner.nextInt();
                    scanner.nextLine();
                    ArrayList<SubTask> subTasks = manager.getEpicSubTasks(epicId);
                    if (subTasks.isEmpty()) {
                        System.out.println("Введенный id: " + epicId + " не найден в списке эпиков. Возвращен пустой список.");
                    } else {
                        for (SubTask subTask : subTasks) {
                        System.out.println(subTask);
                        }
                    }
                    break;
                case "8":
                    printTaskInfo(manager.getAllTasks());
                    System.out.print("Введите идентификатор задачи на удаление: ");
                    int deleteId = scanner.nextInt();
                    scanner.nextLine();
                    manager.deleteTaskById(deleteId);
                    switch (manager.getTaskManagerStatus()) {
                        case OK -> System.out.println("Задача по id: " + deleteId + " удалена");
                        case WRONG_ID -> System.out.println("Введенный id: " + deleteId + " не найден в списках менеджера.");
                    }
                    break;
                case "9":
                    System.out.print("Вы уверены, что хотите удалить все задачи? Если да, нажмите 1: ");
                    String lastChance = scanner.nextLine();
                    if (lastChance.equals("1")) {
                        manager.deleteAllTasks();
                        System.out.println("Все задачи удалены.");
                    } else {
                        System.out.println("Удаление отменено.");
                    }
                    break;
                case "10":
                    System.out.println("Работа программы прекращена.");
                    return;
                default:
                    System.out.println("Введена неизвестная команда.");
                    break;
            }
        }

    }

    private static void printMenu() {
        System.out.println("-".repeat(20));
        System.out.println("Отладочное меню TaskManager.");
        System.out.println("Выберите команду:");
        System.out.println("-".repeat(20));
        System.out.println("1 - Добавить новую задачу в TaskManager.");
        System.out.println("2 - Получить список всех хранящихся задач.");
        System.out.println("3 - Получить список всех хранящихся эпиков.");
        System.out.println("4 - Получить список всех хранящихся подзадач.");
        System.out.println("5 - Получить задачу по идентификатору.");
        System.out.println("6 - Обновить хранящуюся задачу.");
        System.out.println("7 - Получить список всех подзадач эпика.");
        System.out.println("8 - Удалить задачу по идентификатору");
        System.out.println("9 - Удалить все задачи.");
        System.out.println("10 - Выйти из программы.");
        System.out.println("-".repeat(20));
    }

    private static Task addNewTask() {
        System.out.print("Введите имя задачи: ");
        String name = scanner.nextLine().trim();
        System.out.print("Введите описание задачи: ");
        String description = scanner.nextLine().trim();
        System.out.print("Задача является подзадачей эпика? Введите 1 если да: ");
        String subTaskChoice = scanner.nextLine().trim();
        if (subTaskChoice.equals("1")) {
            System.out.print("Введите идентификатор эпика к которому относится подзадача: ");
            Integer epicId = scanner.nextInt();
            scanner.nextLine();
            SubTask subTask = new SubTask(name, description, epicId);
            System.out.println("Подзадача создана.");
            return subTask;
        } else {
            System.out.print("Задача является новым эпиком? Введите 1 если да: ");
            String epicChoice = scanner.nextLine().trim();
            if (epicChoice.equals("1")) {
                Epic epic = new Epic(name, description);
                System.out.println("Эпик создан.");
                return epic;
            } else {
                Task task = new Task(name, description);
                System.out.println("Задача создана.");
                return task;
            }
        }
    }

    private static Task updateTask() {
        System.out.print("Введите идентификатор задачи: ");
        int id = scanner.nextInt();
        scanner.nextLine();
        System.out.print("Введите новое имя задачи: ");
        String name = scanner.nextLine().trim();
        System.out.print("Введите новое описание задачи: ");
        String description = scanner.nextLine().trim();

        System.out.println("В каком статусе находится задача - NEW, IN_PROGRESS, DONE? Для эпиков статус " +
                "рассчитывается автоматически.");
        System.out.print("Введите текущий статус задачи: ");
        String statusInput = scanner.nextLine().trim();
        Status status;

        switch (statusInput) {
            case "NEW" -> status = Status.NEW;
            case "IN_PROGRESS" -> status = Status.IN_PROGRESS;
            case "DONE" -> status = Status.DONE;
            default -> {
                System.out.println("Введен несуществующий статус: " + statusInput + ", установлен статус - NEW.");
                status = Status.NEW;
            }
        }

        System.out.print("Задача является подзадачей эпика? Введите 1 если да: ");
        String subTaskChoice = scanner.nextLine().trim();
        if (subTaskChoice.equals("1")) {
            System.out.print("Введите идентификатор эпика к которому относится подзадача: ");
            Integer epicId = scanner.nextInt();
            scanner.nextLine();
            SubTask subTask = new SubTask(id, name, description, status, epicId);
            System.out.println("Подзадача для обновления создана.");
            return subTask;
        } else {
            System.out.print("Задача является новым эпиком? Введите 1 если да: ");
            String epicChoice = scanner.nextLine().trim();
            if (epicChoice.equals("1")) {
                Epic epic = new Epic(id, name, description);
                System.out.println("Эпик для обновления создан.");
                return epic;
            } else {
                Task task = new Task(id, name, description, status);
                System.out.println("Задача для обновления создана.");
                return task;
            }
        }
    }

    private static void printTaskInfo (ArrayList<? extends Task> taskList) {
        System.out.println("Список сохраненных задач:");
        System.out.println("-".repeat(100));
        for (Task task : taskList) {
            int id = task.getId();
            String name = task.getName();

            if (task instanceof SubTask subTask) {
                int epicId = subTask.getEpicId();
                System.out.println("Подзадача id [" + id + "], эпика epicId [" + epicId + "]. Имя: *" + name + "*.");
                System.out.println("-".repeat(100));
            } else if (task instanceof Epic) {
                System.out.println("Эпик id [" + id + "]. Имя: *" + name + "*.");
                System.out.println("-".repeat(100));
            } else {
                System.out.println("Задача id [:" + id + "]. Имя: *" + name + "*.");
                System.out.println("-".repeat(100));
            }
        }
    }
}

