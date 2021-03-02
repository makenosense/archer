package archer.control;

import archer.util.AlertUtil;
import javafx.concurrent.Service;

public abstract class BaseJavaApi {

    private Service service;

    protected abstract class ExclusiveService {

        protected abstract Service createService() throws Exception;

        protected abstract void onCreationFailed(Exception e);

        public void start() {
            try {
                if (service != null && service.isRunning()) {
                    service.cancel();
                }
                service = createService();
                service.start();
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
