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
    private transient Object[] logTreeNodes;

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

    private void addLogTreePathNodes(long revision, Map<String, SVNLogEntryPath> changedPaths,
                                     HashSet<String> movedPaths, HashMap<String, String> renamedPaths,
                                     HashMap<String, RepositoryLogTreeNode> pathNodes) {
        HashSet<String> countedMovedPaths = new HashSet<>();
        Path revisionNodePath = Paths.get("r" + revision);
        changedPaths.keySet().stream().sorted().forEach(key -> {
            SVNLogEntryPath changedPath = changedPaths.get(key);
            String path = changedPath.getPath();
            if (!movedPaths.contains(path)) {
                SVNNodeKind kind = changedPath.getKind();
                char type = changedPath.getType();
                String copyPath = changedPath.getCopyPath();
                long copyRevision = changedPath.getCopyRevision();

                Path nodePath = Paths.get(revisionNodePath.toString(), path);
                Path parentNodePath = nodePath.getParent();
                String nodeName = nodePath.getFileName().toString();
                String nodeType = "";
                if (kind == SVNNodeKind.DIR) {
                    nodeType = ("dir_" + type).toLowerCase(Locale.ROOT);
                } else if (kind == SVNNodeKind.FILE) {
                    nodeType = ("file_" + type).toLowerCase(Locale.ROOT);
                }
                String nodeComment = "";
                if (copyPath != null && copyRevision >= 0) {
                    String commentAction = "copy";
                    if (movedPaths.contains(copyPath)
                            && !countedMovedPaths.contains(copyPath)
                            && !renamedPaths.containsKey(copyPath)) {
                        commentAction = "move";
                        countedMovedPaths.add(copyPath);
                    }
                    if (path.equals(renamedPaths.getOrDefault(copyPath, null))) {
                        commentAction = "rename";
                    }
                    nodeComment = String.format("%s from (%d) %s", commentAction, copyRevision, copyPath);
                }

                pathNodes.put(nodePath.toString(), new RepositoryLogTreeNode(
                        nodePath.toString(), parentNodePath.toString(), nodeType, nodeName, nodeComment));
                while (!parentNodePath.equals(revisionNodePath)) {
                    Path grandParentNodePath = parentNodePath.getParent();
                    String parentNodeName = parentNodePath.getFileName().toString();
                    pathNodes.putIfAbsent(parentNodePath.toString(), new RepositoryLogTreeNode(
                            parentNodePath.toString(), grandParentNodePath.toString(), "dir", parentNodeName, ""));
                    parentNodePath = grandParentNodePath;
                }
            }
        });
    }

    private void reduceLogTreePathNodes(HashMap<String, RepositoryLogTreeNode> pathNodes) {
        HashMap<String, Integer> childrenCount = new HashMap<>();
        pathNodes.values().forEach(node -> {
            childrenCount.putIfAbsent(node.parent, 0);
            childrenCount.computeIfPresent(node.parent, (k, v) -> ++v);
        });
        pathNodes.keySet().stream().sorted().forEach(key -> {
            RepositoryLogTreeNode node = pathNodes.get(key);
            if (node.type.startsWith("dir")
                    && childrenCount.get(node.parent) == 1
                    && pathNodes.containsKey(node.parent)) {
                childrenCount.computeIfPresent(node.parent, (k, v) -> -1);
                Path newParentNodePath = Paths.get(pathNodes.get(node.parent).parent);
                node.parent = newParentNodePath.toString();
                node.text = newParentNodePath.relativize(Paths.get(node.id)).toString();
            }
        });
        pathNodes.keySet().removeIf(key -> childrenCount.getOrDefault(key, 0) < 0);
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
            HashSet<String> movedPaths = new HashSet<>();
            HashMap<String, String> renamedPaths = new HashMap<>();
            changedPaths.keySet().forEach(key -> {
                SVNLogEntryPath changedPath = changedPaths.get(key);
                String path = changedPath.getPath();
                String copyPath = changedPath.getCopyPath();
                long copyRevision = changedPath.getCopyRevision();
                if (changedPaths.containsKey(copyPath)
                        && changedPaths.get(copyPath).getType() == 'D'
                        && copyRevision == revision - 1) {
                    movedPaths.add(copyPath);
                    if (Paths.get(path).getParent().equals(Paths.get(copyPath).getParent())) {
                        renamedPaths.putIfAbsent(copyPath, path);
                    }
                }
            });

            String dateNodeText = String.format(RepositoryLogTreeNode.DATE_TEXT_TPL, dateString);
            dateNodes.putIfAbsent(dateString, new RepositoryLogTreeNode(
                    dateString, "#", "date", dateNodeText, ""));

            String revisionNodeText = String.format(RepositoryLogTreeNode.REVISION_TEXT_TPL,
                    date, date, revision, message, changedPaths.size() - movedPaths.size());
            revisionNodes.put(revision, new RepositoryLogTreeNode(
                    "r" + revision, dateString, "revision", revisionNodeText, ""));

            addLogTreePathNodes(revision, changedPaths, movedPaths, renamedPaths, pathNodes);
        }
        if (latestDateString != null) {
            dateNodes.get(latestDateString).state.opened = true;
        }
        LinkedList<RepositoryLogTreeNode> nodes = new LinkedList<>();
        nodes.addAll(dateNodes.values());
        nodes.addAll(revisionNodes.values());
        reduceLogTreePathNodes(pathNodes);
        nodes.addAll(pathNodes.values());
        logTreeNodes = nodes.toArray();
        return logTreeNodes;
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
