package com.auction.app;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class SceneNavigator {
    private final AppContext appContext;
    private final Stage stage;

    public SceneNavigator(AppContext appContext, Stage stage) {
        this.appContext = appContext;
        this.stage = stage;
    }

    public void showLogin() {
        show("/com/auction/view/login.fxml", true);
    }

    public void showRegister() {
        show("/com/auction/view/register.fxml", true);
    }

    public void showDashboard() {
        show("/com/auction/view/dashboard.fxml", true);
    }

    private void show(String resource, boolean maximized) {
        try {
            URL url = getClass().getResource(resource);
            if (url == null) {
                throw new IllegalStateException("Khong tim thay FXML: " + resource);
            }

            FXMLLoader loader = new FXMLLoader(url);
            loader.setControllerFactory(type -> ControllerFactory.create(type, appContext));

            Parent root = loader.load();
            Scene scene = new Scene(root);

            URL cssUrl = getClass().getResource("/com/auction/style/app.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }

            stage.setScene(scene);
            stage.show();

            if (maximized) {
                Platform.runLater(() -> {
                    stage.setMaximized(false);
                    stage.setMaximized(true);
                });
            }

        } catch (IOException e) {
            throw new IllegalStateException("Khong tai duoc giao dien: " + resource, e);
        }
    }
}