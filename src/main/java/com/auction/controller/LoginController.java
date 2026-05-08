package com.auction.controller;

import com.auction.app.AppContext;
import com.auction.exception.AuctionException;
import com.auction.model.user.Role;
import com.auction.model.user.User;
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
        roleComboBox.getItems().setAll(Role.BIDDER, Role.SELLER, Role.ADMIN);
        roleComboBox.getSelectionModel().select(Role.BIDDER);
        messageLabel.setText("");
    }

    @FXML
    private void handleLogin() {
        try {
            User user = appContext.getAuthService().login(
                    usernameField.getText(),
                    passwordField.getText()
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
        roleComboBox.getSelectionModel().select(Role.BIDDER);
        messageLabel.setText("");
    }

    @FXML
    private void goToRegister() {
        appContext.getNavigator().showRegister();
    }

    private void setError(String message) {
        messageLabel.getStyleClass().setAll("error-label");
        messageLabel.setText(message == null ? "Có lỗi xảy ra." : message);
    }
}