package yandex.practicum.kanban.tasks;
import java.util.ArrayList;

public class Epic extends Task {
    private final ArrayList<Integer> subTaskIds = new ArrayList<>();

    public Epic(String name, String description) {
        super(name, description);
    }

    public void addSubTaskId(Integer id) {
        subTaskIds.add(id);
    }

    public ArrayList<Integer> getSubTaskIdList() {
        return subTaskIds;
    }
}

