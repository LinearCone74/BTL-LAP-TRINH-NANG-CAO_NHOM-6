package com.auction.controller;

import javafx.scene.chart.XYChart;
import java.math.BigDecimal;

import com.auction.app.AppContext;
import com.auction.model.auction.AuctionView;
import com.auction.model.user.User;
import com.auction.repository.JdbcBidRepository;
import com.auction.repository.JdbcItemRepository;
import com.auction.repository.JdbcItemRepository.ItemRow;
import com.auction.repository.RealtimeAuctionRepository;
import com.auction.socket.AuctionSocketClient;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;

import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import javafx.scene.control.cell.PropertyValueFactory;

import java.text.NumberFormat;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class DashboardController {

    private static final int REALTIME_REFRESH_SECONDS = 30;

    private static final String SOCKET_HOST =
            System.getProperty("auction.socket.host", "localhost");

    private static final int SOCKET_PORT =
            Integer.getInteger("auction.socket.port", 5555);

    // =========================
    // TAB / ROOT
    // =========================

    @FXML
    private TabPane managementTabPane;

    @FXML
    private TabPane mainTabPane;

    @FXML
    private Tab bidRealtimeTab;

    @FXML
    private Tab sellerTab;

    @FXML
    private Tab adminTab;

    // =========================
    // TOPBAR
    // =========================

    @FXML
    private Label welcomeLabel;

    // =========================
    // FILTER
    // =========================

    @FXML
    private TextField searchField;

    @FXML
    private ChoiceBox<String> statusFilterChoiceBox;

    // =========================
    // AUCTION TABLE
    // =========================

    @FXML
    private TableView<AuctionView> auctionTable;

    @FXML
    private TableColumn<AuctionView, String> colAuctionItem;

    @FXML
    private TableColumn<AuctionView, String> colAuctionSeller;

    @FXML
    private TableColumn<AuctionView, String> colAuctionCurrentPrice;

    @FXML
    private TableColumn<AuctionView, String> colAuctionStatus;

    @FXML
    private TableColumn<AuctionView, String> colAuctionEndTime;

    // =========================
    // DETAIL
    // =========================

    @FXML
    private Label itemNameLabel;

    @FXML
    private Label itemDescriptionLabel;

    @FXML
    private Label startingPriceLabel;

    @FXML
    private Label currentPriceLabel;

    @FXML
    private Label leaderLabel;

    @FXML
    private Label startTimeLabel;

    @FXML
    private Label endTimeLabel;

    @FXML
    private Label auctionStatusLabel;

    // =========================
    // BID
    // =========================

    @FXML
    private TextField manualBidField;

    @FXML
    private TextField autoBidMaxField;

    @FXML
    private TextField autoBidIncrementField;

    @FXML
    private Label bidMessageLabel;

    // =========================
    // BID HISTORY
    // =========================

    @FXML
    private TableView<RealtimeAuctionRepository.BidHistoryRow> bidHistoryTable;

    @FXML
    private TableColumn<RealtimeAuctionRepository.BidHistoryRow, String> colBidTime;

    @FXML
    private TableColumn<RealtimeAuctionRepository.BidHistoryRow, String> colBidder;

    @FXML
    private TableColumn<RealtimeAuctionRepository.BidHistoryRow, String> colBidAmount;

    @FXML
    private TableColumn<RealtimeAuctionRepository.BidHistoryRow, String> colBidType;

    // =========================
    // CHART
    // =========================

    @FXML
    private LineChart<Number, Number> priceChart;

    @FXML
    private NumberAxis xAxis;

    @FXML
    private NumberAxis yAxis;

    // =========================
    // SELLER
    // =========================

    @FXML
    private ChoiceBox<String> itemTypeChoiceBox;

    @FXML
    private TextField sellerItemNameField;

    @FXML
    private TextArea sellerItemDescriptionArea;

    @FXML
    private TextField sellerStartingPriceField;

    @FXML
    private TextField sellerStartTimeField;

    @FXML
    private TextField sellerEndTimeField;

    @FXML
    private TextArea sellerMetadataArea;

    @FXML
    private Label sellerMessageLabel;

    // *** THAY ĐỔI: đổi từ TableView<?> sang TableView<ItemRow> ***
    @FXML
    private TableView<ItemRow> sellerItemTable;

    // *** THÊM MỚI: 3 cột cho seller table ***
    @FXML
    private TableColumn<ItemRow, String> colSellerItemName;

    @FXML
    private TableColumn<ItemRow, String> colSellerItemCategory;

    @FXML
    private TableColumn<ItemRow, String> colSellerItemPrice;

    // *** THÊM MỚI: lưu item đang chọn để sửa/xóa ***
    private Integer selectedItemId;

    // =========================
    // ADMIN
    // =========================

    @FXML
    private Label adminMessageLabel;

    @FXML
    private TableView<?> userTable;

    // =========================
    // LOGIC
    // =========================

    private final AppContext appContext;

    private final JdbcBidRepository bidRepository =
            new JdbcBidRepository();

    // *** THÊM MỚI ***
    private final JdbcItemRepository itemRepository =
            new JdbcItemRepository();

    private final RealtimeAuctionRepository realtimeRepo =
            new RealtimeAuctionRepository();

    private final NumberFormat currencyFormat =
            NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    private final AtomicBoolean refreshInProgress =
            new AtomicBoolean(false);

    private ScheduledExecutorService refreshExecutor;

    private AuctionSocketClient socketClient;

    private volatile Runnable pendingSocketSuccessAction;

    private AuctionView selectedAuctionForBid;

    private volatile Integer selectedAuctionIdForBid;

    private volatile int lastAuctionTableHash = Integer.MIN_VALUE;

    private volatile int lastBidHistoryVersion = Integer.MIN_VALUE;

    // =========================
    // CONSTRUCTOR
    // =========================

    public DashboardController(AppContext appContext) {
        this.appContext = appContext;
    }

    // =========================
    // INITIALIZE
    // =========================

    @FXML
    public void initialize() {

        setupAuctionTable();

        setupBidHistoryTable();

        setupAuctionTableClick();

        setupChoiceBoxes();

        // *** THÊM MỚI ***
        setupSellerTable();

        startSocketClient();

        startRealtimeRefresh();

        submitRefreshNow();

        User current = appContext.getCurrentUser();

        if (welcomeLabel != null && current != null) {
            welcomeLabel.setText(
                    "Xin chào, "
                            + current.getFullName()
                            + " ("
                            + current.getRole()
                            + ")"
            );

            applyRolePermissions();
        }
    }

    // =========================
    // PHÂN QUYỀN
    // =========================

    private void applyRolePermissions() {
        User current = appContext.getCurrentUser();
        if (current == null || mainTabPane == null) return;

        String role = current.getRole() != null ? current.getRole().name().toUpperCase() : "";

        if (sellerTab != null) mainTabPane.getTabs().remove(sellerTab);
        if (adminTab != null) mainTabPane.getTabs().remove(adminTab);

        if ("SELLER".equals(role) && sellerTab != null) {
            mainTabPane.getTabs().add(sellerTab);
        } else if ("ADMIN".equals(role) && adminTab != null) {
            mainTabPane.getTabs().add(adminTab);
        }

        boolean isBidder = "BIDDER".equals(role);

        if (manualBidField != null) manualBidField.setDisable(!isBidder);
        if (autoBidMaxField != null) autoBidMaxField.setDisable(!isBidder);
        if (autoBidIncrementField != null) autoBidIncrementField.setDisable(!isBidder);

        if (!isBidder && bidMessageLabel != null) {
            bidMessageLabel.setText("Tài khoản Seller/Admin không thể đặt bid.");
            bidMessageLabel.setStyle("-fx-text-fill: #e53e3e; -fx-font-weight: bold;");
        }
    }

    // =========================
    // SETUP
    // =========================

    private void setupChoiceBoxes() {

        if (statusFilterChoiceBox != null) {
            statusFilterChoiceBox.setItems(
                    FXCollections.observableArrayList(
                            "Tất cả", "OPEN", "RUNNING", "ENDED"
                    )
            );
            statusFilterChoiceBox.setValue("Tất cả");
        }

        if (itemTypeChoiceBox != null) {
            itemTypeChoiceBox.setItems(
                    FXCollections.observableArrayList(
                            "Laptop", "Điện thoại", "Xe", "Đồng hồ", "Khác"
                    )
            );
        }
    }

    private void setupAuctionTableClick() {

        auctionTable.setOnMouseClicked(event -> {

            AuctionView selectedAuction =
                    auctionTable.getSelectionModel().getSelectedItem();

            if (selectedAuction == null) return;

            selectedAuctionForBid = selectedAuction;
            selectedAuctionIdForBid = parseAuctionId(selectedAuction);
            lastBidHistoryVersion = Integer.MIN_VALUE;

            updateBidRealtimeTab(selectedAuctionForBid);
            loadBidHistoryAsync(selectedAuctionIdForBid, true);

            if (mainTabPane != null && bidRealtimeTab != null) {
                mainTabPane.getSelectionModel().select(bidRealtimeTab);
            }
        });
    }

    private void setupAuctionTable() {

        colAuctionItem.setCellValueFactory(new PropertyValueFactory<>("title"));
        colAuctionSeller.setCellValueFactory(new PropertyValueFactory<>("sellerName"));

        colAuctionCurrentPrice.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        formatMoney(cellData.getValue().getCurrentPrice())
                )
        );

        colAuctionStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        colAuctionEndTime.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        formatDateTime(cellData.getValue().getEndTimeText())
                )
        );

        centerColumn(colAuctionItem);
        centerColumn(colAuctionSeller);
        centerColumn(colAuctionCurrentPrice);
        centerColumn(colAuctionStatus);
        centerColumn(colAuctionEndTime);
    }

    private void setupBidHistoryTable() {

        if (bidHistoryTable == null) return;

        colBidTime.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        formatDateTime(cellData.getValue().time())
                )
        );

        colBidder.setCellValueFactory(
                cellData -> new SimpleStringProperty(cellData.getValue().bidder())
        );

        colBidAmount.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        formatMoney(new BigDecimal(cellData.getValue().amount()))
                )
        );

        colBidType.setCellValueFactory(
                cellData -> new SimpleStringProperty(cellData.getValue().type())
        );
    }

    // *** THÊM MỚI: setup bảng sản phẩm của Seller ***
    private void setupSellerTable() {

        if (sellerItemTable == null) return;

        if (colSellerItemName != null) {
            colSellerItemName.setCellValueFactory(
                    cd -> new SimpleStringProperty(cd.getValue().name())
            );
        }

        if (colSellerItemCategory != null) {
            colSellerItemCategory.setCellValueFactory(
                    cd -> new SimpleStringProperty(cd.getValue().category())
            );
        }

        if (colSellerItemPrice != null) {
            colSellerItemPrice.setCellValueFactory(
                    cd -> new SimpleStringProperty(
                            formatMoney(cd.getValue().startingPrice())
                    )
            );
        }

        // Click vào row → điền thông tin vào form để sửa
        sellerItemTable.setOnMouseClicked(e -> {
            ItemRow selected = sellerItemTable.getSelectionModel().getSelectedItem();
            if (selected == null) return;

            selectedItemId = selected.itemId();

            if (sellerItemNameField != null)
                sellerItemNameField.setText(selected.name());

            if (sellerItemDescriptionArea != null)
                sellerItemDescriptionArea.setText(
                        selected.description() == null ? "" : selected.description()
                );

            if (sellerStartingPriceField != null)
                sellerStartingPriceField.setText(
                        selected.startingPrice().toPlainString()
                );

            if (sellerMetadataArea != null)
                sellerMetadataArea.setText(
                        selected.metadata() == null ? "" : selected.metadata()
                );

            if (itemTypeChoiceBox != null && selected.category() != null)
                itemTypeChoiceBox.setValue(selected.category());
        });

        // Load danh sách sản phẩm của seller hiện tại
        refreshSellerTable();
    }

    // *** THÊM MỚI: load lại bảng seller ***
    private void refreshSellerTable() {

        if (sellerItemTable == null) return;

        String username = getCurrentUsername();
        if (username == null) return;

        startRealtimeRefresh();

        refreshExecutor.execute(() -> {
            List<ItemRow> items = itemRepository.findBySeller(username);
            Platform.runLater(() ->
                    sellerItemTable.setItems(
                            FXCollections.observableArrayList(items)
                    )
            );
        });
    }

    // *** THÊM MỚI: hiển thị thông báo ở seller tab ***
    private void setSellerMessage(String msg, boolean success) {
        if (sellerMessageLabel != null) {
            sellerMessageLabel.setText(msg);
            sellerMessageLabel.setStyle(success
                    ? "-fx-text-fill: #38a169; -fx-font-weight: bold;"
                    : "-fx-text-fill: #e53e3e; -fx-font-weight: bold;"
            );
        }
    }

    // *** THÊM MỚI: xóa trắng form seller ***
    private void clearSellerForm() {
        selectedItemId = null;
        if (sellerItemNameField != null) sellerItemNameField.clear();
        if (sellerItemDescriptionArea != null) sellerItemDescriptionArea.clear();
        if (sellerStartingPriceField != null) sellerStartingPriceField.clear();
        if (sellerStartTimeField != null) sellerStartTimeField.clear();
        if (sellerEndTimeField != null) sellerEndTimeField.clear();
        if (sellerMetadataArea != null) sellerMetadataArea.clear();
        if (sellerItemTable != null) sellerItemTable.getSelectionModel().clearSelection();
    }

    // =========================
    // REALTIME
    // =========================

    private void startRealtimeRefresh() {

        if (refreshExecutor != null && !refreshExecutor.isShutdown()) return;

        refreshExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "auction-realtime-refresh");
            thread.setDaemon(true);
            return thread;
        });

        refreshExecutor.scheduleWithFixedDelay(
                this::refreshRealtimeDataInBackground,
                REALTIME_REFRESH_SECONDS,
                REALTIME_REFRESH_SECONDS,
                TimeUnit.SECONDS
        );
    }

    private void submitRefreshNow() {
        startRealtimeRefresh();
        refreshExecutor.execute(this::refreshRealtimeDataInBackground);
    }

    private void refreshRealtimeDataInBackground() {

        if (!refreshInProgress.compareAndSet(false, true)) return;

        try {

            Integer selectedId = selectedAuctionIdForBid;
            List<AuctionView> auctions = realtimeRepo.findAll();
            int tableHash = calculateAuctionTableHash(auctions);
            boolean tableChanged = tableHash != lastAuctionTableHash;

            AuctionView freshSelected = null;

            if (selectedId != null) {
                freshSelected = findAuctionInList(auctions, selectedId);
                if (freshSelected == null) {
                    freshSelected = realtimeRepo.findById(selectedId);
                }
            }

            int historyVersion = selectedId == null
                    ? lastBidHistoryVersion
                    : realtimeRepo.findLastBidId(selectedId);

            boolean historyChanged = selectedId != null
                    && historyVersion != lastBidHistoryVersion;

            List<RealtimeAuctionRepository.BidHistoryRow> historyRows =
                    historyChanged ? realtimeRepo.findBidHistory(selectedId) : null;

            AuctionView finalFreshSelected = freshSelected;
            List<RealtimeAuctionRepository.BidHistoryRow> finalHistoryRows = historyRows;

            Platform.runLater(() -> {

                if (tableChanged) {
                    lastAuctionTableHash = tableHash;
                    auctionTable.setItems(FXCollections.observableArrayList(auctions));
                    if (selectedId != null) selectAuctionInTable(selectedId);
                }

                if (finalFreshSelected != null) {
                    selectedAuctionForBid = finalFreshSelected;
                    updateBidRealtimeTab(finalFreshSelected);
                }

                if (finalHistoryRows != null && bidHistoryTable != null) {
                    lastBidHistoryVersion = historyVersion;
                    bidHistoryTable.setItems(
                            FXCollections.observableArrayList(finalHistoryRows)
                    );
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            refreshInProgress.set(false);
        }
    }

    // =========================
    // BID HISTORY
    // =========================

    private void loadBidHistoryAsync(Integer auctionId, boolean force) {

        if (auctionId == null || bidHistoryTable == null) return;

        startRealtimeRefresh();

        refreshExecutor.execute(() -> {

            int historyVersion = realtimeRepo.findLastBidId(auctionId);

            if (!force && historyVersion == lastBidHistoryVersion) return;

            List<RealtimeAuctionRepository.BidHistoryRow> historyRows =
                    realtimeRepo.findBidHistory(auctionId);

            Platform.runLater(() -> {
                lastBidHistoryVersion = historyVersion;
                bidHistoryTable.setItems(
                        FXCollections.observableArrayList(historyRows)
                );
                updatePriceChart(historyRows);
            });
        });
    }

    private void updatePriceChart(
            List<RealtimeAuctionRepository.BidHistoryRow> historyRows
    ) {

        if (priceChart == null) return;

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        int index = 1;

        Collections.reverse(historyRows);

        for (RealtimeAuctionRepository.BidHistoryRow row : historyRows) {
            try {
                String amountText = row.amount()
                        .replace("VNĐ", "")
                        .replace(".", "")
                        .replace(",", "")
                        .trim();
                double amount = Double.parseDouble(amountText);
                series.getData().add(new XYChart.Data<>(index++, amount));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        priceChart.getData().clear();
        priceChart.getData().add(series);
        xAxis.setLabel("Lượt bid");
        yAxis.setLabel("Giá");
    }

    // =========================
    // HASH / FIND
    // =========================

    private int calculateAuctionTableHash(List<AuctionView> auctions) {
        int hash = 1;
        for (AuctionView auction : auctions) {
            hash = 31 * hash + Objects.hash(
                    auction.getId(), auction.getTitle(), auction.getSellerName(),
                    auction.getCurrentPrice(), auction.getStatus(),
                    auction.getEndTimeText(), auction.getHighestBidderName()
            );
        }
        return hash;
    }

    private AuctionView findAuctionInList(List<AuctionView> auctions, int auctionId) {
        for (AuctionView auction : auctions) {
            Integer id = parseAuctionId(auction);
            if (id != null && id == auctionId) return auction;
        }
        return null;
    }

    private Integer parseAuctionId(AuctionView auction) {
        if (auction == null || auction.getId() == null) return null;
        try {
            return Integer.parseInt(auction.getId());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void selectAuctionInTable(int auctionId) {
        for (AuctionView auction : auctionTable.getItems()) {
            Integer id = parseAuctionId(auction);
            if (id != null && id == auctionId) {
                auctionTable.getSelectionModel().select(auction);
                return;
            }
        }
    }

    // =========================
    // UPDATE UI
    // =========================

    private void updateBidRealtimeTab(AuctionView auction) {

        if (auction == null) return;

        itemNameLabel.setText(auction.getTitle());

        itemDescriptionLabel.setText(
                auction.getDescription() == null ? "" : auction.getDescription()
        );

        startingPriceLabel.setText(formatMoney(auction.getCurrentPrice()));
        currentPriceLabel.setText(formatMoney(auction.getCurrentPrice()));

        String leader = auction.getHighestBidderName();
        leaderLabel.setText(leader == null || leader.isBlank() ? "Chưa có" : leader);

        startTimeLabel.setText(
                auction.getStartTime() == null
                        ? ""
                        : formatDateTime(auction.getStartTime().toString())
        );

        endTimeLabel.setText(formatDateTime(auction.getEndTimeText()));
        auctionStatusLabel.setText(auction.getStatus().toString());
    }

    // =========================
    // BUTTON EVENTS
    // =========================

    @FXML
    private void handleRefresh() {
        submitRefreshNow();
    }

    @FXML
    private void handleLogout() {
        stopRealtimeRefresh();
        appContext.setCurrentUser(null);
        appContext.getNavigator().showLogin();
    }

    @FXML
    private void handleFilterAuctions() {
        submitRefreshNow();
    }

    @FXML
    private void handlePlaceBid() {

        if (!isBidderRole()) return;

        Integer auctionId = getSelectedAuctionId();

        if (auctionId == null) {
            setBidMessage("Vui lòng chọn một phiên đấu giá!");
            return;
        }

        BigDecimal bidAmount;
        try {
            bidAmount = parseMoney(manualBidField.getText());
        } catch (NumberFormatException e) {
            setBidMessage("Số tiền bid không hợp lệ!");
            return;
        }

        if (sendBidViaSocket(auctionId, bidAmount, () -> manualBidField.clear())) return;

        runDatabaseAction(
                () -> {
                    RealtimeAuctionRepository.BidResponse response = realtimeRepo.placeBid(
                            auctionId, getCurrentUsername(), bidAmount
                    );
                    bidRepository.saveBid(auctionId, getCurrentUsername(), bidAmount.doubleValue());
                    return response;
                },
                () -> manualBidField.clear()
        );
    }

    @FXML
    private void handleRegisterAutoBid() {

        if (!isBidderRole()) return;

        Integer auctionId = getSelectedAuctionId();

        if (auctionId == null) {
            setBidMessage("Vui lòng chọn một phiên đấu giá!");
            return;
        }

        BigDecimal maxBid;
        BigDecimal increment;

        try {
            maxBid = parseMoney(autoBidMaxField.getText());
            increment = parseMoney(autoBidIncrementField.getText());
        } catch (NumberFormatException e) {
            setBidMessage("Max bid hoặc bước nhảy không hợp lệ!");
            return;
        }

        Runnable clearFields = () -> {
            autoBidMaxField.clear();
            autoBidIncrementField.clear();
        };

        if (sendAutoBidViaSocket(auctionId, maxBid, increment, clearFields)) return;

        runDatabaseAction(
                () -> realtimeRepo.registerAutoBid(
                        auctionId, getCurrentUsername(), maxBid, increment
                ),
                clearFields
        );
    }

    // =========================
    // SOCKET
    // =========================

    private void startSocketClient() {

        if (socketClient != null && socketClient.isConnected()) return;

        socketClient = new AuctionSocketClient(SOCKET_HOST, SOCKET_PORT);

        socketClient.setStatusListener(
                message -> Platform.runLater(() -> setBidMessage(message))
        );

        socketClient.setUpdateListener(auctionId -> {
            Integer selectedId = selectedAuctionIdForBid;
            lastAuctionTableHash = Integer.MIN_VALUE;
            if (selectedId != null && selectedId == auctionId) {
                lastBidHistoryVersion = Integer.MIN_VALUE;
            }
            submitRefreshNow();
        });

        socketClient.setResultListener(result -> Platform.runLater(() -> {
            setBidMessage(result.message());
            if (result.success()) {
                Runnable successAction = pendingSocketSuccessAction;
                pendingSocketSuccessAction = null;
                if (successAction != null) successAction.run();
                lastAuctionTableHash = Integer.MIN_VALUE;
                lastBidHistoryVersion = Integer.MIN_VALUE;
                submitRefreshNow();
            }
        }));

        socketClient.connect();
    }

    // =========================
    // SOCKET SEND
    // =========================

    private boolean sendBidViaSocket(int auctionId, BigDecimal amount, Runnable onSuccess) {

        if (socketClient == null || !socketClient.isConnected()) return false;

        try {
            setBidMessage("Đang gửi bid qua socket server...");
            pendingSocketSuccessAction = onSuccess;
            socketClient.sendBid(auctionId, getCurrentUsername(), amount);
            return true;
        } catch (Exception e) {
            pendingSocketSuccessAction = null;
            setBidMessage("Socket lỗi: " + e.getMessage());
            return false;
        }
    }

    private boolean sendAutoBidViaSocket(
            int auctionId, BigDecimal maxBid, BigDecimal increment, Runnable onSuccess
    ) {
        if (socketClient == null || !socketClient.isConnected()) return false;

        try {
            setBidMessage("Đang gửi Auto-Bid...");
            pendingSocketSuccessAction = onSuccess;
            socketClient.sendAutoBid(auctionId, getCurrentUsername(), maxBid, increment);
            return true;
        } catch (Exception e) {
            pendingSocketSuccessAction = null;
            setBidMessage("Socket lỗi: " + e.getMessage());
            return false;
        }
    }

    // =========================
    // DB ACTION
    // =========================

    private void runDatabaseAction(
            Supplier<RealtimeAuctionRepository.BidResponse> action,
            Runnable onSuccess
    ) {
        setBidMessage("Đang xử lý...");
        startRealtimeRefresh();

        refreshExecutor.execute(() -> {
            RealtimeAuctionRepository.BidResponse response = action.get();
            Platform.runLater(() -> {
                setBidMessage(response.message());
                if (response.success()) onSuccess.run();
            });
            if (response.success()) refreshRealtimeDataInBackground();
        });
    }

    // =========================
    // HELPER
    // =========================

    private boolean isBidderRole() {
        User current = appContext.getCurrentUser();
        if (current == null || current.getRole() == null) return false;
        if (!"BIDDER".equalsIgnoreCase(current.getRole().name())) {
            setBidMessage("Tài khoản Seller/Admin không thể tham gia đặt bid!");
            return false;
        }
        return true;
    }

    private Integer getSelectedAuctionId() {
        AuctionView selected = auctionTable == null
                ? null
                : auctionTable.getSelectionModel().getSelectedItem();

        Integer id = parseAuctionId(selected);
        if (id != null) {
            selectedAuctionIdForBid = id;
            return id;
        }
        if (selectedAuctionIdForBid != null) return selectedAuctionIdForBid;
        return parseAuctionId(selectedAuctionForBid);
    }

    private String getCurrentUsername() {
        User current = appContext.getCurrentUser();
        return current == null ? null : current.getUsername();
    }

    private BigDecimal parseMoney(String raw) {
        if (raw == null || raw.isBlank()) throw new NumberFormatException("empty");
        return new BigDecimal(raw.trim().replace(".", "").replace(",", ""));
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "";
        return currencyFormat.format(amount) + " VNĐ";
    }

    private String formatDateTime(String rawDateTime) {
        if (rawDateTime == null || rawDateTime.isBlank()) return "";
        try {
            String normalized = rawDateTime.replace(' ', 'T');
            if (normalized.length() > 19) normalized = normalized.substring(0, 19);
            LocalDateTime dateTime = LocalDateTime.parse(
                    normalized, DateTimeFormatter.ISO_LOCAL_DATE_TIME
            );
            return dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        } catch (Exception e) {
            return rawDateTime;
        }
    }

    private void setBidMessage(String message) {
        if (bidMessageLabel != null) {
            bidMessageLabel.setText(message);
        } else {
            showAlert(message);
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private <S, T> void centerColumn(TableColumn<S, T> column) {
        column.setCellFactory(tc -> {
            TableCell<S, T> cell = new TableCell<>() {
                @Override
                protected void updateItem(T item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.toString());
                }
            };
            cell.setAlignment(Pos.CENTER);
            return cell;
        });
    }

    private void stopRealtimeRefresh() {
        if (refreshExecutor != null) {
            refreshExecutor.shutdownNow();
            refreshExecutor = null;
        }
        if (socketClient != null) {
            socketClient.close();
            socketClient = null;
        }
    }

    private void openAllTabsForDemo() {
        if (managementTabPane == null) return;
        for (Tab tab : managementTabPane.getTabs()) {
            tab.setDisable(false);
            tab.setStyle("-fx-opacity: 1;");
        }
    }

    // =========================
    // SELLER ACTIONS  ← THAY THẾ HOÀN TOÀN 3 HÀM CŨ
    // =========================

    @FXML
    private void handleCreateItem() {

        String name        = sellerItemNameField == null ? "" : sellerItemNameField.getText().trim();
        String category    = itemTypeChoiceBox   == null ? "Khác" : itemTypeChoiceBox.getValue();
        String description = sellerItemDescriptionArea == null ? "" : sellerItemDescriptionArea.getText().trim();
        String priceText   = sellerStartingPriceField  == null ? "" : sellerStartingPriceField.getText().trim();
        String startText   = sellerStartTimeField == null ? "" : sellerStartTimeField.getText().trim();
        String endText     = sellerEndTimeField   == null ? "" : sellerEndTimeField.getText().trim();
        String metadata    = sellerMetadataArea   == null ? "" : sellerMetadataArea.getText().trim();
        String username    = getCurrentUsername();

        if (name.isEmpty())      { setSellerMessage("Vui lòng nhập tên sản phẩm!", false); return; }
        if (priceText.isEmpty()) { setSellerMessage("Vui lòng nhập giá mở phiên!", false); return; }

        BigDecimal price;
        try {
            price = new BigDecimal(priceText.replace(".", "").replace(",", ""));
        } catch (NumberFormatException e) {
            setSellerMessage("Giá không hợp lệ!", false);
            return;
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        LocalDateTime startTime, endTime;
        try {
            startTime = startText.isEmpty() ? LocalDateTime.now()             : LocalDateTime.parse(startText, fmt);
            endTime   = endText.isEmpty()   ? LocalDateTime.now().plusDays(30) : LocalDateTime.parse(endText,   fmt);
        } catch (Exception e) {
            setSellerMessage("Định dạng thời gian: yyyy-MM-dd HH:mm", false);
            return;
        }

        setSellerMessage("Đang tạo...", true);

        refreshExecutor.execute(() -> {
            JdbcItemRepository.CreateResult result = itemRepository.createItemAndAuction(
                    name, category, description, price, username, metadata, startTime, endTime
            );
            Platform.runLater(() -> {
                setSellerMessage(result.message(), result.success());
                if (result.success()) {
                    clearSellerForm();
                    refreshSellerTable();
                    lastAuctionTableHash = Integer.MIN_VALUE;
                    submitRefreshNow();
                }
            });
        });
    }

    @FXML
    private void handleUpdateItem() {

        if (selectedItemId == null) {
            setSellerMessage("Vui lòng chọn sản phẩm cần cập nhật!", false);
            return;
        }

        String name        = sellerItemNameField == null ? "" : sellerItemNameField.getText().trim();
        String category    = itemTypeChoiceBox   == null ? "Khác" : itemTypeChoiceBox.getValue();
        String description = sellerItemDescriptionArea == null ? "" : sellerItemDescriptionArea.getText().trim();
        String priceText   = sellerStartingPriceField  == null ? "" : sellerStartingPriceField.getText().trim();
        String startText   = sellerStartTimeField == null ? "" : sellerStartTimeField.getText().trim();
        String endText     = sellerEndTimeField   == null ? "" : sellerEndTimeField.getText().trim();
        String metadata    = sellerMetadataArea   == null ? "" : sellerMetadataArea.getText().trim();

        if (name.isEmpty()) { setSellerMessage("Vui lòng nhập tên sản phẩm!", false); return; }

        BigDecimal price;
        try {
            price = new BigDecimal(priceText.replace(".", "").replace(",", ""));
        } catch (NumberFormatException e) {
            setSellerMessage("Giá không hợp lệ!", false);
            return;
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        LocalDateTime startTime = null, endTime = null;
        try {
            if (!startText.isEmpty()) startTime = LocalDateTime.parse(startText, fmt);
            if (!endText.isEmpty())   endTime   = LocalDateTime.parse(endText,   fmt);
        } catch (Exception e) {
            setSellerMessage("Định dạng thời gian: yyyy-MM-dd HH:mm", false);
            return;
        }

        int itemId = selectedItemId;
        LocalDateTime finalStart = startTime;
        LocalDateTime finalEnd   = endTime;

        setSellerMessage("Đang cập nhật...", true);

        refreshExecutor.execute(() -> {
            JdbcItemRepository.CreateResult result = itemRepository.updateItemAndAuction(
                    itemId, name, category, description, price, metadata, finalStart, finalEnd
            );
            Platform.runLater(() -> {
                setSellerMessage(result.message(), result.success());
                if (result.success()) {
                    clearSellerForm();
                    refreshSellerTable();
                    lastAuctionTableHash = Integer.MIN_VALUE;
                    submitRefreshNow();
                }
            });
        });
    }

    @FXML
    private void handleDeleteItem() {

        if (selectedItemId == null) {
            setSellerMessage("Vui lòng chọn sản phẩm cần xóa!", false);
            return;
        }

        int itemId = selectedItemId;
        setSellerMessage("Đang xóa...", true);

        refreshExecutor.execute(() -> {
            JdbcItemRepository.CreateResult result = itemRepository.deleteItem(itemId);
            Platform.runLater(() -> {
                setSellerMessage(result.message(), result.success());
                if (result.success()) {
                    clearSellerForm();
                    refreshSellerTable();
                    lastAuctionTableHash = Integer.MIN_VALUE;
                    submitRefreshNow();
                }
            });
        });
    }

    // =========================
    // ADMIN
    // =========================

    @FXML
    private void handleApproveUser() {
        System.out.println("handleApproveUser");
    }

    @FXML
    private void handleLockUser() {
        System.out.println("handleLockUser");
    }

    @FXML
    private void handleRemoveAuction() {
        System.out.println("handleRemoveAuction");
    }

    @FXML
    private void handleCloseApp() {
        stopRealtimeRefresh();
        System.exit(0);
    }
}