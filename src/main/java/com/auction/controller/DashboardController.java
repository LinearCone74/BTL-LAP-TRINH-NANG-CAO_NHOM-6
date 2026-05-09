package com.auction.controller;
import javafx.collections.FXCollections;

import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;

import javafx.scene.control.cell.PropertyValueFactory;

import com.auction.repository.JdbcAuctionRepository;
import com.auction.app.AppContext;
import com.auction.model.auction.AuctionView;
import com.auction.model.user.User;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

public class DashboardController {
    @FXML
    private void handleCloseApp() {
        System.exit(0);
    }
    @FXML
    private TableView<AuctionView> auctionTable;

    @FXML
    private TableColumn<AuctionView, String> colAuctionItem;

    @FXML
    private TableColumn<AuctionView, String> colAuctionSeller;

    @FXML
    private TableColumn<AuctionView, Double> colAuctionCurrentPrice;

    @FXML
    private TableColumn<AuctionView, String> colAuctionStatus;

    @FXML
    private TableColumn<AuctionView, String> colAuctionEndTime;

    private final AppContext appContext;

    @FXML
    private Label welcomeLabel;

    @FXML
    private TabPane managementTabPane;

    public DashboardController(AppContext appContext) {
        this.appContext = appContext;
    }

    @FXML
    public void initialize() {
        openAllTabsForDemo();

        if (auctionTable != null) {
            setupAuctionTable();
            loadAuctionTable();
        }

        User current = appContext.getCurrentUser();

        if (welcomeLabel != null && current != null) {
            welcomeLabel.setText(
                    "Xin chào, " + current.getFullName() + " (" + current.getRole() + ")"
            );
        }
    }

    private void openAllTabsForDemo() {
        if (managementTabPane == null) {
            return;
        }

        for (Tab tab : managementTabPane.getTabs()) {
            tab.setDisable(false);
            tab.setStyle("-fx-opacity: 1;");
        }
    }

    @FXML
    private void handleRefresh() {
        openAllTabsForDemo();
    }

    @FXML
    private void handleLogout() {
        appContext.setCurrentUser(null);
        appContext.getNavigator().showLogin();
    }

    @FXML
    private void handleFilterAuctions() {
        openAllTabsForDemo();
    }

    @FXML
    private void handlePlaceBid() {
        System.out.println("handlePlaceBid clicked");
    }

    @FXML
    private void handleRegisterAutoBid() {
        System.out.println("handleRegisterAutoBid clicked");
    }

    @FXML
    private void handleCreateItem() {
        System.out.println("handleCreateItem clicked");
    }

    @FXML
    private void handleUpdateItem() {
        System.out.println("handleUpdateItem clicked");
    }

    @FXML
    private void handleDeleteItem() {
        System.out.println("handleDeleteItem clicked");
    }

    @FXML
    private void handleApproveUser() {
        System.out.println("handleApproveUser clicked");
    }

    @FXML
    private void handleLockUser() {
        System.out.println("handleLockUser clicked");
    }

    @FXML
    private void handleRemoveAuction() {
        System.out.println("handleRemoveAuction clicked");
    }
    private void setupAuctionTable() {

        colAuctionItem.setCellValueFactory(
                new PropertyValueFactory<>("title"));

        colAuctionSeller.setCellValueFactory(
                new PropertyValueFactory<>("sellerName"));

        colAuctionCurrentPrice.setCellValueFactory(
                new PropertyValueFactory<>("currentPrice"));

        colAuctionStatus.setCellValueFactory(
                new PropertyValueFactory<>("status"));

        colAuctionEndTime.setCellValueFactory(
                new PropertyValueFactory<>("endTime"));
    }

    private void loadAuctionTable() {

        JdbcAuctionRepository repo =
                new JdbcAuctionRepository();

        auctionTable.setItems(
                FXCollections.observableArrayList(
                        repo.findAll()
                )
        );
    }
}