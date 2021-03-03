package archer;

import archer.control.BaseController;
import archer.control.DoubleProgressController;
import archer.control.InterfaceController;
import archer.control.ProgressController;
import archer.model.RepositoryConfig;
import com.sun.javafx.webkit.WebConsoleListener;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.io.SVNRepository;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.LinkedList;
import java.util.List;

public class MainApp extends Application {
    public static final String APP_NAME = "Archer";

    private TrayIcon trayIcon;
    private Stage primaryStage;
    private Stage progressStage;
    private ProgressController progressController;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            Platform.setImplicitExit(false);
            initSystemTray();
        }
        progressStage = new Stage();
        progressStage.setResizable(false);
        progressStage.initOwner(primaryStage);
        progressStage.initModality(Modality.WINDOW_MODAL);
        WebConsoleListener.setDefaultListener((webView, message, lineNumber, sourceId) -> System.out.println(message));
        SVNRepositoryFactoryImpl.setup();
        showWelcome();
    }

    private void initSystemTray() {
        if (SystemTray.isSupported()) {
            PopupMenu popup = new PopupMenu();
            MenuItem exitItem = new MenuItem("退出");
            exitItem.addActionListener(e -> exit());
            popup.add(exitItem);

            ImageIcon imageIcon = new ImageIcon(getClass().getResource("view/html/img/logo.png"));
            trayIcon = new TrayIcon(imageIcon.getImage());
            trayIcon.setImageAutoSize(true);
            trayIcon.setToolTip(APP_NAME);
            trayIcon.setPopupMenu(popup);
            trayIcon.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        if (primaryStage.isShowing()) {
                            Platform.runLater(() -> primaryStage.hide());
                        } else {
                            Platform.runLater(() -> primaryStage.show());
                        }
                    }
                }
            });

            try {
                SystemTray.getSystemTray().add(trayIcon);
            } catch (AWTException e) {
                e.printStackTrace();
            }
        }
    }

    public void showWelcome() {
        initAndShowStage(primaryStage, "view/fxml/Welcome.fxml");
    }

    public void showInterface(RepositoryConfig repositoryConfig, SVNRepository repository) {
        initAndShowStage(primaryStage, "view/fxml/Interface.fxml");
        ((InterfaceController) primaryStage.getScene().getUserData()).setRepository(repository);
        primaryStage.setTitle(APP_NAME + " - " + repositoryConfig.getTitle());
    }

    public File chooseDirectory() {
        return chooseDirectory("请选择文件夹");
    }

    public File chooseDirectory(String title) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(title);
        return directoryChooser.showDialog(primaryStage);
    }

    public List<File> chooseMultipleFiles() {
        return chooseMultipleFiles("请选择文件（多选）");
    }

    public List<File> chooseMultipleFiles(String title) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        List<File> files = fileChooser.showOpenMultipleDialog(primaryStage);
        return files != null ? files : new LinkedList<>();
    }

    public void showProgress(double value, String text) {
        initAndShowStage(progressStage, "view/fxml/Progress.fxml");
        progressController = (ProgressController) progressStage.getScene().getUserData();
        progressController.setProgress(value);
        progressController.setText(text);
    }

    public void showProgress(double value, String text, double subValue, String subText) {
        initAndShowStage(progressStage, "view/fxml/DoubleProgress.fxml");
        progressController = (ProgressController) progressStage.getScene().getUserData();
        progressController.setProgress(value);
        progressController.setText(text);
        ((DoubleProgressController) progressController).setSubProgress(subValue);
        ((DoubleProgressController) progressController).setSubText(subText);
    }

    public void setProgress(double value, String text) {
        progressController.setProgress(value);
        progressController.setText(text);
    }

    public void setProgress(double value, String text, double subValue, String subText) {
        setProgress(value, text);
        ((DoubleProgressController) progressController).setSubProgress(subValue);
        ((DoubleProgressController) progressController).setSubText(subText);
    }

    public void setProgressTitle(String title) {
        progressStage.setTitle(title);
    }

    public void hideProgress() {
        progressStage.hide();
    }

    private void initAndShowStage(Stage stage, String fxmlPath) {
        try {
            stage.hide();
            stage.getIcons().clear();
            stage.getIcons().add(new Image(getClass().getResource("view/html/img/logo.png").toExternalForm()));
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent parent = loader.load();
            BaseController controller = loader.getController();
            controller.setMainApp(this);
            stage.setTitle(controller.getTitle());
            stage.setWidth(controller.getWidth());
            stage.setHeight(controller.getHeight());
            stage.setMinWidth(controller.getWidth());
            stage.setMinHeight(controller.getHeight());
            Scene scene = new Scene(parent, controller.getWidth(), controller.getHeight());
            scene.setUserData(controller);
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void exit() {
        System.exit(0);
    }
}
