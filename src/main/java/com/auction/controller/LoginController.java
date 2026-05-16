package com.auction.controller;

import com.auction.app.AppContext;
import com.auction.exception.AuctionException;
import com.auction.model.user.Role;
import com.auction.model.user.User;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    private final AppContext appContext;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField visiblePasswordField;

    @FXML
    private Button togglePasswordButton;

    @FXML
    private ComboBox<Role> roleComboBox;

    @FXML
    private Label messageLabel;

    private boolean passwordVisible = false;

    public LoginController(AppContext appContext) {
        this.appContext = appContext;
    }

    @FXML
    public void initialize() {
        roleComboBox.getItems().setAll(
                Role.BIDDER,
                Role.SELLER,
                Role.ADMIN
        );

        roleComboBox.getSelectionModel().select(Role.BIDDER);

        if (messageLabel != null) {
            messageLabel.setText("");
        }

        if (visiblePasswordField != null) {
            visiblePasswordField.setVisible(false);
            visiblePasswordField.setManaged(false);
        }

        if (passwordField != null) {
            passwordField.setVisible(true);
            passwordField.setManaged(true);
        }

        if (togglePasswordButton != null) {
            togglePasswordButton.setText("👁");
        }
    }

    @FXML
    private void handleTogglePassword() {
        passwordVisible = !passwordVisible;

        if (passwordVisible) {
            visiblePasswordField.setText(passwordField.getText());

            visiblePasswordField.setVisible(true);
            visiblePasswordField.setManaged(true);

            passwordField.setVisible(false);
            passwordField.setManaged(false);

            togglePasswordButton.setText("🙈");
        } else {
            passwordField.setText(visiblePasswordField.getText());

            passwordField.setVisible(true);
            passwordField.setManaged(true);

            visiblePasswordField.setVisible(false);
            visiblePasswordField.setManaged(false);

            togglePasswordButton.setText("👁");
        }
    }

    @FXML
    private void handleLogin() {
        try {
            String username = usernameField.getText();

            String password = passwordVisible
                    ? visiblePasswordField.getText()
                    : passwordField.getText();

            User user = appContext.getAuthService().login(
                    username,
                    password
            );

            Role selectedRole = roleComboBox.getValue();

            if (selectedRole != null && user.getRole() != selectedRole) {
                setError("Tài khoản này thuộc vai trò " + user.getRole()
                        + ", không phải " + selectedRole + ".");
                return;
            }

            appContext.setCurrentUser(user);
            appContext.getNavigator().showDashboard();

        } catch (AuctionException | IllegalArgumentException ex) {
            setError(ex.getMessage());
        }
    }

    @FXML
    private void handleClear() {
        usernameField.clear();

        passwordField.clear();
        visiblePasswordField.clear();

        passwordVisible = false;

        passwordField.setVisible(true);
        passwordField.setManaged(true);

        visiblePasswordField.setVisible(false);
        visiblePasswordField.setManaged(false);

        togglePasswordButton.setText("👁");

        roleComboBox.getSelectionModel().select(Role.BIDDER);

        if (messageLabel != null) {
            messageLabel.setText("");
        }
    }

    @FXML
    private void goToRegister() {
        appContext.getNavigator().showRegister();
    }

    private void setError(String message) {
        if (messageLabel == null) {
            return;
        }

        messageLabel.getStyleClass().setAll("error-label");
        messageLabel.setText(message == null ? "Có lỗi xảy ra." : message);
    }
}