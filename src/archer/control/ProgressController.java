package archer.control;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

public class ProgressController extends BaseController {
    private static final String TITLE = "进度";
    private static final int WIDTH = 500;
    private static final int HEIGHT = 120;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Label label;

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

    public void setProgress(double value) {
        progressBar.setProgress(value);
    }

    public void setText(String text) {
        label.setText(text);
    }
}
