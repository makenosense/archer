package archer.control;

import archer.model.RepositoryPathNode;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.io.SVNRepository;

import java.io.File;

public class DownloadTask {

    private final long revision;
    private final DownloadReporterBaton reporter;
    private final DownloadEditor editor;

    public DownloadTask(String path, long revision, RepositoryPathNode parentPathNode) {
        this(path, revision, parentPathNode, null);
    }

    public DownloadTask(String path, long revision, RepositoryPathNode parentPathNode, File downloadParent) {
        this.revision = revision;
        reporter = new DownloadReporterBaton(path, revision);
        editor = new DownloadEditor(parentPathNode, downloadParent);
    }

    public void execute(SVNRepository repository) throws SVNException {
        repository.update(revision, null, true, reporter, editor);
    }
}
