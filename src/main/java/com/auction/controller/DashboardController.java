package com.auction.controller;

import com.auction.app.AppContext;
import com.auction.model.auction.AuctionView;
import com.auction.model.auction.BidTransaction;
import com.auction.model.user.Bidder;
import com.auction.model.user.User;
import com.auction.realtime.RealtimeBidClient;
import com.auction.realtime.RealtimeBidMessage;
import com.auction.realtime.RealtimeBidServer;
import com.auction.repository.JdbcAuctionRepository;
import java.math.BigDecimal;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

public class DashboardController {

    private static final int REALTIME_PORT = 5555;

    @FXML
    private TextField manualBidField;

    @FXML
    private TableView<AuctionView> auctionTable;

    @FXML
    private TableColumn<AuctionView, String> colAuctionItem;

    @FXML
    private TableColumn<AuctionView, String> colAuctionSeller;

    @FXML
    private TableColumn<AuctionView, BigDecimal> colAuctionCurrentPrice;

    @FXML
    private TableColumn<AuctionView, String> colAuctionStatus;

    @FXML
    private TableColumn<AuctionView, String> colAuctionEndTime;

    @FXML
    private Label welcomeLabel;

    @FXML
    private TabPane managementTabPane;

    @FXML
    private LineChart<Number, Number> priceChart;

    @FXML
    private NumberAxis xAxis;

    @FXML
    private NumberAxis yAxis;

    private final AppContext appContext;

    private XYChart.Series<Number, Number> priceSeries;

    private RealtimeBidClient realtimeBidClient;

    public DashboardController(AppContext appContext) {
        this.appContext = appContext;
    }

    @FXML
    public void initialize() {
        openAllTabsForDemo();
        setupAuctionTable();
        setupPriceChart();
        setupRealtimeSocket();
        loadAuctionTable();

        if (auctionTable != null) {
            auctionTable.getSelectionModel()
                    .selectedItemProperty()
                    .addListener((observable, oldValue, newValue) -> {
                        if (newValue != null) {
                            showAuctionDetail(newValue);
                            rebuildPriceChart(newValue);
                        }
                    });
        }

        User currentUser = appContext.getCurrentUser();
        if (welcomeLabel != null && currentUser != null) {
            welcomeLabel.setText(
                    "Xin chào, " + currentUser.getFullName()
                            + " (" + currentUser.getRole() + ")"
            );
        }
    }

    private void setupRealtimeSocket() {
        RealtimeBidServer.getInstance().start(REALTIME_PORT);

        realtimeBidClient = new RealtimeBidClient(
                "localhost",
                REALTIME_PORT,
                this::handleRealtimeBidMessage
        );

        realtimeBidClient.connect();
    }

    private void handleRealtimeBidMessage(RealtimeBidMessage message) {
        Platform.runLater(() -> {
            AuctionView auctionView = findAuctionByMessage(message);

            if (auctionView == null) {
                return;
            }

            BidTransaction bidTransaction = new BidTransaction(
                    null,
                    message.getAmount(),
                    false
            );

            auctionView.setCurrentPrice(message.getAmount());
            auctionView.getBidHistory().add(bidTransaction);

            if (auctionTable != null) {
                auctionTable.refresh();
            }

            AuctionView selectedAuction = getSelectedAuction();

            if (selectedAuction != null
                    && isSameAuction(selectedAuction, auctionView)) {
                rebuildPriceChart(selectedAuction);
            }
        });
    }

    private AuctionView findAuctionByMessage(RealtimeBidMessage message) {
        if (auctionTable == null || auctionTable.getItems() == null) {
            return null;
        }

        for (AuctionView auctionView : auctionTable.getItems()) {
            if (auctionView == null) {
                continue;
            }

            if (message.getAuctionId() != null
                    && auctionView.getId() != null
                    && message.getAuctionId().equals(auctionView.getId())) {
                return auctionView;
            }

            if (message.getAuctionTitle() != null
                    && auctionView.getTitle() != null
                    && message.getAuctionTitle().equals(auctionView.getTitle())) {
                return auctionView;
            }
        }

        return null;
    }

    private boolean isSameAuction(AuctionView first, AuctionView second) {
        if (first == null || second == null) {
            return false;
        }

        if (first.getId() != null && second.getId() != null) {
            return first.getId().equals(second.getId());
        }

        if (first.getTitle() != null && second.getTitle() != null) {
            return first.getTitle().equals(second.getTitle());
        }

        return first == second;
    }

    private void setupPriceChart() {
        if (priceChart == null) {
            return;
        }

        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Giá đấu hiện tại");

        priceChart.getData().clear();
        priceChart.getData().add(priceSeries);

        priceChart.setAnimated(false);
        priceChart.setCreateSymbols(true);
        priceChart.setLegendVisible(false);
        priceChart.setTitle("Bid History Visualization - Realtime Price Curve");

        if (xAxis != null) {
            xAxis.setLabel("Thời gian / lượt bid");
            xAxis.setForceZeroInRange(false);
        }

        if (yAxis != null) {
            yAxis.setLabel("Giá đấu hiện tại");
            yAxis.setForceZeroInRange(false);
        }
    }

    private void rebuildPriceChart(AuctionView auctionView) {
        if (priceSeries == null || auctionView == null) {
            return;
        }

        priceSeries.getData().clear();

        int index = 1;

        if (auctionView.getBidHistory() != null) {
            for (BidTransaction bid : auctionView.getBidHistory()) {
                if (bid != null && bid.getAmount() != null) {
                    priceSeries.getData().add(
                            new XYChart.Data<>(
                                    index,
                                    bid.getAmount()
                            )
                    );
                    index++;
                }
            }
        }

        if (priceSeries.getData().isEmpty()
                && auctionView.getCurrentPrice() != null) {
            priceSeries.getData().add(
                    new XYChart.Data<>(
                            1,
                            auctionView.getCurrentPrice()
                    )
            );
        }
    }

    private void openAllTabsForDemo() {
        if (managementTabPane == null) {
            return;
        }

        managementTabPane.getTabs().forEach(tab -> {
            tab.setDisable(false);
            tab.setStyle("-fx-opacity: 1;");
        });
    }

    @FXML
    private void handleRefresh() {
        openAllTabsForDemo();
        loadAuctionTable();

        AuctionView selectedAuction = getSelectedAuction();

        if (selectedAuction != null) {
            rebuildPriceChart(selectedAuction);
        }
    }

    @FXML
    private void handleLogout() {
        appContext.setCurrentUser(null);
        appContext.getNavigator().showLogin();
    }

    @FXML
    private void handleFilterAuctions() {
        openAllTabsForDemo();
        loadAuctionTable();
    }

    @FXML
    private void handlePlaceBid() {
        AuctionView selectedAuction = getSelectedAuction();

        if (selectedAuction == null) {
            showAlert("Vui lòng chọn một phiên đấu giá!");
            return;
        }

        String amountText = manualBidField.getText();

        if (amountText == null || amountText.isBlank()) {
            showAlert("Vui lòng nhập số tiền bid!");
            return;
        }

        try {
            BigDecimal bidAmount = new BigDecimal(amountText.trim());

            if (selectedAuction.getCurrentPrice() != null
                    && bidAmount.compareTo(selectedAuction.getCurrentPrice()) <= 0) {
                showAlert("Giá bid phải cao hơn giá hiện tại!");
                return;
            }

            Bidder bidder = null;
            String bidderName = "Demo bidder";

            User currentUser = appContext.getCurrentUser();

            if (currentUser instanceof Bidder) {
                bidder = (Bidder) currentUser;
            }

            if (currentUser != null && currentUser.getFullName() != null) {
                bidderName = currentUser.getFullName();
            }

            BidTransaction newBid = new BidTransaction(
                    bidder,
                    bidAmount,
                    false
            );

            selectedAuction.setCurrentPrice(bidAmount);
            selectedAuction.getBidHistory().add(newBid);

            if (auctionTable != null) {
                auctionTable.refresh();
            }

            rebuildPriceChart(selectedAuction);

            if (realtimeBidClient != null) {
                RealtimeBidMessage message = new RealtimeBidMessage(
                        selectedAuction.getId(),
                        selectedAuction.getTitle(),
                        bidderName,
                        bidAmount,
                        System.currentTimeMillis(),
                        realtimeBidClient.getClientId()
                );

                realtimeBidClient.send(message);
            }

            manualBidField.clear();

            showAlert("Đặt bid thành công!");

        } catch (NumberFormatException exception) {
            showAlert("Số tiền bid không hợp lệ!");
        }
    }

    private AuctionView getSelectedAuction() {
        if (auctionTable == null) {
            return null;
        }

        return auctionTable.getSelectionModel().getSelectedItem();
    }

    private void showAuctionDetail(AuctionView auctionView) {
        if (auctionView == null) {
            return;
        }

        System.out.println("Selected auction: " + auctionView.getTitle());
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

    @FXML
    private void handleCloseApp() {
        System.exit(0);
    }

    private void setupAuctionTable() {
        if (auctionTable == null) {
            return;
        }

        colAuctionItem.setCellValueFactory(
                new PropertyValueFactory<>("title")
        );

        colAuctionSeller.setCellValueFactory(
                new PropertyValueFactory<>("sellerName")
        );

        colAuctionCurrentPrice.setCellValueFactory(
                new PropertyValueFactory<>("currentPrice")
        );

        colAuctionStatus.setCellValueFactory(
                new PropertyValueFactory<>("status")
        );

        colAuctionEndTime.setCellValueFactory(
                new PropertyValueFactory<>("endTimeText")
        );
    }

    private void loadAuctionTable() {
        if (auctionTable == null) {
            return;
        }

        JdbcAuctionRepository repository = new JdbcAuctionRepository();

        auctionTable.setItems(
                FXCollections.observableArrayList(repository.findAll())
        );
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}