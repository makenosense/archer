package archer.control;

import archer.util.AlertUtil;
import javafx.application.Platform;
import javafx.concurrent.Service;

public abstract class BaseJavaApi {

    private Service service;

    protected abstract class ExclusiveService {

        protected abstract Service createService() throws Exception;

        protected void onCreationFailed(Exception e) {
            Platform.runLater(() -> AlertUtil.error("出现错误", e));
        }

        public void start() {
            try {
                Service oldService = service;
                service = createService();
                service.start();
                if (oldService != null && oldService.isRunning()) {
                    oldService.cancel();
                }
            } catch (Exception e) {
                onCreationFailed(e);
            }
        }
    }

    protected void startExclusiveService(ExclusiveService exclusiveService) {
        exclusiveService.start();
    }

    public void error(String msg) {
        AlertUtil.error(msg);
    }

    public void warn(String msg) {
        AlertUtil.warn(msg);
    }

    public void info(String msg) {
        AlertUtil.info(msg);
    }

    public boolean confirm(String msg) {
        return AlertUtil.confirm(msg);
    }
}
