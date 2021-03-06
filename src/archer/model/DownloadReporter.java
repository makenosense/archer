package archer.model;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.io.ISVNReporter;
import org.tmatesoft.svn.core.io.ISVNReporterBaton;

public class DownloadReporter implements ISVNReporterBaton {

    private final String path;
    private final long revision;

    public DownloadReporter(String path, long revision) {
        this.path = path;
        this.revision = revision;
    }

    @Override
    public void report(ISVNReporter reporter) throws SVNException {
        try {
            reporter.setPath("", null, revision, SVNDepth.INFINITY, false);
            reporter.deletePath(path);
            reporter.finishReport();
        } catch (Exception e) {
            reporter.abortReport();
            throw e;
        }
    }
}
