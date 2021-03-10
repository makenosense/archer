package archer.model;

public class RepositoryLogTreeNode {
    public static final String DATE_TEXT_TPL = "<span class='log-date'>%s</span>";
    public static final String REVISION_TEXT_TPL = "<span class='revision-time' title='%tT'>%tR</span> " +
            "(%d) %s<span class='revision-item-count'> - %d项</span>";

    public String id;
    public String parent;
    public String type;
    public String text;
    public String comment;
    public State state = new State();

    public static class State {

        public boolean opened = false;
    }

    public RepositoryLogTreeNode(String id, String parent, String type, String text, String comment) {
        this(id, parent, type, text, comment, false);
    }

    public RepositoryLogTreeNode(String id, String parent, String type, String text, String comment, boolean opened) {
        this.id = id;
        this.parent = parent;
        this.type = type;
        this.text = text;
        this.comment = comment;
        this.state.opened = opened;
    }
}
