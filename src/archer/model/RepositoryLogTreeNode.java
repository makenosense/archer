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
}
