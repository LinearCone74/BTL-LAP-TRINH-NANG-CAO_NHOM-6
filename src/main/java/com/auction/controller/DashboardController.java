package com.auction.controller;

import com.auction.app.AppContext;
import com.auction.model.auction.AuctionView;
import com.auction.model.auction.BidTransaction;
import com.auction.model.user.Bidder;
import com.auction.model.user.User;
import com.auction.repository.JdbcAuctionRepository;
import java.math.BigDecimal;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
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

    @FXML private TextField manualBidField;

    @FXML private TableView<AuctionView> auctionTable;
    @FXML private TableColumn<AuctionView, String> colAuctionItem;
    @FXML private TableColumn<AuctionView, String> colAuctionSeller;
    @FXML private TableColumn<AuctionView, BigDecimal> colAuctionCurrentPrice;
    @FXML private TableColumn<AuctionView, String> colAuctionStatus;
    @FXML private TableColumn<AuctionView, String> colAuctionEndTime;

    @FXML private TableView<BidTransaction> bidHistoryTable;
    @FXML private TableColumn<BidTransaction, Object> colBidTime;
    @FXML private TableColumn<BidTransaction, String> colBidder;
    @FXML private TableColumn<BidTransaction, BigDecimal> colBidAmount;
    @FXML private TableColumn<BidTransaction, String> colBidType;

    @FXML private Label welcomeLabel;
    @FXML private Label itemNameLabel;
    @FXML private Label itemDescriptionLabel;
    @FXML private Label startingPriceLabel;
    @FXML private Label currentPriceLabel;
    @FXML private Label leaderLabel;
    @FXML private Label startTimeLabel;
    @FXML private Label endTimeLabel;
    @FXML private Label auctionStatusLabel;
    @FXML private Label bidMessageLabel;

    @FXML private TabPane mainTabPane;

    @FXML private LineChart<Number, Number> priceChart;
    @FXML private NumberAxis xAxis;
    @FXML private NumberAxis yAxis;

    private final AppContext appContext;
    private XYChart.Series<Number, Number> priceSeries;

    public DashboardController(AppContext appContext) {
        this.appContext = appContext;
    }

    @FXML
    public void initialize() {
        openAllTabsForDemo();
        setupAuctionTable();
        setupBidHistoryTable();
        setupPriceChart();
        loadAuctionTable();

        if (auctionTable != null) {
            auctionTable.getSelectionModel()
                    .selectedItemProperty()
                    .addListener((observable, oldValue, newValue) -> {
                        if (newValue != null) {
                            showAuctionDetail(newValue);
                            rebuildBidHistoryTable(newValue);
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

    private void openAllTabsForDemo() {
        if (mainTabPane == null) {
            return;
        }

        mainTabPane.getTabs().forEach(tab -> {
            tab.setDisable(false);
            tab.setStyle("-fx-opacity: 1;");
        });
    }

    private void setupAuctionTable() {
        if (auctionTable == null) {
            return;
        }

        colAuctionItem.setCellValueFactory(new PropertyValueFactory<>("title"));
        colAuctionSeller.setCellValueFactory(new PropertyValueFactory<>("sellerName"));
        colAuctionCurrentPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colAuctionStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colAuctionEndTime.setCellValueFactory(new PropertyValueFactory<>("endTimeText"));
    }

    private void setupBidHistoryTable() {
        if (bidHistoryTable == null) {
            return;
        }

        colBidTime.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(cellData.getValue().getBidTime())
        );

        colBidder.setCellValueFactory(cellData -> {
            Bidder bidder = cellData.getValue().getBidder();
            String bidderName = bidder == null ? "Demo bidder" : bidder.getFullName();
            return new SimpleStringProperty(bidderName);
        });

        colBidAmount.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(cellData.getValue().getAmount())
        );

        colBidType.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().isAutoBid() ? "Auto-bid" : "Manual")
        );
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

    @FXML
    private void handleRefresh() {
        openAllTabsForDemo();
        loadAuctionTable();

        AuctionView selectedAuction = getSelectedAuction();
        if (selectedAuction != null) {
            showAuctionDetail(selectedAuction);
            rebuildBidHistoryTable(selectedAuction);
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
        loadAuctionTable();
    }

    @FXML
    private void handlePlaceBid() {
        AuctionView selectedAuction = getSelectedAuction();

        if (selectedAuction == null) {
            showAlert("Vui lòng chọn một phiên đấu giá ở tab Phiên đấu giá trước!");
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
            User currentUser = appContext.getCurrentUser();
            if (currentUser instanceof Bidder) {
                bidder = (Bidder) currentUser;
            }

            BidTransaction newBid = new BidTransaction(bidder, bidAmount, false);

            selectedAuction.setCurrentPrice(bidAmount);
            selectedAuction.getBidHistory().add(newBid);

            auctionTable.refresh();
            rebuildBidHistoryTable(selectedAuction);
            rebuildPriceChart(selectedAuction);
            showAuctionDetail(selectedAuction);

            manualBidField.clear();

            if (bidMessageLabel != null) {
                bidMessageLabel.setText("Đặt bid thành công: " + bidAmount);
            }

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

        if (itemNameLabel != null) {
            itemNameLabel.setText(auctionView.getTitle());
        }

        if (itemDescriptionLabel != null) {
            itemDescriptionLabel.setText(auctionView.getDescription());
        }

        if (startingPriceLabel != null) {
            startingPriceLabel.setText(
                    auctionView.getItem() == null
                            ? ""
                            : String.valueOf(auctionView.getItem().getStartingPrice())
            );
        }

        if (currentPriceLabel != null) {
            currentPriceLabel.setText(String.valueOf(auctionView.getCurrentPrice()));
        }

        if (leaderLabel != null) {
            leaderLabel.setText(
                    auctionView.getHighestBidder() == null
                            ? "Chưa có"
                            : auctionView.getHighestBidder().getFullName()
            );
        }

        if (startTimeLabel != null) {
            startTimeLabel.setText(String.valueOf(auctionView.getStartTime()));
        }

        if (endTimeLabel != null) {
            endTimeLabel.setText(String.valueOf(auctionView.getEndTime()));
        }

        if (auctionStatusLabel != null) {
            auctionStatusLabel.setText(String.valueOf(auctionView.getStatus()));
        }
    }

    private void rebuildBidHistoryTable(AuctionView auctionView) {
        if (bidHistoryTable == null || auctionView == null) {
            return;
        }

        bidHistoryTable.setItems(
                FXCollections.observableArrayList(auctionView.getBidHistory())
        );
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
                            new XYChart.Data<>(index, bid.getAmount())
                    );
                    index++;
                }
            }
        }

        if (priceSeries.getData().isEmpty()
                && auctionView.getCurrentPrice() != null) {
            priceSeries.getData().add(
                    new XYChart.Data<>(1, auctionView.getCurrentPrice())
            );
        }
    }

    @FXML
    private void handleRegisterAutoBid() {
        showAlert("Chức năng Auto-Bid đã được kích hoạt ở mức demo.");
    }

    @FXML
    private void handleCreateItem() {
        showAlert("Chức năng Seller: mở phiên đấu giá.");
    }

    @FXML
    private void handleUpdateItem() {
        showAlert("Chức năng Seller: cập nhật phiên đấu giá.");
    }

    @FXML
    private void handleDeleteItem() {
        showAlert("Chức năng Seller: xóa phiên đấu giá.");
    }

    @FXML
    private void handleApproveUser() {
        showAlert("Chức năng Admin: duyệt tài khoản.");
    }

    @FXML
    private void handleLockUser() {
        showAlert("Chức năng Admin: khóa tài khoản.");
    }

    @FXML
    private void handleRemoveAuction() {
        showAlert("Chức năng Admin: gỡ phiên đấu giá.");
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