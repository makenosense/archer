package archer.model;

import org.tmatesoft.svn.core.SVNDirEntry;

public class RepositorySearchEntry extends RepositoryDirEntry {

    private final RepositoryPathNode parentPathNode;

    public RepositorySearchEntry(RepositoryPathNode parentPathNode, SVNDirEntry entry) {
        super(entry);
        this.parentPathNode = parentPathNode;
    }

    @Override
    public String getPath() {
        return parentPathNode.resolve(getName()).getPath();
    }
}
