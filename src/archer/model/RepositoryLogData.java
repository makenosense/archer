package archer.model;

import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.io.SVNRepository;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.File;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

@XmlRootElement(name = "RepositoryLogData")
public class RepositoryLogData extends BaseModel {
    private static final String CACHE_PATH = Paths.get(APP_HOME, "cache").toString();
    private static final String CACHE_SUFFIX = ".logcache";
    private static final Logger LOGGER = Logger.getLogger("RepositoryLogData");

    private String repositoryUUID;
    private List<SVNLogEntry> logEntries;

    static {
        File cache = new File(CACHE_PATH);
        if (!cache.isDirectory()) {
            cache.mkdirs();
        }
    }

    public RepositoryLogData() {
        this(null);
    }

    public RepositoryLogData(String repositoryUUID) {
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
                return (RepositoryLogData) JAXBContext.newInstance(RepositoryLogData.class)
                        .createUnmarshaller()
                        .unmarshal(logCacheFile);
            }
        } catch (Exception e) {
            LOGGER.warning("历史记录缓存文件读取失败：" + e.toString());
        }
        return new RepositoryLogData(repositoryUUID);
    }

    public void save() throws Exception {
        if (repositoryUUID != null) {
            Marshaller marshaller = JAXBContext.newInstance(RepositoryLogData.class)
                    .createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(this, getLogCacheFile(repositoryUUID));
        }
    }

    public String getRepositoryUUID() {
        return repositoryUUID;
    }

    public void setRepositoryUUID(String repositoryUUID) {
        this.repositoryUUID = repositoryUUID;
    }

    public List<SVNLogEntry> getLogEntries() {
        return logEntries;
    }

    public void setLogEntries(List<SVNLogEntry> logEntries) {
        this.logEntries = logEntries;
    }
}
