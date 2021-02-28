package archer.model;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.Objects;

public class RepositoryPath extends BaseModel {
    private static final int MAX_HISTORY_SIZE = 100;

    private final LinkedList<Path> pathHistory = new LinkedList<>();

    private Path path;
    private int idx;

    public RepositoryPath() {
        this("/");
    }

    public RepositoryPath(String pathString) {
        this(Paths.get(pathString));
    }

    public RepositoryPath(Path path) {
        switchAndSaveHistory(path.normalize());
    }

    private static boolean equals(Path p1, Path p2) {
        return Objects.equals(p1.normalize().toString(), p2.normalize().toString());
    }

    private void popUntilCurrent() {
        if (idx < 0 || idx >= pathHistory.size() || pathHistory.get(idx) != path) {
            idx = pathHistory.indexOf(path);
            if (idx < 0) {
                switchAndSaveHistory(path);
            }
        }
        while (idx > 0) {
            pathHistory.pop();
            idx--;
        }
    }

    private void switchAndSaveHistory(Path newPath) {
        path = newPath;
        pathHistory.push(path);
        idx = 0;
        while (pathHistory.size() > MAX_HISTORY_SIZE) {
            pathHistory.removeLast();
        }
    }

    public boolean hasPrevious() {
        return idx + 1 < pathHistory.size();
    }

    public boolean hasNext() {
        return idx - 1 >= 0;
    }

    public boolean hasParent() {
        Path parent = path.getParent();
        return parent != null && !equals(path, parent);
    }

    public boolean goPrevious() {
        if (hasPrevious()) {
            idx++;
            path = pathHistory.get(idx);
            return true;
        }
        return false;
    }

    public boolean goNext() {
        if (hasNext()) {
            idx--;
            path = pathHistory.get(idx);
            return true;
        }
        return false;
    }

    public boolean goParent() {
        if (hasParent()) {
            popUntilCurrent();
            switchAndSaveHistory(path.getParent().normalize());
            return true;
        }
        return false;
    }

    public boolean goPath(String pathString) {
        Path newPath = path.resolve(pathString).normalize();
        if (!equals(path, newPath)) {
            popUntilCurrent();
            switchAndSaveHistory(newPath);
            return true;
        }
        return false;
    }

    public RepositoryPath resolve(String pathString) {
        return new RepositoryPath(path.resolve(pathString));
    }

    public RepositoryPathNode getPathNode() {
        return new RepositoryPathNode(path);
    }

    public LinkedList<RepositoryPathNode> getPathNodeList() {
        Path path = this.path.normalize();
        LinkedList<RepositoryPathNode> pathNodeList = new LinkedList<>();
        while (path.getParent() != null) {
            pathNodeList.push(new RepositoryPathNode(path));
            path = path.getParent();
        }
        return pathNodeList;
    }

    @Override
    public String toString() {
        return path.normalize().toString().replaceAll("\\\\", "/");
    }
}
