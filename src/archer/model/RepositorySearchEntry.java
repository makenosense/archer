package archer.model;

import org.tmatesoft.svn.core.SVNDirEntry;

public class RepositorySearchEntry extends RepositoryDirEntry {

    private final RepositoryPathNode dirPathNode;

    public RepositorySearchEntry(RepositoryPathNode dirPathNode, SVNDirEntry entry) {
        super(entry);
        this.dirPathNode = dirPathNode;
    }

    @Override
    public String getPath() {
        return dirPathNode.resolve(getName()).getPath();
    }
}
