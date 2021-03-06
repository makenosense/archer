package archer.model;

import java.nio.file.Path;

public class RepositoryPathNode {

    private final Path path;

    public RepositoryPathNode(Path path) {
        this.path = path.normalize();
    }

    public Path getPath() {
        return path;
    }

    @Override
    public String toString() {
        return path.toString().replaceAll("\\\\", "/");
    }

    public String getName() {
        return path.getFileName().toString();
    }

    public RepositoryPathNode getParent() {
        Path parentPath = path.getParent();
        return parentPath != null ?
                new RepositoryPathNode(parentPath) : null;
    }

    public RepositoryPathNode resolve(String pathString) {
        return new RepositoryPathNode(path.resolve(pathString));
    }

    public Path relativize(Path other) {
        return path.relativize(other);
    }
}
