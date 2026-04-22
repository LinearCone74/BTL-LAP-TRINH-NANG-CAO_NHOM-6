package com.auction.controller;

import com.auction.app.AppContext;
import com.auction.model.user.Role;
import com.auction.util.PasswordHasher;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegisterController {
    private final AppContext appContext;

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private ComboBox<Role> roleComboBox;
    @FXML private Label messageLabel;

    public RegisterController(AppContext appContext) {
        this.appContext = appContext;
    }

    @FXML public void initialize() { roleComboBox.setItems(FXCollections.observableArrayList(Role.BIDDER, Role.SELLER)); }

    @FXML
    private void handleRegister() {
        try {
            appContext.getAuthService().register(usernameField.getText(), PasswordHasher.hash(passwordField.getText()), fullNameField.getText(), emailField.getText(), roleComboBox.getValue());
            messageLabel.setText("Đăng ký thành công. Hãy đăng nhập để tiếp tục.");
        } catch (Exception ex) {
            messageLabel.setText(ex.getMessage());
        }
    }

    @FXML private void goToLogin() { appContext.getNavigator().showLogin(); }
}