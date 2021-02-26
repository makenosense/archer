package archer.control;

import archer.util.AlertUtil;

public abstract class BaseJavaApi {

    public void error(String msg) {
        AlertUtil.error(msg);
    }

    public void warn(String msg) {
        AlertUtil.warn(msg);
    }

    public void info(String msg) {
        AlertUtil.info(msg);
    }
}
