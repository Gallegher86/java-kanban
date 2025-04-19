package yandex.practicum.kanban.tasks;
import java.util.ArrayList;

public class Epic extends Task {
    private final ArrayList<Integer> subTaskIds = new ArrayList<>();

    public Epic(String name, String description) {
        super(name, description);
    }

    public Epic(int id, String name, String description) {
        super(id, name, description);
    }

    protected void addSubTaskId(int id) {
        subTaskIds.add(id);
    }

    protected ArrayList<Integer> getSubTaskIdList() {
        return subTaskIds;
    }

    @Override
    public String toString() {
        return "Epic{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", status=" + status +
                '}';
    }
}

