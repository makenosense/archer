package archer.model;

public class RepositoryLogTreeNode {

    public String id;
    public String parent;
    public String type;
    public String text;
    public State state = new State();

    public static class State {

        public boolean opened = false;
    }

    public RepositoryLogTreeNode(String id, String parent, String type, String text) {
        this(id, parent, type, text, false);
    }

    public RepositoryLogTreeNode(String id, String parent, String type, String text, boolean opened) {
        this.id = id;
        this.parent = parent;
        this.type = type;
        this.text = text;
        this.state.opened = opened;
    }
}
