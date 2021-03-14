package archer.model;

import archer.util.FileUtil;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.io.SVNRepository;

import java.io.File;
import java.nio.file.Paths;
import java.util.UUID;

public class DownloadTask {

    private final UUID uuid;
    private final String path;
    private final SVNDirEntry dirEntry;
    private final RepositoryPathNode parentPathNode;
    private final File downloadParent;
    private final DownloadReporter reporter;
    private final DownloadEditor editor;

    public DownloadTask(String path, SVNDirEntry dirEntry, RepositoryPathNode parentPathNode) {
        this(path, dirEntry, parentPathNode, null);
    }

    public DownloadTask(String path, SVNDirEntry dirEntry, RepositoryPathNode parentPathNode, File downloadParent) {
        this.uuid = UUID.randomUUID();
        this.path = path;
        this.dirEntry = dirEntry;
        this.parentPathNode = parentPathNode;
        this.downloadParent = downloadParent;
        this.reporter = new DownloadReporter(path, dirEntry.getRevision());
        this.editor = new DownloadEditor(parentPathNode, downloadParent);
    }

    public void execute(SVNRepository repository) throws SVNException {
        repository.update(dirEntry.getRevision(), null, true, reporter, editor);
    }

    public String getUuid() {
        return uuid.toString();
    }

    public String getName() {
        return Paths.get(path).getFileName().toString();
    }

    public long getSize() {
        return dirEntry.getSize();
    }

    public String getSizeString() {
        return FileUtil.getSizeString(getSize());
    }

    public double getDownloadPercent() {
        return 1. * editor.getLastReceivedTotal() / Math.max(getSize(), 1);
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof DownloadTask
                && ((DownloadTask) obj).uuid.equals(uuid);
    }
}
