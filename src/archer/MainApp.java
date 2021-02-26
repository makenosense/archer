package archer;

import archer.control.BaseController;
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
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.io.SVNRepository;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

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

    private void showProgress() {
        initAndShowStage(progressStage, "view/fxml/Progress.fxml");
        progressController = (ProgressController) progressStage.getScene().getUserData();
    }

    private void showProgress(double value) {
        showProgress();
        setProgress(value);
    }

    public void showProgress(double value, String text) {
        showProgress(value);
        setProgressText(text);
    }

    private void initAndShowStage(Stage stage, String fxmlPath) {
        try {
            if (stage.isShowing()) {
                stage.hide();
            }
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

    public void setProgressText(String text) {
        progressController.setProgressText(text);
    }

    public void setProgress(double value) {
        progressController.setProgress(value);
    }

    public void setProgress(double value, String text) {
        setProgressText(text);
        setProgress(value);
    }

    public void hideProgress() {
        progressStage.hide();
    }

    public void exit() {
        System.exit(0);
    }
}