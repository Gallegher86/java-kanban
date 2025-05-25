import com.yandex.taskmanager.model.Task;
import com.yandex.taskmanager.model.Epic;
import com.yandex.taskmanager.model.SubTask;
import com.yandex.taskmanager.model.Status;

import com.yandex.taskmanager.service.TaskManager;
import com.yandex.taskmanager.service.Managers;

import java.util.ArrayList;
import java.util.Scanner;
import java.util.List;
import java.util.Optional;

public class Main {
    public static void main(String[] args) {
        TaskManager manager = Managers.getDefaultTaskManager();
        Scanner scanner = new Scanner(System.in);

        Task testTask1 = new Task("Get something 1", "Do something 1 to get something 1");
        Task testTask2 = new Task("Get something 2", "Do something 2 to get something 2");
        Epic testEpic1 = new Epic("Great plan", "PROFIT");
        Epic testEpic2 = new Epic("Small plan", "small PROFIT");

        SubTask testSubTask1 = new SubTask("Phase 1", "Steal socks", 3);
        SubTask testSubTask2 = new SubTask("Phase 2", "???", 3);
        SubTask testSubTask3 = new SubTask("Phase 3", "PROFIT!", 3);
        SubTask testSubTask4 = new SubTask("Make nanoSoft", "small PROFIT", 4);

        manager.addTask(testTask1);
        manager.addTask(testTask2);
        manager.addEpic(testEpic1);
        manager.addEpic(testEpic2);
        manager.addSubTask(testSubTask1);
        manager.addSubTask(testSubTask2);
        manager.addSubTask(testSubTask3);
        manager.addSubTask(testSubTask4);

        for (int i = 1; i < 9; i++) {
            manager.getTaskById(i);
        }
        System.out.println(getIds(manager.getHistory()));

        for (int i = 8; i > 0; i--) {
            manager.getTaskById(i);
        }
        System.out.println(getIds(manager.getHistory()));

        manager.getTaskById(7);
        manager.getTaskById(6);
        manager.getTaskById(5);
        System.out.println(getIds(manager.getHistory()));

        manager.deleteTaskById(1);
        System.out.println(getIds(manager.getHistory()));

        manager.deleteTaskById(3);
        System.out.println(getIds(manager.getHistory()));

        runMainMenu(manager, scanner);
    }

    private static void printMainMenu() {
        System.out.println("-".repeat(20));
        System.out.println("Отладочное меню TaskManager.");
        System.out.println("Выберите команду:");
        System.out.println("-".repeat(20));
        System.out.println("1 - Добавить новую задачу в TaskManager.");
        System.out.println("2 - Получить задачи/эпики/подзадачи.");
        System.out.println("3 - Получить задачу по id/посмотреть историю просмотров.");
        System.out.println("4 - Обновить хранящуюся задачу.");
        System.out.println("5 - Удалить задачу по идентификатору");
        System.out.println("6 - Удалить все задачи.");
        System.out.println("0 - Выйти из программы.");
        System.out.println("-".repeat(20));
    }

    private static void printAddTaskMenu() {
        System.out.println("-".repeat(20));
        System.out.println("Выберите команду:");
        System.out.println("-".repeat(20));
        System.out.println("1 - Добавить новую задачу.");
        System.out.println("2 - Добавить новый эпик.");
        System.out.println("3 - Добавить новую подзадачу.");
        System.out.println("0 - Выйти из раздела.");
        System.out.println("-".repeat(20));
    }

    private static void printGetTasksMenu() {
        System.out.println("-".repeat(20));
        System.out.println("Выберите команду:");
        System.out.println("-".repeat(20));
        System.out.println("1 - Просмотреть полный список Task Manager.");
        System.out.println("2 - Просмотреть все задачи Task Manager.");
        System.out.println("3 - Просмотреть все эпики Task Manager.");
        System.out.println("4 - Просмотреть все подзадачи Task Manager.");
        System.out.println("0 - Выйти из раздела.");
        System.out.println("-".repeat(20));
    }

    private static void printGetTaskByIdMenu() {
        System.out.println("-".repeat(20));
        System.out.println("Выберите команду:");
        System.out.println("-".repeat(20));
        System.out.println("1 - Получить задачу по идентификатору.");
        System.out.println("2 - Получить список всех подзадач эпика.");
        System.out.println("3 - Просмотреть историю просмотра задач.");
        System.out.println("4 - Очистить историю просмотра задач.");
        System.out.println("0 - Выйти из раздела.");
        System.out.println("-".repeat(20));
    }

    private static void printUpdateTaskMenu() {
        System.out.println("-".repeat(20));
        System.out.println("Выберите команду:");
        System.out.println("-".repeat(20));
        System.out.println("1 - Обновить задачу.");
        System.out.println("2 - Обновить эпик.");
        System.out.println("3 - Обновить подзадачу.");
        System.out.println("0 - Выйти из раздела.");
        System.out.println("-".repeat(20));
    }

    private static void runMainMenu(TaskManager manager, Scanner scanner) {
        while (true) {
            printMainMenu();
            System.out.print("Введите команду: ");
            String command = scanner.nextLine();

            switch (command) {
                case "1":
                    runAddTaskMenu(manager, scanner);
                    break;
                case "2": {
                    runGetTasksMenu(manager, scanner);
                    break;
                }
                case "3": {
                    runGetTaskByIdMenu(manager, scanner);
                    break;
                }
                case "4": {
                    runUpdateTaskMenu(manager, scanner);
                    break;
                }
                case "5": {
                    printTaskInfo(manager.getAllTasks());
                    System.out.print("Введите идентификатор задачи на удаление: ");
                    int deleteId = scanner.nextInt();
                    scanner.nextLine();
                    try {
                        manager.deleteTaskById(deleteId);
                        System.out.println("Задача удалена.");
                    } catch (IllegalArgumentException ex) {
                        System.out.println(ex.getMessage());
                    }
                    break;
                }
                case "6": {
                    System.out.print("Вы уверены, что хотите удалить все задачи? Если да, нажмите 1: ");
                    String lastChance = scanner.nextLine();
                    if (lastChance.equals("1")) {
                        manager.deleteAllTasks();
                        System.out.println("Все задачи удалены.");
                    } else {
                        System.out.println("Удаление отменено.");
                    }
                    break;
                }
                case "0":
                    System.out.println("Работа программы завершена.");
                    return;
            }
        }
    }

    private static void runAddTaskMenu(TaskManager manager, Scanner scanner) {
        boolean returnToMainMenu = false;
        while (!returnToMainMenu) {
            printAddTaskMenu();
            System.out.print("Введите команду: ");
            String command = scanner.nextLine();
            switch (command) {
                case "1": {
                    manager.addTask(makeNewTask(scanner));
                    System.out.println("Задача создана и добавлена в менеджер.");
                    break;
                }
                case "2": {
                    manager.addEpic(makeNewEpic(scanner));
                    System.out.println("Эпик создан и добавлен в менеджер.");
                    break;
                }
                case "3": {
                    try {
                        printTaskInfo(manager.getEpics());
                        manager.addSubTask(makeNewSubTask(scanner));
                        System.out.println("Подзадача создана и добавлена в менеджер.");
                    } catch (IllegalArgumentException ex) {
                        System.out.println("Ошибка при создании подзадачи: " + ex.getMessage());
                        System.out.println("Попробуйте ввести данные ещё раз.");
                    }
                    break;
                }
                case "0": {
                    returnToMainMenu = true;
                    break;
                }
                default:
                    System.out.println("Введена неизвестная команда.");
                    break;
            }
        }
    }

    private static void runGetTasksMenu(TaskManager manager, Scanner scanner) {
        boolean returnToMainMenu = false;
        while (!returnToMainMenu) {
            printGetTasksMenu();
            System.out.print("Введите команду: ");
            String command = scanner.nextLine();
            switch (command) {
                case "1": {
                    List<Task> allTasks = manager.getAllTasks();
                    if (allTasks.isEmpty()) {
                        System.out.println("В менеджере нет никаких задач.");
                    } else {
                        for (Task task : allTasks) {
                            System.out.println(task);
                            System.out.println("-".repeat(100));
                        }
                    }
                    break;
                }
                case "2": {
                    List<Task> tasks = manager.getTasks();
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
                    List<Epic> epics = manager.getEpics();
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
                    List<SubTask> subTasks = manager.getSubTasks();
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
                case "0": {
                    returnToMainMenu = true;
                    break;
                }
                default:
                    System.out.println("Введена неизвестная команда.");
                    break;
            }
        }
    }

    private static void runGetTaskByIdMenu(TaskManager manager, Scanner scanner) {
        boolean returnToMainMenu = false;
        while (!returnToMainMenu) {
            printGetTaskByIdMenu();
            System.out.print("Введите команду: ");
            String command = scanner.nextLine();
            switch (command) {
                case "1": {
                    printTaskInfo(manager.getAllTasks());
                    System.out.print("Введите идентификатор задачи: ");
                    int id = scanner.nextInt();
                    scanner.nextLine();
                    Optional<Task> task = manager.getTaskById(id);
                    if (task.isPresent()) {
                        System.out.println(task.get());
                    } else {
                        System.out.println("Введенный id: " + id + " не найден в списке задач.");
                    }
                    break;
                }
                case "2": {
                    printTaskInfo(manager.getEpics());
                    System.out.print("Введите идентификатор эпика для получения подзадач: ");
                    int epicId = scanner.nextInt();
                    scanner.nextLine();
                    List<SubTask> subTasks = manager.getEpicSubTasks(epicId);
                    if (subTasks.isEmpty()) {
                        System.out.println("Введенный id: " + epicId + " не найден в списке эпиков.");
                    } else {
                        for (SubTask subTask : subTasks) {
                            System.out.println(subTask);
                        }
                    }
                    break;
                }
                case "3": {
                    List<Task> historyList = manager.getHistory();
                    if (historyList.isEmpty()) {
                        System.out.println("История просмотров пуста.");
                    } else {
                        for (Task history : historyList) {
                            System.out.println(history);
                            System.out.println("-".repeat(100));
                        }
                    }
                    break;
                }
                case "4": {
                    manager.clearHistory();
                    System.out.println("История просмотров очищена.");
                    break;
                }
                case "0": {
                    returnToMainMenu = true;
                    break;
                }
                default:
                    System.out.println("Введена неизвестная команда.");
                    break;
            }
        }
    }

    private static void runUpdateTaskMenu(TaskManager manager, Scanner scanner) {
        boolean returnToMainMenu = false;
        while (!returnToMainMenu) {
            printUpdateTaskMenu();
            System.out.print("Введите команду: ");
            String command = scanner.nextLine();
            switch (command) {
                case "1": {
                    try {
                        printTaskInfo(manager.getTasks());
                        manager.updateTask(makeTaskForUpdate(scanner));
                        System.out.println("Задача обновлена.");
                    } catch (IllegalArgumentException ex) {
                        System.out.println("Ошибка при обновлении задачи: " + ex.getMessage());
                        System.out.println("Попробуйте ввести данные ещё раз.");
                    }
                    break;
                }
                case "2": {
                    try {
                        printTaskInfo(manager.getEpics());
                        manager.updateEpic(makeEpicForUpdate(scanner));
                        System.out.println("Эпик обновлен.");
                    } catch (IllegalArgumentException ex) {
                        System.out.println("Ошибка при обновлении эпика: " + ex.getMessage());
                        System.out.println("Попробуйте ввести данные ещё раз.");
                    }
                    break;
                }
                case "3": {
                    try {
                        printTaskInfo(manager.getSubTasks());
                        manager.updateSubTask(makeSubTaskForUpdate(scanner));
                        System.out.println("Подзадача обновлена.");
                    } catch (IllegalArgumentException ex) {
                        System.out.println("Ошибка при обновлении подзадачи: " + ex.getMessage());
                        System.out.println("Попробуйте ввести данные ещё раз.");
                    }
                    break;
                }
                case "0": {
                    returnToMainMenu = true;
                    break;
                }
                default:
                    System.out.println("Введена неизвестная команда.");
                    break;
            }
        }
    }

    private static Task makeNewTask(Scanner scanner) {
        System.out.print("Введите имя задачи: ");
        String name = scanner.nextLine().trim();
        System.out.print("Введите описание задачи: ");
        String description = scanner.nextLine().trim();
        return new Task(name, description);
    }

    private static Epic makeNewEpic(Scanner scanner) {
        System.out.print("Введите имя эпика: ");
        String name = scanner.nextLine().trim();
        System.out.print("Введите описание эпика: ");
        String description = scanner.nextLine().trim();
        return new Epic(name, description);
    }

    private static SubTask makeNewSubTask(Scanner scanner) {
        System.out.print("Введите имя подзадачи: ");
        String name = scanner.nextLine().trim();
        System.out.print("Введите описание подзадачи: ");
        String description = scanner.nextLine().trim();
        System.out.print("Введите идентификатор эпика к которому относится подзадача: ");
        Integer epicId = scanner.nextInt();
        scanner.nextLine();
        return new SubTask(name, description, epicId);
    }

    private static Task makeTaskForUpdate(Scanner scanner) {
        System.out.print("Введите идентификатор задачи: ");
        int id = scanner.nextInt();
        scanner.nextLine();
        System.out.print("Введите новое имя задачи: ");
        String name = scanner.nextLine().trim();
        System.out.print("Введите новое описание задачи: ");
        String description = scanner.nextLine().trim();
        Status status = setStatus(scanner);

        return new Task(id, name, description, status);
    }

    private static Epic makeEpicForUpdate(Scanner scanner) {
        System.out.print("Введите идентификатор эпика: ");
        int id = scanner.nextInt();
        scanner.nextLine();
        System.out.print("Введите новое имя эпика: ");
        String name = scanner.nextLine().trim();
        System.out.print("Введите новое описание эпика: ");
        String description = scanner.nextLine().trim();

        return new Epic(id, name, description);
    }

    private static SubTask makeSubTaskForUpdate(Scanner scanner) {
        System.out.print("Введите идентификатор подзадачи: ");
        int id = scanner.nextInt();
        scanner.nextLine();
        System.out.print("Введите новое имя подзадачи: ");
        String name = scanner.nextLine().trim();
        System.out.print("Введите новое описание подзадачи: ");
        String description = scanner.nextLine().trim();
        Status status = setStatus(scanner);
        System.out.print("Введите идентификатор эпика к которому относится подзадача: ");
        Integer epicId = scanner.nextInt();
        scanner.nextLine();

        return new SubTask(id, name, description, status, epicId);
    }

    private static Status setStatus(Scanner scanner) {
        Status status;

        System.out.println("В каком статусе находится задача - NEW, IN_PROGRESS, DONE?");
        System.out.print("Введите текущий статус задачи: ");
        String statusInput = scanner.nextLine().trim();

        switch (statusInput) {
            case "NEW" -> status = Status.NEW;
            case "IN_PROGRESS" -> status = Status.IN_PROGRESS;
            case "DONE" -> status = Status.DONE;
            default -> {
                System.out.println("Введен несуществующий статус: " + statusInput + ", установлен статус - NEW.");
                status = Status.NEW;
            }
        }
        return status;
    }

    private static void printTaskInfo(List<? extends Task> taskList) {
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

    private static List<Integer> getIds(List<? extends Task> taskList) {
        List<Integer> idList = new ArrayList<>();
        for (Task task : taskList) {
            idList.add(task.getId());
        }
        return idList;
    }
}

