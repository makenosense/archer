package archer.model;

import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.io.SVNRepository;

import java.io.*;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.logging.Logger;

public class RepositoryLogData extends BaseModel implements Serializable {
    private static final long serialVersionUID = 20210308000L;
    private static final String CACHE_PATH = Paths.get(APP_HOME, "cache").toString();
    private static final String CACHE_SUFFIX = ".logcache";
    private static final Logger LOGGER = Logger.getLogger("RepositoryLogData");

    private String repositoryUUID;
    private LinkedList<SVNLogEntry> logEntries;

    static {
        File cache = new File(CACHE_PATH);
        if (!cache.isDirectory()) {
            cache.mkdirs();
        }
    }

    public RepositoryLogData() {
        this(null);
    }

    private RepositoryLogData(String repositoryUUID) {
        this.repositoryUUID = repositoryUUID;
        logEntries = new LinkedList<>();
    }

    private static File getLogCacheFile(String repositoryUUID) {
        return new File(CACHE_PATH, repositoryUUID + CACHE_SUFFIX);
    }

    public static RepositoryLogData load(SVNRepository repository) {
        String repositoryUUID = null;
        try {
            repositoryUUID = repository.getRepositoryUUID(true);
            File logCacheFile = getLogCacheFile(repositoryUUID);
            if (logCacheFile.isFile()) {
                return (RepositoryLogData) new ObjectInputStream(new FileInputStream(logCacheFile)).readObject();
            }
        } catch (Exception e) {
            LOGGER.warning("历史记录缓存文件读取失败：" + e.toString());
        }
        return new RepositoryLogData(repositoryUUID);
    }

    public void save() throws Exception {
        if (repositoryUUID != null) {
            new ObjectOutputStream(new FileOutputStream(getLogCacheFile(repositoryUUID))).writeObject(this);
        }
    }

    public long getYoungestRevision() {
        return logEntries.size() > 0 ? logEntries.peek().getRevision() : -1;
    }

    public String getRepositoryUUID() {
        return repositoryUUID;
    }

    public void setRepositoryUUID(String repositoryUUID) {
        this.repositoryUUID = repositoryUUID;
    }

    public LinkedList<SVNLogEntry> getLogEntries() {
        return logEntries;
    }

    public void setLogEntries(LinkedList<SVNLogEntry> logEntries) {
        this.logEntries = logEntries;
    }
}
