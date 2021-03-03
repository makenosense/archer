package archer.control;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

public class DoubleProgressController extends ProgressController {
    private static final String TITLE = "进度";
    private static final int WIDTH = 500;
    private static final int HEIGHT = 180;

    @FXML
    private ProgressBar subProgressBar;

    @FXML
    private Label subLabel;

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

    public void setSubProgress(double value) {
        subProgressBar.setProgress(value);
    }

    public void setSubText(String text) {
        subLabel.setText(text);
    }
}
