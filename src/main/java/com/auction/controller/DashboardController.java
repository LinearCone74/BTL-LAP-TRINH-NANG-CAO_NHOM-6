package com.auction.controller;

import com.auction.app.AppContext;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.*;
import javafx.scene.control.TabPane;

public class DashboardController {

    private final AppContext appContext;

    public DashboardController(AppContext appContext) {
        this.appContext = appContext;
    }

    @FXML private Label welcomeLabel;

    @FXML private TextField searchField;
    @FXML private ChoiceBox<String> statusFilterChoiceBox;
    @FXML private TableView<?> auctionTable;
    @FXML private TableColumn<?, ?> colAuctionItem;
    @FXML private TableColumn<?, ?> colAuctionSeller;
    @FXML private TableColumn<?, ?> colAuctionCurrentPrice;
    @FXML private TableColumn<?, ?> colAuctionStatus;
    @FXML private TableColumn<?, ?> colAuctionEndTime;

    @FXML private Label itemNameLabel;
    @FXML private Label itemDescriptionLabel;
    @FXML private Label startingPriceLabel;
    @FXML private Label currentPriceLabel;
    @FXML private Label leaderLabel;
    @FXML private Label startTimeLabel;
    @FXML private Label endTimeLabel;
    @FXML private Label auctionStatusLabel;

    @FXML private TextField manualBidField;
    @FXML private TextField autoBidMaxField;
    @FXML private TextField autoBidIncrementField;
    @FXML private Label bidMessageLabel;

    @FXML private TableView<?> bidHistoryTable;
    @FXML private TableColumn<?, ?> colBidTime;
    @FXML private TableColumn<?, ?> colBidder;
    @FXML private TableColumn<?, ?> colBidAmount;
    @FXML private TableColumn<?, ?> colBidType;

    @FXML private LineChart<Number, Number> priceChart;
    @FXML private NumberAxis xAxis;
    @FXML private NumberAxis yAxis;

    @FXML private TabPane managementTabPane;

    @FXML private ChoiceBox<String> itemTypeChoiceBox;
    @FXML private TextField sellerItemNameField;
    @FXML private TextArea sellerItemDescriptionArea;
    @FXML private TextField sellerStartingPriceField;
    @FXML private TextField sellerStartTimeField;
    @FXML private TextField sellerEndTimeField;
    @FXML private TextArea sellerMetadataArea;
    @FXML private Label sellerMessageLabel;
    @FXML private TableView<?> sellerItemTable;
    @FXML private TableColumn<?, ?> colSellerItemName;
    @FXML private TableColumn<?, ?> colSellerItemType;
    @FXML private TableColumn<?, ?> colSellerItemPrice;

    @FXML private Label adminMessageLabel;
    @FXML private TableView<?> userTable;
    @FXML private TableColumn<?, ?> colUsername;
    @FXML private TableColumn<?, ?> colFullName;
    @FXML private TableColumn<?, ?> colEmail;
    @FXML private TableColumn<?, ?> colRole;
    @FXML private TableColumn<?, ?> colUserStatus;

    @FXML
    public void initialize() {
        statusFilterChoiceBox.getItems().addAll("Tất cả", "OPEN", "RUNNING", "FINISHED", "PAID", "CANCELED");
        statusFilterChoiceBox.setValue("Tất cả");

        itemTypeChoiceBox.getItems().addAll("electronics", "art", "vehicle");
        itemTypeChoiceBox.setValue("electronics");

        if (appContext.getCurrentUser() != null) {
            welcomeLabel.setText("Xin chào, " + appContext.getCurrentUser().getFullName());
        }
    }

    @FXML private void handleRefresh() {}
    @FXML private void handleLogout() {}
    @FXML private void handleFilterAuctions() {}
    @FXML private void handlePlaceBid() {}
    @FXML private void handleRegisterAutoBid() {}
    @FXML private void handleCreateItem() {}
    @FXML private void handleUpdateItem() {}
    @FXML private void handleDeleteItem() {}
    @FXML private void handleApproveUser() {}
    @FXML private void handleLockUser() {}
    @FXML private void handleRemoveAuction() {}
}