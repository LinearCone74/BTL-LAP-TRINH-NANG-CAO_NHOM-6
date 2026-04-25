package com.auction.controller;

import com.auction.app.AppContext;
import com.auction.model.user.Role;
import com.auction.model.user.User;
import com.auction.util.PasswordHasher;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {
    private final AppContext appContext;

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<Role> roleComboBox;
    @FXML private Label messageLabel;

    public LoginController(AppContext appContext) {
        this.appContext = appContext;
    }

    @FXML
    public void initialize() {
        roleComboBox.setItems(FXCollections.observableArrayList(Role.values()));
    }

    @FXML
    private void handleLogin() {
        try {
            User user = appContext.getAuthService().login(usernameField.getText(), PasswordHasher.hash(passwordField.getText()));
            if (roleComboBox.getValue() != null && user.getRole() != roleComboBox.getValue()) {
                messageLabel.setText("Vai trò không khớp với tài khoản.");
                return;
            }
            appContext.setCurrentUser(user);
            appContext.getNavigator().showDashboard();
        } catch (Exception ex) {
            messageLabel.setText(ex.getMessage());
        }
    }

    @FXML private void handleClear() { usernameField.clear(); passwordField.clear(); roleComboBox.getSelectionModel().clearSelection(); messageLabel.setText(""); }
    @FXML private void goToRegister() { appContext.getNavigator().showRegister(); }
}