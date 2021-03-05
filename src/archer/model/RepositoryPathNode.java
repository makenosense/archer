package archer.model;

import java.nio.file.Path;

public class RepositoryPathNode extends BaseModel {

    private final Path path;

    public RepositoryPathNode(Path path) {
        this.path = path.normalize();
    }

    public String getPath() {
        return path.toString().replaceAll("\\\\", "/");
    }

    public String getName() {
        return path.getFileName().toString();
    }

    public RepositoryPathNode resolve(String pathString) {
        return new RepositoryPathNode(path.resolve(pathString));
    }

    public Path relativize(Path other) {
        return path.relativize(other);
    }
}
