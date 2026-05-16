package com.auction.controller;

import com.auction.app.AppContext;
import com.auction.exception.AuctionException;
import com.auction.model.user.Role;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegisterController {

    private final AppContext appContext;

    @FXML
    private TextField usernameField;

    @FXML
    private TextField fullNameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField visiblePasswordField;

    @FXML
    private Button togglePasswordButton;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private TextField visibleConfirmPasswordField;

    @FXML
    private Button toggleConfirmPasswordButton;

    @FXML
    private ComboBox<Role> roleComboBox;

    @FXML
    private Label messageLabel;

    private boolean passwordVisible = false;
    private boolean confirmPasswordVisible = false;

    public RegisterController(AppContext appContext) {
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

        if (visibleConfirmPasswordField != null) {
            visibleConfirmPasswordField.setVisible(false);
            visibleConfirmPasswordField.setManaged(false);
        }

        if (togglePasswordButton != null) {
            togglePasswordButton.setText("👁");
        }

        if (toggleConfirmPasswordButton != null) {
            toggleConfirmPasswordButton.setText("👁");
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
    private void handleToggleConfirmPassword() {
        confirmPasswordVisible = !confirmPasswordVisible;

        if (confirmPasswordVisible) {
            visibleConfirmPasswordField.setText(confirmPasswordField.getText());

            visibleConfirmPasswordField.setVisible(true);
            visibleConfirmPasswordField.setManaged(true);

            confirmPasswordField.setVisible(false);
            confirmPasswordField.setManaged(false);

            toggleConfirmPasswordButton.setText("🙈");
        } else {
            confirmPasswordField.setText(visibleConfirmPasswordField.getText());

            confirmPasswordField.setVisible(true);
            confirmPasswordField.setManaged(true);

            visibleConfirmPasswordField.setVisible(false);
            visibleConfirmPasswordField.setManaged(false);

            toggleConfirmPasswordButton.setText("👁");
        }
    }

    @FXML
    private void handleRegister() {
        try {
            String username = usernameField.getText();
            String fullName = fullNameField.getText();
            String email = emailField.getText();

            String password = passwordVisible
                    ? visiblePasswordField.getText()
                    : passwordField.getText();

            String confirmPassword = confirmPasswordVisible
                    ? visibleConfirmPasswordField.getText()
                    : confirmPasswordField.getText();

            Role role = roleComboBox.getValue();

            if (username == null || username.isBlank()) {
                setError("Vui lòng nhập tên đăng nhập.");
                return;
            }

            if (fullName == null || fullName.isBlank()) {
                setError("Vui lòng nhập họ và tên.");
                return;
            }

            if (email == null || email.isBlank()) {
                setError("Vui lòng nhập email.");
                return;
            }

            if (password == null || password.isBlank()) {
                setError("Vui lòng nhập mật khẩu.");
                return;
            }

            if (!password.equals(confirmPassword)) {
                setError("Mật khẩu xác nhận không khớp.");
                return;
            }

            appContext.getAuthService().register(
                    username,
                    password,
                    fullName,
                    email,
                    role
            );

            messageLabel.getStyleClass().setAll("success-label");
            messageLabel.setText("Đăng ký thành công! Vui lòng đăng nhập.");

        } catch (AuctionException | IllegalArgumentException ex) {
            setError(ex.getMessage());
        }
    }

    @FXML
    private void goToLogin() {
        appContext.getNavigator().showLogin();
    }

    private void setError(String message) {
        if (messageLabel == null) {
            return;
        }

        messageLabel.getStyleClass().setAll("error-label");
        messageLabel.setText(message == null ? "Có lỗi xảy ra." : message);
    }
}