package archer.model;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.io.SVNRepository;

import java.io.File;
import java.nio.file.Paths;

public class DownloadTask {

    private final String path;
    private final long revision;
    private final RepositoryPathNode parentPathNode;
    private final File downloadParent;

    public DownloadTask(String path, long revision, RepositoryPathNode parentPathNode) {
        this(path, revision, parentPathNode, null);
    }

    public DownloadTask(String path, long revision, RepositoryPathNode parentPathNode, File downloadParent) {
        this.path = path;
        this.revision = revision;
        this.parentPathNode = parentPathNode;
        this.downloadParent = downloadParent;
    }

    public void execute(SVNRepository repository) throws SVNException {
        repository.update(revision, null, true,
                new DownloadReporter(path, revision),
                new DownloadEditor(parentPathNode, downloadParent));
    }

    public String getName() {
        return Paths.get(path).getFileName().toString();
    }
}
