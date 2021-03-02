package archer.control;

import archer.MainApp;
import archer.model.RepositoryContentData;
import archer.model.RepositoryDirEntry;
import archer.model.RepositoryPath;
import archer.util.AlertUtil;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.SVNRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
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

        /**
         * 私有方法
         */
        private void enableRefreshingRepositoryContent() {
            getWindow().call("switchRepoNavOps", "repo-nav-ops-refresh", true);
        }

        private ExclusiveService buildNonInteractiveService(Service service, String creationFailedMsg) {
            return buildNonInteractiveService(service, creationFailedMsg, null, null);
        }

        private ExclusiveService buildNonInteractiveService(Service service, String creationFailedMsg,
                                                            Integer initProgress, String initProgressText) {
            return new ExclusiveService() {
                @Override
                protected Service createService() {
                    webView.setDisable(true);
                    if (initProgress != null && initProgressText != null) {
                        mainApp.showProgress(initProgress, initProgressText);
                    }
                    return service;
                }

                @Override
                protected void onCreationFailed(Exception e) {
                    AlertUtil.error(creationFailedMsg, e);
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
        public void createDir(String name) {
            String errorMsg = "文件夹新建失败";
            startExclusiveService(buildNonInteractiveService(
                    new EditingWithRefreshingService("createDir", errorMsg) {
                        @Override
                        protected void doEditing(ISVNEditor editor) throws Exception {
                            editor.addDir(path.resolve(name).toString(), null, -1);
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
