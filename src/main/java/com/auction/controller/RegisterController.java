package com.auction.controller;

import com.auction.app.AppContext;
import com.auction.exception.AuctionException;
import com.auction.model.user.Role;
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

    @FXML
    public void initialize() {
        roleComboBox.getItems().setAll(Role.BIDDER, Role.SELLER);
        roleComboBox.getSelectionModel().select(Role.BIDDER);
        messageLabel.setText("");
    }

    @FXML
    private void handleRegister() {
        try {
            appContext.getAuthService().register(
                    usernameField.getText(),
                    passwordField.getText(),
                    fullNameField.getText(),
                    emailField.getText(),
                    roleComboBox.getValue()
            );

            messageLabel.getStyleClass().setAll("success-label");
            messageLabel.setText("Đăng ký thành công. Tài khoản cần Admin duyệt trước khi đăng nhập.");
            passwordField.clear();

        } catch (AuctionException | IllegalArgumentException ex) {
            messageLabel.getStyleClass().setAll("error-label");
            messageLabel.setText(ex.getMessage());
        }
    }

    @FXML
    private void goToLogin() {
        appContext.getNavigator().showLogin();
    }
}