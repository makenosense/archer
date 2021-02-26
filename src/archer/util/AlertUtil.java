package archer.util;

import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.io.PrintWriter;
import java.io.StringWriter;

public class AlertUtil {

    public static void error(String contentText) {
        error(null, contentText);
    }

    public static void error(String headerText, String contentText) {
        error("错误", headerText, contentText);
    }

    public static void error(String title, String headerText, String contentText) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);
        alert.showAndWait();
    }

    public static void error(String contentText, Exception e) {
        error(null, contentText, e);
    }

    public static void error(String headerText, String contentText, Exception e) {
        error("错误", headerText, contentText, e);
    }

    public static void error(String title, String headerText, String contentText, Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        e.printStackTrace(printWriter);
        String errorString = stringWriter.toString();

        TextArea textArea = new TextArea(errorString);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        textArea.setMaxHeight(Double.MAX_VALUE);
        textArea.setMaxWidth(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane errorContent = new GridPane();
        errorContent.setMaxWidth(Double.MAX_VALUE);
        errorContent.add(textArea, 0, 0);

        alert.getDialogPane().setExpandableContent(errorContent);

        alert.showAndWait();
    }

    public static void warn(String contentText) {
        warn(null, contentText);
    }

    public static void warn(String headerText, String contentText) {
        warn("警告", headerText, contentText);
    }

    public static void warn(String title, String headerText, String contentText) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);
        alert.showAndWait();
    }

    public static void info(String contentText) {
        info(null, contentText);
    }

    public static void info(String headerText, String contentText) {
        info("信息", headerText, contentText);
    }

    public static void info(String title, String headerText, String contentText) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);
        alert.showAndWait();
    }
}
