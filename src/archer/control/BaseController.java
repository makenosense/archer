package archer.control;

import archer.MainApp;

public abstract class BaseController {

    protected MainApp mainApp;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public abstract String getTitle();

    public abstract int getWidth();

    public abstract int getHeight();
}
