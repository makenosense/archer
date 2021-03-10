package archer.control;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.io.ISVNReporter;
import org.tmatesoft.svn.core.io.ISVNReporterBaton;

public class DownloadReporterBaton implements ISVNReporterBaton {

    private final String path;
    private final long revision;

    public DownloadReporterBaton(String path, long revision) {
        this.path = path;
        this.revision = revision;
    }

    @Override
    public void report(ISVNReporter reporter) throws SVNException {
        try {
            reporter.setPath(path, null, revision, SVNDepth.INFINITY, true);
            reporter.finishReport();
        } catch (Exception e) {
            reporter.abortReport();
            throw e;
        }
    }
}
