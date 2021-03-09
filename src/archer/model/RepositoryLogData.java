package archer.model;

import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.io.SVNRepository;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

public class RepositoryLogData extends BaseModel implements Serializable {
    private static final long serialVersionUID = 20210308000L;
    private static final String CACHE_PATH = Paths.get(APP_HOME, "cache").toString();
    private static final String CACHE_SUFFIX = ".logcache";
    private static final Logger LOGGER = Logger.getLogger("RepositoryLogData");

    private String repositoryUUID;
    private LinkedList<SVNLogEntry> logEntries;
    private Long lastChangeTime;

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
                try (FileInputStream fileInputStream = new FileInputStream(logCacheFile);
                     ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream)) {
                    return (RepositoryLogData) objectInputStream.readObject();
                }
            }
        } catch (Exception e) {
            LOGGER.warning("历史记录缓存文件读取失败：" + e.toString());
        }
        return new RepositoryLogData(repositoryUUID);
    }

    public void save() throws Exception {
        if (repositoryUUID != null) {
            try (FileOutputStream fileOutputStream = new FileOutputStream(getLogCacheFile(repositoryUUID));
                 ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream)) {
                objectOutputStream.writeObject(this);
            }
        }
    }

    public void dumpCache() {
        if (repositoryUUID != null) {
            getLogCacheFile(repositoryUUID).delete();
        }
    }

    public long getYoungestRevision() {
        return logEntries.size() > 0 ? logEntries.peek().getRevision() : -1;
    }

    private void addLogTreePathNodes(long revision, Map<String, SVNLogEntryPath> changedPaths, HashMap<String, RepositoryLogTreeNode> pathNodes) {
        Path revisionNodePath = Paths.get("r" + revision);
        changedPaths.keySet().stream().sorted().forEach(k -> {
            SVNLogEntryPath changedPath = changedPaths.get(k);
            String path = changedPath.getPath();
            SVNNodeKind kind = changedPath.getKind();
            char type = changedPath.getType();

            Path nodePath = Paths.get("r" + revision, path);
            Path parentNodePath = nodePath.getParent();
            String nodeName = nodePath.getFileName().toString();
            String nodeType = "";
            if (kind == SVNNodeKind.DIR) {
                nodeType = ("dir_" + type).toLowerCase(Locale.ROOT);
            } else if (kind == SVNNodeKind.FILE) {
                nodeType = ("file_" + type).toLowerCase(Locale.ROOT);
            }
            pathNodes.put(nodePath.toString(), new RepositoryLogTreeNode(
                    nodePath.toString(), parentNodePath.toString(), nodeType, nodeName));
            while (!parentNodePath.equals(revisionNodePath)) {
                Path grandParentNodePath = parentNodePath.getParent();
                String parentNodeName = parentNodePath.getFileName().toString();
                pathNodes.putIfAbsent(parentNodePath.toString(), new RepositoryLogTreeNode(
                        parentNodePath.toString(), grandParentNodePath.toString(), "dir", parentNodeName));
                parentNodePath = grandParentNodePath;
            }
        });
    }

    public Object[] buildLogTreeNodeArray() {
        HashMap<String, RepositoryLogTreeNode> dateNodes = new HashMap<>();
        HashMap<Long, RepositoryLogTreeNode> revisionNodes = new HashMap<>();
        HashMap<String, RepositoryLogTreeNode> pathNodes = new HashMap<>();
        String latestDateString = null;
        for (SVNLogEntry logEntry : logEntries) {
            Date date = logEntry.getDate();
            String dateString = String.format("%tF", date);
            if (latestDateString == null || dateString.compareTo(latestDateString) > 0) {
                latestDateString = dateString;
            }
            long revision = logEntry.getRevision();
            String message = logEntry.getMessage();
            Map<String, SVNLogEntryPath> changedPaths = logEntry.getChangedPaths();
            String dateNodeText = String.format(RepositoryLogTreeNode.DATE_TEXT_TPL, dateString);
            String revisionNodeText = String.format(RepositoryLogTreeNode.REVISION_TEXT_TPL, date, revision, message, changedPaths.size());

            dateNodes.putIfAbsent(dateString, new RepositoryLogTreeNode(dateString, "#", "date", dateNodeText));
            revisionNodes.put(revision, new RepositoryLogTreeNode("r" + revision, dateString, "revision", revisionNodeText));
            addLogTreePathNodes(revision, changedPaths, pathNodes);
        }
        if (latestDateString != null) {
            dateNodes.get(latestDateString).state.opened = true;
        }
        LinkedList<RepositoryLogTreeNode> nodes = new LinkedList<>();
        nodes.addAll(dateNodes.values());
        nodes.addAll(revisionNodes.values());
        reduceLogTreePathNodes(pathNodes);
        nodes.addAll(pathNodes.values());
        return nodes.toArray();
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

    public Long getLastChangeTime() {
        return lastChangeTime;
    }

    public String getLastChangeTimeString() {
        Date date = new Date(lastChangeTime);
        return String.format("%tF %tT", date, date);
    }

    public void setLastChangeTime(Long lastChangeTime) {
        this.lastChangeTime = lastChangeTime;
    }
}
