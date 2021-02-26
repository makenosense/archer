package archer.control;

import archer.MainApp;
import archer.model.RepositoryDirEntry;
import archer.model.RepositoryPath;
import archer.model.RepositoryPathNode;
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
import org.tmatesoft.svn.core.io.SVNRepository;

import java.util.ArrayList;
import java.util.List;
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

        private Service service;
        private RepositoryContentData repositoryContentData;

        public class RepositoryContentData {
            public List<RepositoryPathNode> pathNodeList = new ArrayList<>();
            public List<RepositoryDirEntry> entryList = new ArrayList<>();
        }

        public class LoadRepositoryContentService extends Service<Void> {

            @Override
            protected Task<Void> createTask() {
                return new Task<Void>() {
                    @Override
                    protected Void call() {
                        try {
                            repositoryContentData = new RepositoryContentData();
                            repositoryContentData.pathNodeList = path.getPathNodeList();
                            SVNNodeKind kind = repository.checkPath(path.toString(), -1);
                            if (kind == SVNNodeKind.DIR) {
                                ArrayList<SVNDirEntry> entryList = new ArrayList<>();
                                repository.getDir(path.toString(), -1, null, entryList);
                                repositoryContentData.entryList = entryList.stream()
                                        .map(RepositoryDirEntry::new)
                                        .collect(Collectors.toList());
                            }
                            Platform.runLater(() -> getWindow().call("fillRepoContent"));
                        } catch (Exception e) {
                            Platform.runLater(() -> AlertUtil.error("仓库加载失败", e));
                        } finally {
                            Platform.runLater(() -> {
                                webView.setDisable(false);
                                enableRefreshRepositoryContent();
                            });
                        }
                        return null;
                    }
                };
            }
        }

        public void loadRepositoryContent() {
            if (service != null && service.isRunning()) {
                service.cancel();
            }
            try {
                webView.setDisable(true);
                service = new LoadRepositoryContentService();
                service.start();
            } catch (Exception e) {
                AlertUtil.error("仓库加载失败", e);
                webView.setDisable(false);
                enableRefreshRepositoryContent();
            }
        }

        private void enableRefreshRepositoryContent() {
            getWindow().call("switchRepoNavOps", "repo-nav-ops-refresh", true);
        }

        public Object[] getPathNodeListAsArray() {
            return repositoryContentData != null ? repositoryContentData.pathNodeList.toArray() : new Object[0];
        }

        public Object[] getEntryListAsArray() {
            return repositoryContentData != null ? repositoryContentData.entryList.toArray() : new Object[0];
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

        public void openDir(String name) {
            if (path.resolve(name)) {
                getWindow().call("loadRepoContent");
            }
        }

        public void closeRepository() {
            mainApp.showWelcome();
        }
    }

    public SVNRepository getRepository() {
        return repository;
    }

    public void setRepository(SVNRepository repository) {
        this.repository = repository;
    }
}