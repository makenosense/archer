package archer.control;

import archer.MainApp;
import archer.model.RepositoryConfig;
import archer.util.AlertUtil;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.tmatesoft.svn.core.io.SVNRepository;

public class WelcomeController extends BaseController {
    private static final String TITLE = "Welcome to " + MainApp.APP_NAME;
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final String INIT_URL = "view/html/welcome.html";

    private final JavaApi javaApi = new JavaApi();

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
                getWindow().call("switchToSidebarTabWithDelay", "sidebar-repo-list");
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

        private void hideProgressWithDelay() throws InterruptedException {
            Thread.sleep(100);
            Platform.runLater(() -> mainApp.hideProgress());
        }

        private void switchToRepositoryList() {
            getWindow().call("switchToSidebarTab", "sidebar-repo-list");
        }

        public class OpenRepositoryService extends Service<Void> {

            private final RepositoryConfig repositoryConfig;
            private final boolean saveBeforeOpen;

            public OpenRepositoryService(RepositoryConfig repositoryConfig, boolean saveBeforeOpen) {
                this.repositoryConfig = repositoryConfig;
                this.saveBeforeOpen = saveBeforeOpen;
            }

            @Override
            protected Task<Void> createTask() {
                return new Task<Void>() {
                    @Override
                    protected Void call() {
                        try {
                            Platform.runLater(() -> mainApp.setProgress(saveBeforeOpen ? 1.0 / 3 : 1.0 / 2, "尝试连接仓库"));
                            SVNRepository repository = repositoryConfig.getRepository();

                            if (saveBeforeOpen) {
                                Platform.runLater(() -> mainApp.setProgress(2.0 / 3, "保存仓库配置"));
                                repositoryConfig.save();
                            }

                            Platform.runLater(() -> mainApp.setProgress(1, "正在打开仓库"));
                            hideProgressWithDelay();
                            Platform.runLater(() -> mainApp.showInterface(repositoryConfig, repository));
                        } catch (Exception e) {
                            Platform.runLater(() -> {
                                AlertUtil.error("出现错误", e);
                                mainApp.hideProgress();
                                switchToRepositoryList();
                            });
                        }
                        return null;
                    }
                };
            }
        }

        public void addRepository(JSObject params) {
            if (service != null && service.isRunning()) {
                service.cancel();
            }
            try {
                mainApp.showProgress(0, "初始化仓库配置");
                RepositoryConfig repositoryConfig = new RepositoryConfig(params);
                service = new OpenRepositoryService(repositoryConfig, true);
                service.start();
            } catch (Exception e) {
                AlertUtil.error("仓库添加失败", e);
                mainApp.hideProgress();
                switchToRepositoryList();
            }
        }

        public Object[] loadRepositoryConfigList() {
            return RepositoryConfig.loadAll().toArray(new RepositoryConfig[0]);
        }

        public void openRepository(int index) {
            if (service != null && service.isRunning()) {
                service.cancel();
            }
            try {
                mainApp.showProgress(0, "加载仓库配置");
                RepositoryConfig repositoryConfig = RepositoryConfig.loadAndMoveFirst(index);
                service = new OpenRepositoryService(repositoryConfig, false);
                service.start();
            } catch (Exception e) {
                AlertUtil.error("仓库打开失败", e);
                mainApp.hideProgress();
                switchToRepositoryList();
            }
        }

        public void removeRepository(int index) {
            try {
                mainApp.showProgress(-1, "正在移除仓库");
                RepositoryConfig.remove(index);
            } catch (Exception e) {
                AlertUtil.error("仓库移除失败", e);
            } finally {
                mainApp.hideProgress();
                switchToRepositoryList();
            }
        }
    }
}
