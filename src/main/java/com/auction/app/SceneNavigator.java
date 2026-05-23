package com.auction.app;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.animation.FadeTransition;
import javafx.util.Duration;
import javafx.animation.TranslateTransition;
import javafx.util.Duration;
import javafx.scene.layout.StackPane;

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

            root.setOpacity(0);

            FadeTransition ft = new FadeTransition(Duration.millis(120), root);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();

            Scene scene = stage.getScene();

            if (scene == null) {
                scene = new Scene(root);
                stage.setScene(scene);
            } else {
                slideTransition(root, true);
            }

            URL cssUrl = getClass().getResource("/com/auction/style/app.css");
            if (cssUrl != null && !scene.getStylesheets().contains(cssUrl.toExternalForm())) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }

            // ❌ XÓA hoàn toàn Platform.runLater maximize hack
            stage.show();

            if (maximized) {
                stage.setMaximized(true);
            }

        } catch (IOException e) {
            throw new IllegalStateException("Khong tai duoc giao dien: " + resource, e);
        }
    }

    private void slideTransition(Parent newRoot, boolean forward) {

        Scene scene = stage.getScene();

        if (scene == null) {
            scene = new Scene(newRoot);
            stage.setScene(scene);
            return;
        }

        Parent oldRoot = scene.getRoot();

        double width = stage.getWidth() > 0 ? stage.getWidth() : 1100;

        StackPane container = new StackPane(oldRoot, newRoot);
        scene.setRoot(container);

        // old root out
        TranslateTransition oldSlide = new TranslateTransition(Duration.millis(250), oldRoot);
        oldSlide.setToX(forward ? -width : width);

        // new root in
        newRoot.setTranslateX(forward ? width : -width);
        TranslateTransition newSlide = new TranslateTransition(Duration.millis(250), newRoot);
        newSlide.setToX(0);

        oldSlide.play();
        newSlide.play();

        oldSlide.setOnFinished(e -> {
            container.getChildren().remove(oldRoot);
        });
    }
}