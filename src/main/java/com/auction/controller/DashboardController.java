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
import com.auction.service.AuctionClosingService;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

public class DashboardController {

    private static final int REALTIME_PORT = 5555;

    private final NumberFormat vnFormat =
            NumberFormat.getInstance(new Locale("vi", "VN"));

    @FXML private TextField manualBidField;

    @FXML private TextField searchField;
    @FXML private ChoiceBox<String> statusFilterChoiceBox;

    @FXML private TableView<AuctionView> auctionTable;
    @FXML private TableColumn<AuctionView, String> colAuctionItem;
    @FXML private TableColumn<AuctionView, String> colAuctionSeller;
    @FXML private TableColumn<AuctionView, String> colAuctionCurrentPrice;
    @FXML private TableColumn<AuctionView, String> colAuctionStatus;
    @FXML private TableColumn<AuctionView, String> colAuctionEndTime;

    @FXML private TableView<BidTransaction> bidHistoryTable;
    @FXML private TableColumn<BidTransaction, Object> colBidTime;
    @FXML private TableColumn<BidTransaction, String> colBidder;
    @FXML private TableColumn<BidTransaction, String> colBidAmount;
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
    @FXML private TabPane managementTabPane;

    @FXML private LineChart<Number, Number> priceChart;
    @FXML private NumberAxis xAxis;
    @FXML private NumberAxis yAxis;

    private final AppContext appContext;
    private final AuctionClosingService auctionClosingService = new AuctionClosingService();

    private ObservableList<AuctionView> allAuctions = FXCollections.observableArrayList();

    private XYChart.Series<Number, Number> priceSeries;
    private RealtimeBidClient realtimeBidClient;

    public DashboardController(AppContext appContext) {
        this.appContext = appContext;
    }

    @FXML
    public void initialize() {
        openAllTabsForDemo();
        setupAuctionTable();
        setupBidHistoryTable();
        setupPriceChart();
        setupFilterControls();
        setupRealtimeSocket();
        loadAuctionTable();
        startAuctionMonitor();

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

    private void setupFilterControls() {
        if (statusFilterChoiceBox != null) {
            statusFilterChoiceBox.setItems(
                    FXCollections.observableArrayList(
                            "Tất cả",
                            "OPEN",
                            "RUNNING",
                            "FINISHED",
                            "PAID",
                            "CANCELED"
                    )
            );
            statusFilterChoiceBox.setValue("Tất cả");
        }

        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                applyAuctionFilter();
            });
        }

        if (statusFilterChoiceBox != null) {
            statusFilterChoiceBox.valueProperty().addListener((observable, oldValue, newValue) -> {
                applyAuctionFilter();
            });
        }
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) {
            return "";
        }

        return vnFormat.format(amount);
    }

    private void startAuctionMonitor() {
        Thread monitorThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);

                    Platform.runLater(() -> {
                        if (auctionTable == null || auctionTable.getItems() == null) {
                            return;
                        }

                        boolean changed = false;

                        for (AuctionView auctionView : allAuctions) {
                            boolean closed = auctionClosingService.checkAndClose(auctionView);

                            if (closed) {
                                changed = true;
                                System.out.println(
                                        "[AUCTION CLOSED] Winner: "
                                                + auctionClosingService.getWinnerName(auctionView)
                                );
                            }
                        }

                        if (changed) {
                            applyAuctionFilter();

                            AuctionView selectedAuction = getSelectedAuction();
                            if (selectedAuction != null) {
                                showAuctionDetail(selectedAuction);
                                rebuildBidHistoryTable(selectedAuction);
                                rebuildPriceChart(selectedAuction);
                            }
                        }
                    });

                } catch (Exception ignored) {
                }
            }
        });

        monitorThread.setDaemon(true);
        monitorThread.setName("auction-closing-monitor");
        monitorThread.start();
    }

    private void setupRealtimeSocket() {
        try {
            RealtimeBidServer.getInstance().start(REALTIME_PORT);

            realtimeBidClient = new RealtimeBidClient(
                    "localhost",
                    REALTIME_PORT,
                    this::handleRealtimeBidMessage
            );

            realtimeBidClient.connect();

        } catch (Exception exception) {
            System.out.println("Realtime socket disabled: " + exception.getMessage());
        }
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

            if (selectedAuction != null && isSameAuction(selectedAuction, auctionView)) {
                rebuildBidHistoryTable(selectedAuction);
                rebuildPriceChart(selectedAuction);
                showAuctionDetail(selectedAuction);
            }
        });
    }

    private AuctionView findAuctionByMessage(RealtimeBidMessage message) {
        if (allAuctions == null) {
            return null;
        }

        for (AuctionView auctionView : allAuctions) {
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

    private void openAllTabsForDemo() {
        if (mainTabPane != null) {
            mainTabPane.getTabs().forEach(tab -> {
                tab.setDisable(false);
                tab.setStyle("-fx-opacity: 1;");
            });
        }

        if (managementTabPane != null) {
            managementTabPane.getTabs().forEach(tab -> {
                tab.setDisable(false);
                tab.setStyle("-fx-opacity: 1;");
            });
        }
    }

    private void setupAuctionTable() {
        if (auctionTable == null) {
            return;
        }

        colAuctionItem.setCellValueFactory(new PropertyValueFactory<>("title"));
        colAuctionSeller.setCellValueFactory(new PropertyValueFactory<>("sellerName"));

        colAuctionCurrentPrice.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        formatMoney(cellData.getValue().getCurrentPrice())
                )
        );

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
                new SimpleStringProperty(
                        formatMoney(cellData.getValue().getAmount())
                )
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
        applyAuctionFilter();
    }

    private void applyAuctionFilter() {
        if (auctionTable == null) {
            return;
        }

        String keyword = "";

        if (searchField != null && searchField.getText() != null) {
            keyword = searchField.getText().trim().toLowerCase();
        }

        String selectedStatus = "Tất cả";

        if (statusFilterChoiceBox != null && statusFilterChoiceBox.getValue() != null) {
            selectedStatus = statusFilterChoiceBox.getValue();
        }

        ObservableList<AuctionView> filteredAuctions =
                FXCollections.observableArrayList();

        for (AuctionView auctionView : allAuctions) {
            if (auctionView == null) {
                continue;
            }

            String title = auctionView.getTitle() == null
                    ? ""
                    : auctionView.getTitle().toLowerCase();

            String status = auctionView.getStatus() == null
                    ? ""
                    : String.valueOf(auctionView.getStatus());

            boolean matchesKeyword = keyword.isBlank()
                    || title.contains(keyword);

            boolean matchesStatus = "Tất cả".equals(selectedStatus)
                    || status.equalsIgnoreCase(selectedStatus);

            if (matchesKeyword && matchesStatus) {
                filteredAuctions.add(auctionView);
            }
        }

        auctionTable.setItems(filteredAuctions);
        auctionTable.refresh();
    }

    @FXML
    private void handlePlaceBid() {
        AuctionView selectedAuction = getSelectedAuction();

        if (selectedAuction == null) {
            showAlert("Vui lòng chọn một phiên đấu giá ở tab Phiên đấu giá trước!");
            return;
        }

        auctionClosingService.checkAndClose(selectedAuction);

        if ("FINISHED".equals(String.valueOf(selectedAuction.getStatus()))
                || "PAID".equals(String.valueOf(selectedAuction.getStatus()))
                || "CANCELED".equals(String.valueOf(selectedAuction.getStatus()))) {
            auctionTable.refresh();
            showAuctionDetail(selectedAuction);
            showAlert("Phiên đấu giá đã kết thúc, không thể đặt bid!");
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

            auctionTable.refresh();
            rebuildBidHistoryTable(selectedAuction);
            rebuildPriceChart(selectedAuction);
            showAuctionDetail(selectedAuction);

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

            if (bidMessageLabel != null) {
                bidMessageLabel.setText("Đặt bid thành công: " + formatMoney(bidAmount));
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
                            : formatMoney(auctionView.getItem().getStartingPrice())
            );
        }

        if (currentPriceLabel != null) {
            currentPriceLabel.setText(formatMoney(auctionView.getCurrentPrice()));
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
        showAlert("Chức năng Auto-Bid đã được kích hoạt.");
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

    @FXML
    private void handleCloseApp() {
        System.exit(0);
    }

    private void loadAuctionTable() {
        if (auctionTable == null) {
            return;
        }

        JdbcAuctionRepository repository = new JdbcAuctionRepository();

        allAuctions = FXCollections.observableArrayList(repository.findAll());

        applyAuctionFilter();
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}