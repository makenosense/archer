package archer.model;

public class RepositoryLogTreeNode {

    public String id;
    public String parent;
    public String type;
    public String text;
    public State state = new State();

    public static class State {

        public boolean opened = false;

        public boolean isOpened() {
            return opened;
        }
    }

    public String getId() {
        return id;
    }

    public String getParent() {
        return parent;
    }

    public String getType() {
        return type;
    }

    public String getText() {
        return text;
    }

    public State getState() {
        return state;
    }
}
