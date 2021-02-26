package archer.model;

import java.nio.file.Path;

public class RepositoryPathNode extends BaseModel {

    private final Path path;

    public RepositoryPathNode(Path path) {
        this.path = path;
    }

    public String getPath() {
        return path.toString().replaceAll("\\\\", "/");
    }

    public String getName() {
        return path.getFileName().toString();
    }
}
