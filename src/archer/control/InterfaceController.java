package archer.control;

import archer.MainApp;
import archer.model.RepositoryContentData;
import archer.model.RepositoryDirEntry;
import archer.model.RepositoryPath;
import archer.model.UploadTransactionData;
import archer.util.AlertUtil;
import archer.util.FileUtil;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.util.Pair;
import netscape.javascript.JSObject;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.diff.SVNDeltaGenerator;

import java.io.File;
import java.io.FileInputStream;
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
        private RepositoryContentData repositoryContentData = new RepositoryContentData();
        private UploadTransactionData uploadTransactionData;

        /**
         * 私有方法
         */
        private void enableRefreshingRepositoryContent() {
            getWindow().call("switchRepoNavOps", "repo-nav-ops-refresh", true);
        }

        private ExclusiveService buildNonInteractiveService(Service service, String creationFailedMsg) {
            return buildNonInteractiveService(service, creationFailedMsg, null, null, null);
        }

        private ExclusiveService buildNonInteractiveService(Service service, String creationFailedMsg,
                                                            String progressTitle, Pair<Double, String> progress, Pair<Double, String> subProgress) {
            return new ExclusiveService() {
                @Override
                protected Service createService() {
                    webView.setDisable(true);
                    if (progress != null) {
                        if (subProgress != null) {
                            mainApp.showProgress(progress, subProgress);
                        } else {
                            mainApp.showProgress(progress);
                        }
                        if (progressTitle != null) {
                            mainApp.setProgressTitle(progressTitle);
                        }
                    }
                    return service;
                }

                @Override
                protected void onCreationFailed(Exception e) {
                    AlertUtil.error(creationFailedMsg, e);
                    uploadTransactionData = null;
                    mainApp.hideProgress();
                    webView.setDisable(false);
                    enableRefreshingRepositoryContent();
                }
            };
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
                            repositoryContentData = new RepositoryContentData();
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
                            Platform.runLater(() -> {
                                webView.setDisable(false);
                                enableRefreshingRepositoryContent();
                            });
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

            protected abstract void doEditing(ISVNEditor editor) throws Exception;

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
                            editor = repository.getCommitEditor(logMessage, null);
                            editor.openRoot(-1);
                            doEditing(editor);
                            editor.closeDir();
                            editor.closeEdit();
                            onEditingSuccess();
                        } catch (Exception e) {
                            if (editor != null) {
                                editor.abortEdit();
                            }
                            onEditingFailed(e);
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
                    uploadTransactionData = null;
                    mainApp.hideProgress();
                    webView.setDisable(false);
                    enableRefreshingRepositoryContent();
                    getWindow().call("loadRepoContent");
                });
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
         * 公共方法 - 主页 - 编辑
         */
        public void uploadDir() {
            File dir = mainApp.chooseDirectory();
            //TODO
        }

        public void uploadFiles() {
            List<File> files = mainApp.chooseMultipleFiles().stream()
                    .filter(file -> !file.isHidden())
                    .collect(Collectors.toList());
            if (!files.isEmpty()) {
                Map<File, String> repositoryPathMap = files.stream()
                        .collect(Collectors.toMap(Function.identity(), file -> path.resolve(file.getName()).toString()));
                upload(null, files, repositoryPathMap);
            }
        }

        private void checkUploadItems(List<File> dirs, List<File> files, String errorMsg) throws Exception {
            for (File dir : dirs) {
                if (!dir.isDirectory()) {
                    throw new Exception(errorMsg + "（文件夹不存在）：" + dir.getCanonicalPath());
                }
                if (!dir.canRead()) {
                    throw new Exception(errorMsg + "（文件夹不可读）：" + dir.getCanonicalPath());
                }
            }
            for (File file : files) {
                if (!file.isFile()) {
                    throw new Exception(errorMsg + "（文件不存在）：" + file.getCanonicalPath());
                }
                if (!file.canRead()) {
                    throw new Exception(errorMsg + "（文件不可读）：" + file.getCanonicalPath());
                }
            }
        }

        private void upload(List<File> dirs, List<File> files, Map<File, String> repositoryPathMap) {
            if (uploadTransactionData == null) {
                String errorMsg = "上传失败";
                String progressTextTpl = "[%s] 正在上传（%d/%d）：%s";
                String subProgressTextTpl = "[%s] 上传进度：%s / %s";
                try {
                    uploadTransactionData = new UploadTransactionData(repository, dirs, files, repositoryPathMap);
                    checkUploadItems(uploadTransactionData.dirList(), uploadTransactionData.fileList(), errorMsg);
                } catch (Exception e) {
                    AlertUtil.error(errorMsg, e);
                    uploadTransactionData = null;
                    return;
                }
                startExclusiveService(buildNonInteractiveService(new EditingWithRefreshingService("uploadFiles", errorMsg) {
                    private void updateProgress(File file, long sent) {
                        int fileIdx = uploadTransactionData.indexOf(file);
                        int uploadLength = uploadTransactionData.length();
                        long totalSent = uploadTransactionData.getPrevSize(file) + sent;
                        long totalSize = uploadTransactionData.getTotalSize();
                        double progressValue = 1. * totalSent / totalSize;
                        String progressPercent = String.format("%.1f%%", 100 * progressValue);
                        String fileName = file.getName();
                        long fileSize = uploadTransactionData.getSize(file);
                        double subProgressValue = 1. * sent / fileSize;
                        String subProgressPercent = String.format("%.1f%%", 100 * subProgressValue);
                        String sentString = FileUtil.getSizeString(sent);
                        String fileSizeString = FileUtil.getSizeString(fileSize);
                        Platform.runLater(() -> mainApp.setProgress(
                                progressValue, String.format(progressTextTpl, progressPercent, fileIdx + 1, uploadLength, fileName),
                                subProgressValue, String.format(subProgressTextTpl, subProgressPercent, sentString, fileSizeString)));
                    }

                    @Override
                    protected void doEditing(ISVNEditor editor) throws Exception {
                        /*上传文件夹*/
                        for (File dir : uploadTransactionData.dirList()) {
                            if (uploadTransactionData.getKind(dir) != SVNNodeKind.DIR) {
                                editor.addDir(repositoryPathMap.get(dir), null, -1);
                                editor.closeDir();
                            }
                        }

                        /*上传文件*/
                        for (File file : uploadTransactionData.fileList()) {
                            long sent = 0;
                            updateProgress(file, sent);

                            String repositoryFilePath = repositoryPathMap.get(file);
                            if (uploadTransactionData.getKind(file) == SVNNodeKind.FILE) {
                                editor.openFile(repositoryFilePath, -1);
                            } else {
                                editor.addFile(repositoryFilePath, null, -1);
                            }
                            editor.applyTextDelta(repositoryFilePath, null);
                            SVNDeltaGenerator deltaGenerator = new SVNDeltaGenerator();
                            String checkSum = deltaGenerator.sendDelta(repositoryFilePath, new FileInputStream(file), editor, true);
                            editor.closeFile(repositoryFilePath, checkSum);

                            updateProgress(file, sent);
                        }

                        /*上传完成*/
                        Platform.runLater(() -> mainApp.setProgress(new Pair<>(1., "上传完成"), new Pair<>(1., "上传完成")));
                    }
                }, errorMsg, "上传进度", new Pair<>(-1., "上传准备中"), new Pair<>(-1., "上传准备中")));
            }
        }

        public void createDir(String name) {
            String errorMsg = "文件夹新建失败";
            startExclusiveService(buildNonInteractiveService(
                    new EditingWithRefreshingService("createDir", errorMsg) {
                        @Override
                        protected void doEditing(ISVNEditor editor) throws Exception {
                            editor.addDir(path.resolve(name).toString(), null, -1);
                            editor.closeDir();
                        }
                    }, errorMsg));
        }

        public void deleteEntry(JSObject pathArray, int length) {
            LinkedList<String> pathList = new LinkedList<>();
            for (int idx = 0; idx < length; ) {
                pathList.add((String) pathArray.getSlot(idx++));
            }
            String errorMsg = "删除失败";
            startExclusiveService(buildNonInteractiveService(
                    new EditingWithRefreshingService("deleteEntry", errorMsg) {
                        @Override
                        protected void doEditing(ISVNEditor editor) throws Exception {
                            for (String p : pathList) {
                                editor.deleteEntry(path.resolve(p).toString(), -1);
                            }
                        }
                    }, errorMsg));
        }
    }

    public SVNRepository getRepository() {
        return repository;
    }

    public void setRepository(SVNRepository repository) {
        this.repository = repository;
    }
}
