package archer.control;

import archer.MainApp;
import archer.model.*;
import archer.util.AlertUtil;
import archer.util.FileUtil;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.diff.SVNDeltaGenerator;
import org.tmatesoft.svn.core.io.diff.SVNDiffWindow;
import org.tmatesoft.svn.util.SVNLogType;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class InterfaceController extends BaseController {
    private static final String TITLE = MainApp.APP_NAME;
    private static final int WIDTH = 900;
    private static final int HEIGHT = 640;
    private static final String INIT_URL = "view/html/interface.html";

    private final JavaApi javaApi = new JavaApi();
    private final RepositoryPath path = new RepositoryPath();

    private SVNRepository repository;

    @FXML
    private WebView webView;

    private WebEngine webEngine;

    @FXML
    public void initialize() {
        webEngine = webView.getEngine();
        webEngine.load(MainApp.class.getResource(INIT_URL).toExternalForm());
        webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == Worker.State.SUCCEEDED) {
                getWindow().setMember("javaApi", javaApi);
                getWindow().call("switchToSidebarTabWithDelay", "sidebar-repo-home");
            }
        });
    }

    @Override
    public String getTitle() {
        return TITLE;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    private JSObject getWindow() {
        return (JSObject) webEngine.executeScript("window");
    }

    public class JavaApi extends BaseJavaApi {

        /**
         * 私有字段
         */
        private final RepositoryContentData repositoryContentData = new RepositoryContentData();
        private RepositoryLogData repositoryLogData;
        private UploadTransactionData uploadTransactionData;

        /**
         * 私有方法
         */
        private LinkedList<String> convertJSStringArray(JSObject array, int length) {
            LinkedList<String> list = new LinkedList<>();
            for (int idx = 0; idx < length; ) {
                list.add((String) array.getSlot(idx++));
            }
            return list;
        }

        private void serviceCleanup() {
            uploadTransactionData = null;
            mainApp.hideProgress();
            webView.setDisable(false);
            getWindow().call("switchRepoNavOps", "repo-nav-ops-refresh", true);
        }

        private ExclusiveService buildNonInteractiveService(Service service, String creationFailedMsg) {
            return new ExclusiveService() {
                @Override
                protected Service createService() {
                    webView.setDisable(true);
                    return service;
                }

                @Override
                protected void onCreationFailed(Exception e) {
                    AlertUtil.error(creationFailedMsg, e);
                    serviceCleanup();
                }
            };
        }

        private void cancelExclusiveService() {
            if (service != null && service.isRunning() && AlertUtil.confirm("确定取消上传吗？")) {
                service.cancel();
            }
        }

        /**
         * 私有服务类
         */
        private class LoadRepositoryContentService extends Service<Void> {

            @Override
            protected Task<Void> createTask() {
                return new Task<Void>() {
                    @Override
                    protected Void call() {
                        try {
                            repositoryContentData.pathNodeList = path.getPathNodeList();
                            if (repository.checkPath(path.toString(), -1) != SVNNodeKind.DIR) {
                                throw new Exception("文件夹不存在");
                            }
                            ArrayList<SVNDirEntry> entryList = new ArrayList<>();
                            repository.getDir(path.toString(), -1, null, entryList);
                            repositoryContentData.entryList = entryList.stream()
                                    .map(RepositoryDirEntry::new)
                                    .collect(Collectors.toList());
                            Platform.runLater(() -> {
                                getWindow().call("updateRepoNav");
                                getWindow().call("sortEntryList");
                            });
                        } catch (Exception e) {
                            Platform.runLater(() -> AlertUtil.error("仓库加载失败", e));
                        } finally {
                            Platform.runLater(JavaApi.this::serviceCleanup);
                        }
                        return null;
                    }
                };
            }
        }

        private class LaunchDownloadTaskService extends Service<Void> {

            private final LinkedList<String> pathList;

            public LaunchDownloadTaskService(LinkedList<String> pathList) {
                this.pathList = pathList;
            }

            @Override
            protected Task<Void> createTask() {
                return new Task<Void>() {
                    @Override
                    protected Void call() {
                        try {
                            Platform.runLater(() -> mainApp.showProgress(-1, "正在收集下载任务"));
                            List<DownloadTask> newDownloadTasks = new LinkedList<>();
                            for (String srcPathString : pathList) {
                                RepositoryPathNode srcPathNode = path.resolve(srcPathString).getPathNode();
                                collectDownloadTasks(srcPathNode, srcPathNode.getParent(), newDownloadTasks);
                            }
                            Platform.runLater(() -> mainApp.showProgress(-1, "正在添加下载任务"));
                            downloadQueue.addAll(newDownloadTasks);
                        } catch (Exception e) {
                            Platform.runLater(() -> AlertUtil.error("下载任务添加失败", e));
                        } finally {
                            Platform.runLater(JavaApi.this::serviceCleanup);
                        }
                        return null;
                    }
                };
            }
        }

        private abstract class EditingService extends Service<Void> {

            protected final String logMessage;
            protected final String errorMessage;

            protected EditingService(String logMessage, String errorMessage) {
                this.logMessage = logMessage;
                this.errorMessage = errorMessage;
            }

            protected void beforeEditing(Task<Void> task) throws Exception {
            }

            protected abstract void doEditing(ISVNEditor editor, Task<Void> task) throws Exception;

            protected void onEditingFailed(Exception e) {
                Platform.runLater(() -> AlertUtil.error(errorMessage, e));
            }

            protected void onEditingSuccess() {
            }

            protected void onEditingComplete() {
            }

            @Override
            protected Task<Void> createTask() {
                return new Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        ISVNEditor editor = null;
                        try {
                            beforeEditing(this);
                            editor = repository.getCommitEditor(logMessage, null);
                            editor.openRoot(-1);
                            doEditing(editor, this);
                            editor.closeDir();
                            editor.closeEdit();
                            onEditingSuccess();
                        } catch (UploadCancelledException e) {
                            try {
                                if (editor != null) {
                                    editor.abortEdit();
                                }
                            } finally {
                                Platform.runLater(() -> AlertUtil.info(e.getMessage()));
                            }
                        } catch (Exception e) {
                            try {
                                if (editor != null) {
                                    editor.abortEdit();
                                }
                            } finally {
                                onEditingFailed(e);
                            }
                        } finally {
                            onEditingComplete();
                        }
                        return null;
                    }
                };
            }
        }

        private abstract class EditingWithRefreshingService extends EditingService {

            protected EditingWithRefreshingService(String logMessage, String errorMessage) {
                super(logMessage, errorMessage);
            }

            @Override
            protected void onEditingComplete() {
                Platform.runLater(() -> {
                    serviceCleanup();
                    getWindow().call("loadRepoContent");
                });
            }
        }

        private class LoadRepositoryLogService extends Service<Void> {

            private boolean rebuild;

            public LoadRepositoryLogService(boolean rebuild) {
                this.rebuild = rebuild;
            }

            @Override
            protected Task<Void> createTask() {
                return new Task<Void>() {
                    @Override
                    protected Void call() {
                        try {
                            if (repositoryLogData == null) {
                                rebuild = true;
                                repositoryLogData = RepositoryLogData.load(repository);
                            }
                            long latestRevision = repository.getLatestRevision();
                            long youngestInCache = repositoryLogData.getYoungestRevision();
                            if (latestRevision < youngestInCache) {
                                repositoryLogData.dumpCache();
                                rebuild = true;
                                repositoryLogData = RepositoryLogData.load(repository);
                                youngestInCache = repositoryLogData.getYoungestRevision();
                            }
                            if (latestRevision > youngestInCache) {
                                LinkedList<SVNLogEntry> newLogEntries = new LinkedList<>();
                                repository.log(new String[]{""}, newLogEntries, youngestInCache + 1, latestRevision, true, true);
                                if (newLogEntries.size() > 0) {
                                    rebuild = true;
                                    for (SVNLogEntry newLogEntry : newLogEntries) {
                                        if (newLogEntry.getRevision() != repositoryLogData.getYoungestRevision() + 1) {
                                            throw new Exception("版本号不连续");
                                        }
                                        repositoryLogData.getLogEntries().push(newLogEntry);
                                        repositoryLogData.setLastChangeTime(new Date().getTime());
                                    }
                                    repositoryLogData.save();
                                }
                            }
                            if (rebuild) {
                                Platform.runLater(() -> {
                                    getWindow().call("createLogTree");
                                    getWindow().call("setLogCacheRefreshingTime", repositoryLogData.getLastChangeTimeString());
                                });
                            }
                        } catch (Exception e) {
                            Platform.runLater(() -> AlertUtil.error("仓库历史记录加载失败", e));
                        } finally {
                            Platform.runLater(JavaApi.this::serviceCleanup);
                        }
                        return null;
                    }
                };
            }
        }

        /**
         * 公共方法 - 关闭仓库
         */
        public void closeRepository() {
            mainApp.showWelcome();
        }

        /**
         * 公共方法 - 主页 - 查询
         */
        public void loadRepositoryContent() {
            startExclusiveService(buildNonInteractiveService(
                    new LoadRepositoryContentService(), "仓库加载失败"));
        }

        public Object[] getPathNodeArray() {
            return repositoryContentData.getPathNodeArray();
        }

        public Object[] getEntryArray() {
            return repositoryContentData.getEntryArray();
        }

        public void sortEntryList(String sortKey, String direction) {
            direction = "up".equals(direction) ? "up" : "down";
            Comparator<RepositoryDirEntry> comparator;
            switch (sortKey) {
                case "mtime":
                    comparator = "up".equals(direction) ?
                            RepositoryDirEntry::entryMtimeCompare : RepositoryDirEntry::entryMtimeCompareRev;
                    break;
                case "type":
                    comparator = "up".equals(direction) ?
                            RepositoryDirEntry::entryTypeCompare : RepositoryDirEntry::entryTypeCompareRev;
                    break;
                case "size":
                    comparator = "up".equals(direction) ?
                            RepositoryDirEntry::entrySizeCompare : RepositoryDirEntry::entrySizeCompareRev;
                    break;
                case "name":
                default:
                    sortKey = "name";
                    comparator = "up".equals(direction) ?
                            RepositoryDirEntry::entryNameCompare : RepositoryDirEntry::entryNameCompareRev;
            }
            repositoryContentData.entryList.sort(comparator);
            getWindow().call("fillRepoContentTable");
            getWindow().call("showSortIcon", sortKey, direction);
        }

        public boolean hasPrevious() {
            return path.hasPrevious();
        }

        public boolean hasNext() {
            return path.hasNext();
        }

        public boolean hasParent() {
            return path.hasParent();
        }

        public void goPrevious() {
            if (path.goPrevious()) {
                getWindow().call("loadRepoContent");
            }
        }

        public void goNext() {
            if (path.goNext()) {
                getWindow().call("loadRepoContent");
            }
        }

        public void goParent() {
            if (path.goParent()) {
                getWindow().call("loadRepoContent");
            }
        }

        public void goPath(String pathString) {
            if (path.goPath(pathString)) {
                getWindow().call("loadRepoContent");
            }
        }

        /**
         * 公共方法 - 主页 - 上传
         */
        public void uploadDir() {
            File dir = mainApp.chooseDirectory();
            if (dir != null) {
                List<File> dirs = new LinkedList<>();
                List<File> files = new LinkedList<>();
                Map<File, String> uploadPathMap = new HashMap<>();

                mainApp.showProgress(-1, "收集上传项目");
                collectUploadItems(dir, path.resolve(dir.getName()).getPathNode(), dirs, files, uploadPathMap);
                mainApp.hideProgress();

                if (!dirs.isEmpty()) {
                    upload(dirs, files, uploadPathMap);
                }
            }
        }

        public void uploadFiles() {
            List<File> files = mainApp.chooseMultipleFiles();
            if (!files.isEmpty()) {
                mainApp.showProgress(-1, "收集上传项目");
                Map<File, String> uploadPathMap = files.stream()
                        .collect(Collectors.toMap(Function.identity(), file -> path.resolve(file.getName()).toString()));
                mainApp.hideProgress();

                upload(null, files, uploadPathMap);
            }
        }

        private void collectUploadItems(File item, RepositoryPathNode itemPathNode,
                                        List<File> dirs, List<File> files, Map<File, String> uploadPathMap) {
            if (item.exists()) {
                if (item.isFile()) {
                    files.add(item);
                    uploadPathMap.put(item, itemPathNode.toString());
                } else if (item.isDirectory()) {
                    dirs.add(item);
                    uploadPathMap.put(item, itemPathNode.toString());
                    File[] children = item.listFiles();
                    children = children != null ? children : new File[0];
                    Arrays.asList(children).forEach(
                            child -> collectUploadItems(child, itemPathNode.resolve(child.getName()),
                                    dirs, files, uploadPathMap));
                }
            }
        }

        private void checkUploadItems(List<File> dirs, List<File> files, String errorMsg, Task<Void> task) throws Exception {
            for (File dir : dirs) {
                if (task.isCancelled()) {
                    throw new UploadCancelledException();
                }
                if (!dir.isDirectory()) {
                    throw new Exception(errorMsg + "（文件夹不存在）：" + dir.getCanonicalPath());
                }
                if (!dir.canRead()) {
                    throw new Exception(errorMsg + "（文件夹不可读）：" + dir.getCanonicalPath());
                }
            }
            for (File file : files) {
                if (task.isCancelled()) {
                    throw new UploadCancelledException();
                }
                if (!file.isFile()) {
                    throw new Exception(errorMsg + "（文件不存在）：" + file.getCanonicalPath());
                }
                if (!file.canRead()) {
                    throw new Exception(errorMsg + "（文件不可读）：" + file.getCanonicalPath());
                }
            }
        }

        private void upload(List<File> dirs, List<File> files, Map<File, String> uploadPathMap) {
            if (uploadTransactionData == null) {
                String errorMsg = "上传失败";
                String progressTitle = "上传进度";
                String uploadPreCheckProgressText = "上传准备中";
                String dirUploadProgressText = "上传文件夹";
                String dirUploadProgressTextTpl = dirUploadProgressText + "（%d/%d）：%s";
                String fileUploadProgressText = "上传文件";
                String fileUploadProgressTextTpl = "[%6s] [%s / %s] 总进度：%d / %d \t| 剩余时间：%s";
                String fileUploadSubProgressTextTpl = "[%6s] [%s / %s] 正在上传：%s";
                String uploadCompleteProgressText = "上传完成";
                startExclusiveService(buildNonInteractiveService(new EditingWithRefreshingService("上传", errorMsg) {
                    @Override
                    protected void beforeEditing(Task<Void> task) throws Exception {
                        Platform.runLater(() -> {
                            mainApp.showProgress(-1, uploadPreCheckProgressText);
                            mainApp.setOnProgressCloseRequest(event -> cancelExclusiveService());
                        });
                        uploadTransactionData = new UploadTransactionData(repository, dirs, files, uploadPathMap, task);
                        checkUploadItems(uploadTransactionData.dirList(), uploadTransactionData.fileList(), errorMsg, task);
                    }

                    @Override
                    protected void doEditing(ISVNEditor editor, Task<Void> task) throws Exception {
                        if (uploadTransactionData.lengthOfDirs() > 0) {
                            /*准备上传文件夹*/
                            Platform.runLater(() -> {
                                mainApp.showProgress(0, dirUploadProgressText);
                                mainApp.setProgressTitle(progressTitle);
                                mainApp.setOnProgressCloseRequest(event -> cancelExclusiveService());
                            });

                            /*上传文件夹*/
                            for (File dir : uploadTransactionData.dirList()) {
                                if (uploadTransactionData.getKind(dir) != SVNNodeKind.DIR) {
                                    if (task.isCancelled()) {
                                        throw new UploadCancelledException();
                                    }
                                    editor.addDir(uploadPathMap.get(dir), null, -1);
                                    editor.closeDir();
                                }
                                updateProgress(dir);
                            }
                        }

                        if (uploadTransactionData.lengthOfFiles() > 0) {
                            /*准备上传文件*/
                            Platform.runLater(() -> {
                                mainApp.showProgress(0, fileUploadProgressText, 0, fileUploadProgressText);
                                mainApp.setProgressTitle(progressTitle);
                                mainApp.setOnProgressCloseRequest(event -> cancelExclusiveService());
                            });

                            /*上传文件*/
                            for (File file : uploadTransactionData.fileList()) {
                                long sent = 0;
                                updateProgress(file, sent);

                                String uploadFilePath = uploadPathMap.get(file);
                                if (uploadTransactionData.getKind(file) == SVNNodeKind.FILE) {
                                    editor.openFile(uploadFilePath, -1);
                                } else {
                                    editor.addFile(uploadFilePath, null, -1);
                                }
                                editor.applyTextDelta(uploadFilePath, null);
                                SVNDeltaGenerator deltaGenerator = new SVNDeltaGenerator();
                                String checkSum;
                                try (FileInputStream fileInputStream = new FileInputStream(file)) {
                                    byte[] targetBuffer = new byte[64 * 1024];
                                    MessageDigest digest = null;
                                    try {
                                        digest = MessageDigest.getInstance("MD5");
                                    } catch (NoSuchAlgorithmException e) {
                                        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR,
                                                "MD5 implementation not found: {0}", e.getLocalizedMessage());
                                        SVNErrorManager.error(err, e, SVNLogType.DEFAULT);
                                    }
                                    boolean windowSent = false;
                                    while (true) {
                                        if (task.isCancelled()) {
                                            throw new UploadCancelledException();
                                        }
                                        int targetLength = fileInputStream.read(targetBuffer);
                                        if (targetLength <= 0) {
                                            if (!windowSent) {
                                                editor.textDeltaChunk(uploadFilePath, SVNDiffWindow.EMPTY);
                                            }
                                            break;
                                        }
                                        if (digest != null) {
                                            digest.update(targetBuffer, 0, targetLength);
                                        }
                                        deltaGenerator.sendDelta(uploadFilePath, targetBuffer, targetLength, editor);
                                        windowSent = true;
                                        sent += targetLength;
                                        updateProgress(file, sent);
                                    }
                                    editor.textDeltaEnd(uploadFilePath);
                                    checkSum = SVNFileUtil.toHexDigest(digest);
                                }
                                editor.closeFile(uploadFilePath, checkSum);

                                updateProgress(file, sent);
                            }
                        }

                        /*上传完成*/
                        Platform.runLater(() -> mainApp.setProgress(1, uploadCompleteProgressText, 1, uploadCompleteProgressText));
                    }

                    private void updateProgress(File dir) {
                        int dirIdx = uploadTransactionData.indexOfDir(dir);
                        int lengthOfDirs = uploadTransactionData.lengthOfDirs();
                        double progressValue = 1. * (dirIdx + 1) / lengthOfDirs;
                        String dirName = dir.getName();
                        Platform.runLater(() -> mainApp.setProgress(
                                progressValue, String.format(dirUploadProgressTextTpl,
                                        dirIdx + 1, lengthOfDirs, dirName)));
                    }

                    private void updateProgress(File file, long sent) {
                        long totalSent = uploadTransactionData.getPrevSize(file) + sent;
                        long totalSize = Math.max(uploadTransactionData.getTotalSize(), 1);
                        double progressValue = 1. * totalSent / totalSize;
                        String progressPercent = String.format("%.1f%%", 100 * progressValue);
                        String totalSentString = FileUtil.getSizeString(totalSent, 0);
                        String totalSizeString = FileUtil.getSizeString(totalSize, 0);
                        int fileIdx = uploadTransactionData.indexOfFile(file);
                        int lengthOfFiles = uploadTransactionData.lengthOfFiles();
                        String remainingTimeString = uploadTransactionData.getRemainingTimeString(totalSent);

                        long fileSize = Math.max(uploadTransactionData.getSize(file), 1);
                        double subProgressValue = 1. * sent / fileSize;
                        String subProgressPercent = String.format("%.1f%%", 100 * subProgressValue);
                        String sentString = FileUtil.getSizeString(sent, 0);
                        String fileSizeString = FileUtil.getSizeString(fileSize, 0);
                        String fileName = file.getName();
                        Platform.runLater(() -> mainApp.setProgress(
                                progressValue, String.format(fileUploadProgressTextTpl,
                                        progressPercent, totalSentString, totalSizeString,
                                        fileIdx + 1, lengthOfFiles, remainingTimeString),
                                subProgressValue, String.format(fileUploadSubProgressTextTpl,
                                        subProgressPercent, sentString, fileSizeString, fileName)));
                    }
                }, errorMsg));
            }
        }

        /**
         * 公共方法 - 主页 - 新建文件夹
         */
        public void createDir(String name) {
            String errorMsg = "文件夹新建失败";
            startExclusiveService(buildNonInteractiveService(
                    new EditingWithRefreshingService("新建文件夹", errorMsg) {
                        @Override
                        protected void doEditing(ISVNEditor editor, Task<Void> task) throws Exception {
                            editor.addDir(path.resolve(name).toString(), null, -1);
                            editor.closeDir();
                        }
                    }, errorMsg));
        }

        /**
         * 公共方法 - 主页 - 下载
         */
        public void downloadEntry(JSObject pathArray, int length) {
            LinkedList<String> pathList = convertJSStringArray(pathArray, length);
            startExclusiveService(buildNonInteractiveService(
                    new LaunchDownloadTaskService(pathList), "下载任务添加失败"));
        }

        private void collectDownloadTasks(RepositoryPathNode pathNode, RepositoryPathNode parentPathNode,
                                          List<DownloadTask> downloadTasks) throws SVNException {
            SVNDirEntry entry = repository.info(pathNode.toString(), -1);
            if (entry.getKind() == SVNNodeKind.FILE) {
                downloadTasks.add(new DownloadTask(pathNode.toString(), entry.getRevision(), parentPathNode));
            } else if (entry.getKind() == SVNNodeKind.DIR) {
                ArrayList<SVNDirEntry> dirEntries = new ArrayList<>();
                repository.getDir(pathNode.toString(), -1, null, dirEntries);
                for (SVNDirEntry dirEntry : dirEntries) {
                    collectDownloadTasks(pathNode.resolve(dirEntry.getName()), parentPathNode, downloadTasks);
                }
            }
        }

        /**
         * 公共方法 - 主页 - 删除
         */
        public void deleteEntry(JSObject pathArray, int length) {
            LinkedList<String> pathList = convertJSStringArray(pathArray, length);
            String errorMsg = "删除失败";
            startExclusiveService(buildNonInteractiveService(
                    new EditingWithRefreshingService("删除", errorMsg) {
                        @Override
                        protected void doEditing(ISVNEditor editor, Task<Void> task) throws Exception {
                            for (String p : pathList) {
                                editor.deleteEntry(path.resolve(p).toString(), -1);
                            }
                        }
                    }, errorMsg));
        }

        /**
         * 公共方法 - 历史记录
         */
        public void loadRepositoryLog(boolean rebuild) {
            startExclusiveService(buildNonInteractiveService(
                    new LoadRepositoryLogService(rebuild), "仓库历史记录加载失败"));
        }

        public Object[] getLogTreeNodeArray() {
            return repositoryLogData.buildLogTreeNodeArray();
        }
    }

    public SVNRepository getRepository() {
        return repository;
    }

    public void setRepository(SVNRepository repository) {
        this.repository = repository;
    }
}
