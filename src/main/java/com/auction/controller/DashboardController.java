package com.auction.controller;

import com.auction.app.AppContext;
import com.auction.model.auction.AuctionView;
import com.auction.model.user.User;
import com.auction.repository.JdbcBidRepository;
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

import java.math.BigDecimal;

import java.text.NumberFormat;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
    private Tab sellerTab; // Thêm khai báo Tab Seller

    @FXML
    private Tab adminTab; // Thêm khai báo Tab Admin

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

    @FXML
    private TableView<?> sellerItemTable;

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

        // Đã tắt hàm hiển thị tất cả Tab để áp dụng phân quyền
        // openAllTabsForDemo();

        setupAuctionTable();

        setupBidHistoryTable();

        setupAuctionTableClick();

        setupChoiceBoxes();

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

            // Gọi hàm phân quyền
            applyRolePermissions();
        }
    }

    // =========================
    // PHÂN QUYỀN
    // =========================

    private void applyRolePermissions() {
        User current = appContext.getCurrentUser();
        if (current == null || mainTabPane == null) return;

        // Dùng name() để chuyển enum thành String an toàn
        String role = current.getRole() != null ? current.getRole().name().toUpperCase() : "";

        // Mặc định ẩn các tab quản trị
        if (sellerTab != null) mainTabPane.getTabs().remove(sellerTab);
        if (adminTab != null) mainTabPane.getTabs().remove(adminTab);

        // Hiển thị tab tùy theo vai trò
        if ("SELLER".equals(role) && sellerTab != null) {
            mainTabPane.getTabs().add(sellerTab);
        } else if ("ADMIN".equals(role) && adminTab != null) {
            mainTabPane.getTabs().add(adminTab);
        }

        // ==========================================
        // KHÓA CHỨC NĂNG ĐẤU GIÁ NẾU KHÔNG PHẢI BIDDER
        // ==========================================
        boolean isBidder = "BIDDER".equals(role);

        if (manualBidField != null) manualBidField.setDisable(!isBidder);
        if (autoBidMaxField != null) autoBidMaxField.setDisable(!isBidder);
        if (autoBidIncrementField != null) autoBidIncrementField.setDisable(!isBidder);

        if (!isBidder && bidMessageLabel != null) {
            bidMessageLabel.setText("Tài khoản Seller/Admin không thể đặt bid.");
            bidMessageLabel.setStyle("-fx-text-fill: #e53e3e; -fx-font-weight: bold;"); // Cảnh báo chữ đỏ
        }
    }

    // =========================
    // SETUP
    // =========================

    private void setupChoiceBoxes() {

        if (statusFilterChoiceBox != null) {

            statusFilterChoiceBox.setItems(
                    FXCollections.observableArrayList(
                            "Tất cả",
                            "OPEN",
                            "RUNNING",
                            "ENDED"
                    )
            );

            statusFilterChoiceBox.setValue("Tất cả");
        }

        if (itemTypeChoiceBox != null) {

            itemTypeChoiceBox.setItems(
                    FXCollections.observableArrayList(
                            "Laptop",
                            "Điện thoại",
                            "Xe",
                            "Đồng hồ",
                            "Khác"
                    )
            );
        }
    }

    private void setupAuctionTableClick() {

        auctionTable.setOnMouseClicked(event -> {

            AuctionView selectedAuction =
                    auctionTable.getSelectionModel().getSelectedItem();

            if (selectedAuction == null) {
                return;
            }

            selectedAuctionForBid = selectedAuction;

            selectedAuctionIdForBid =
                    parseAuctionId(selectedAuction);

            lastBidHistoryVersion = Integer.MIN_VALUE;

            updateBidRealtimeTab(selectedAuctionForBid);

            loadBidHistoryAsync(selectedAuctionIdForBid, true);

            if (mainTabPane != null && bidRealtimeTab != null) {
                mainTabPane.getSelectionModel()
                        .select(bidRealtimeTab);
            }
        });
    }

    private void setupAuctionTable() {

        colAuctionItem.setCellValueFactory(
                new PropertyValueFactory<>("title")
        );

        colAuctionSeller.setCellValueFactory(
                new PropertyValueFactory<>("sellerName")
        );

        colAuctionCurrentPrice.setCellValueFactory(
                cellData ->
                        new SimpleStringProperty(
                                formatMoney(
                                        cellData.getValue().getCurrentPrice()
                                )
                        )
        );

        colAuctionStatus.setCellValueFactory(
                new PropertyValueFactory<>("status")
        );

        colAuctionEndTime.setCellValueFactory(
                cellData ->
                        new SimpleStringProperty(
                                formatDateTime(
                                        cellData.getValue().getEndTimeText()
                                )
                        )
        );

        centerColumn(colAuctionItem);
        centerColumn(colAuctionSeller);
        centerColumn(colAuctionCurrentPrice);
        centerColumn(colAuctionStatus);
        centerColumn(colAuctionEndTime);
    }

    private void setupBidHistoryTable() {

        if (bidHistoryTable == null) {
            return;
        }

        colBidTime.setCellValueFactory(
                cellData ->
                        new SimpleStringProperty(
                                formatDateTime(
                                        cellData.getValue().time()
                                )
                        )
        );

        colBidder.setCellValueFactory(
                cellData ->
                        new SimpleStringProperty(
                                cellData.getValue().bidder()
                        )
        );

        colBidAmount.setCellValueFactory(
                cellData ->
                        new SimpleStringProperty(
                                formatMoney(
                                        new BigDecimal(
                                                cellData.getValue().amount()
                                        )
                                )
                        )
        );

        colBidType.setCellValueFactory(
                cellData ->
                        new SimpleStringProperty(
                                cellData.getValue().type()
                        )
        );
    }

    // =========================
    // REALTIME
    // =========================

    private void startRealtimeRefresh() {

        if (refreshExecutor != null
                && !refreshExecutor.isShutdown()) {
            return;
        }

        refreshExecutor =
                Executors.newSingleThreadScheduledExecutor(r -> {

                    Thread thread =
                            new Thread(r, "auction-realtime-refresh");

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

        refreshExecutor.execute(
                this::refreshRealtimeDataInBackground
        );
    }

    private void refreshRealtimeDataInBackground() {

        if (!refreshInProgress.compareAndSet(false, true)) {
            return;
        }

        try {

            Integer selectedId = selectedAuctionIdForBid;

            List<AuctionView> auctions =
                    realtimeRepo.findAll();

            int tableHash =
                    calculateAuctionTableHash(auctions);

            boolean tableChanged =
                    tableHash != lastAuctionTableHash;

            AuctionView freshSelected = null;

            if (selectedId != null) {

                freshSelected =
                        findAuctionInList(
                                auctions,
                                selectedId
                        );

                if (freshSelected == null) {

                    freshSelected =
                            realtimeRepo.findById(selectedId);
                }
            }

            int historyVersion =
                    selectedId == null
                            ? lastBidHistoryVersion
                            : realtimeRepo.findLastBidId(selectedId);

            boolean historyChanged =
                    selectedId != null
                            && historyVersion != lastBidHistoryVersion;

            List<RealtimeAuctionRepository.BidHistoryRow> historyRows =
                    historyChanged
                            ? realtimeRepo.findBidHistory(selectedId)
                            : null;

            AuctionView finalFreshSelected = freshSelected;

            List<RealtimeAuctionRepository.BidHistoryRow>
                    finalHistoryRows = historyRows;

            Platform.runLater(() -> {

                if (tableChanged) {

                    lastAuctionTableHash = tableHash;

                    auctionTable.setItems(
                            FXCollections.observableArrayList(
                                    auctions
                            )
                    );

                    if (selectedId != null) {
                        selectAuctionInTable(selectedId);
                    }
                }

                if (finalFreshSelected != null) {

                    selectedAuctionForBid =
                            finalFreshSelected;

                    updateBidRealtimeTab(finalFreshSelected);
                }

                if (finalHistoryRows != null
                        && bidHistoryTable != null) {

                    lastBidHistoryVersion =
                            historyVersion;

                    bidHistoryTable.setItems(
                            FXCollections.observableArrayList(
                                    finalHistoryRows
                            )
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

    private void loadBidHistoryAsync(
            Integer auctionId,
            boolean force
    ) {

        if (auctionId == null
                || bidHistoryTable == null) {
            return;
        }

        startRealtimeRefresh();

        refreshExecutor.execute(() -> {

            int historyVersion =
                    realtimeRepo.findLastBidId(auctionId);

            if (!force
                    && historyVersion == lastBidHistoryVersion) {
                return;
            }

            List<RealtimeAuctionRepository.BidHistoryRow>
                    historyRows =
                    realtimeRepo.findBidHistory(auctionId);

            Platform.runLater(() -> {

                lastBidHistoryVersion =
                        historyVersion;

                bidHistoryTable.setItems(
                        FXCollections.observableArrayList(
                                historyRows
                        )
                );
            });
        });
    }

    // =========================
    // HASH
    // =========================

    private int calculateAuctionTableHash(
            List<AuctionView> auctions
    ) {

        int hash = 1;

        for (AuctionView auction : auctions) {

            hash = 31 * hash + Objects.hash(
                    auction.getId(),
                    auction.getTitle(),
                    auction.getSellerName(),
                    auction.getCurrentPrice(),
                    auction.getStatus(),
                    auction.getEndTimeText(),
                    auction.getHighestBidderName()
            );
        }

        return hash;
    }

    // =========================
    // FIND
    // =========================

    private AuctionView findAuctionInList(
            List<AuctionView> auctions,
            int auctionId
    ) {

        for (AuctionView auction : auctions) {

            Integer id = parseAuctionId(auction);

            if (id != null && id == auctionId) {
                return auction;
            }
        }

        return null;
    }

    private Integer parseAuctionId(AuctionView auction) {

        if (auction == null
                || auction.getId() == null) {
            return null;
        }

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

                auctionTable.getSelectionModel()
                        .select(auction);

                return;
            }
        }
    }

    // =========================
    // UPDATE UI
    // =========================

    private void updateBidRealtimeTab(AuctionView auction) {

        if (auction == null) {
            return;
        }

        itemNameLabel.setText(auction.getTitle());

        itemDescriptionLabel.setText(
                auction.getDescription() == null
                        ? ""
                        : auction.getDescription()
        );

        startingPriceLabel.setText(
                formatMoney(auction.getCurrentPrice())
        );

        currentPriceLabel.setText(
                formatMoney(auction.getCurrentPrice())
        );

        String leader =
                auction.getHighestBidderName();

        leaderLabel.setText(
                leader == null || leader.isBlank()
                        ? "Chưa có"
                        : leader
        );

        startTimeLabel.setText(
                auction.getStartTime() == null
                        ? ""
                        : formatDateTime(
                        auction.getStartTime().toString()
                )
        );

        endTimeLabel.setText(
                formatDateTime(
                        auction.getEndTimeText()
                )
        );

        auctionStatusLabel.setText(
                auction.getStatus().toString()
        );
    }

    // =========================
    // BUTTON EVENTS
    // =========================

    @FXML
    private void handleRefresh() {
        // Đã tắt hàm mở tab demo
        // openAllTabsForDemo();

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
        // Đã tắt hàm mở tab demo
        // openAllTabsForDemo();

        submitRefreshNow();
    }

    @FXML
    private void handlePlaceBid() {
        // Chặn quyền Seller / Admin
        if (!isBidderRole()) return;

        Integer auctionId =
                getSelectedAuctionId();

        if (auctionId == null) {

            setBidMessage(
                    "Vui lòng chọn một phiên đấu giá!"
            );

            return;
        }

        BigDecimal bidAmount;

        try {

            bidAmount =
                    parseMoney(manualBidField.getText());

        } catch (NumberFormatException e) {

            setBidMessage(
                    "Số tiền bid không hợp lệ!"
            );

            return;
        }

        if (sendBidViaSocket(
                auctionId,
                bidAmount,
                () -> manualBidField.clear()
        )) {
            return;
        }

        runDatabaseAction(
                () -> {
                    // Hứng kết quả trả về từ DB
                    RealtimeAuctionRepository.BidResponse response = realtimeRepo.placeBid(
                            auctionId,
                            getCurrentUsername(),
                            bidAmount
                    );

                    bidRepository.saveBid(
                            auctionId,
                            getCurrentUsername(),
                            bidAmount.doubleValue()
                    );

                    // Trả kết quả về thay vì null
                    return response;
                },
                () -> manualBidField.clear()
        );
    }

    @FXML
    private void handleRegisterAutoBid() {
        // Chặn quyền Seller / Admin
        if (!isBidderRole()) return;

        Integer auctionId =
                getSelectedAuctionId();

        if (auctionId == null) {

            setBidMessage(
                    "Vui lòng chọn một phiên đấu giá!"
            );

            return;
        }

        BigDecimal maxBid;
        BigDecimal increment;

        try {

            maxBid =
                    parseMoney(autoBidMaxField.getText());

            increment =
                    parseMoney(autoBidIncrementField.getText());

        } catch (NumberFormatException e) {

            setBidMessage(
                    "Max bid hoặc bước nhảy không hợp lệ!"
            );

            return;
        }

        Runnable clearFields = () -> {

            autoBidMaxField.clear();
            autoBidIncrementField.clear();
        };

        if (sendAutoBidViaSocket(
                auctionId,
                maxBid,
                increment,
                clearFields
        )) {
            return;
        }

        runDatabaseAction(
                () ->
                        realtimeRepo.registerAutoBid(
                                auctionId,
                                getCurrentUsername(),
                                maxBid,
                                increment
                        ),
                clearFields
        );
    }

    // =========================
    // SOCKET
    // =========================

    private void startSocketClient() {

        if (socketClient != null
                && socketClient.isConnected()) {
            return;
        }

        socketClient =
                new AuctionSocketClient(
                        SOCKET_HOST,
                        SOCKET_PORT
                );

        socketClient.setStatusListener(
                message ->
                        Platform.runLater(
                                () -> setBidMessage(message)
                        )
        );

        socketClient.setUpdateListener(auctionId -> {

            Integer selectedId =
                    selectedAuctionIdForBid;

            lastAuctionTableHash =
                    Integer.MIN_VALUE;

            if (selectedId != null
                    && selectedId == auctionId) {

                lastBidHistoryVersion =
                        Integer.MIN_VALUE;
            }

            submitRefreshNow();
        });

        socketClient.setResultListener(result ->
                Platform.runLater(() -> {

                    setBidMessage(result.message());

                    if (result.success()) {

                        Runnable successAction =
                                pendingSocketSuccessAction;

                        pendingSocketSuccessAction = null;

                        if (successAction != null) {
                            successAction.run();
                        }

                        lastAuctionTableHash =
                                Integer.MIN_VALUE;

                        lastBidHistoryVersion =
                                Integer.MIN_VALUE;

                        submitRefreshNow();
                    }
                }));

        socketClient.connect();
    }

    // =========================
    // SOCKET SEND
    // =========================

    private boolean sendBidViaSocket(
            int auctionId,
            BigDecimal amount,
            Runnable onSuccess
    ) {

        if (socketClient == null
                || !socketClient.isConnected()) {
            return false;
        }

        try {

            setBidMessage(
                    "Đang gửi bid qua socket server..."
            );

            pendingSocketSuccessAction =
                    onSuccess;

            socketClient.sendBid(
                    auctionId,
                    getCurrentUsername(),
                    amount
            );

            return true;

        } catch (Exception e) {

            pendingSocketSuccessAction = null;

            setBidMessage(
                    "Socket lỗi: "
                            + e.getMessage()
            );

            return false;
        }
    }

    private boolean sendAutoBidViaSocket(
            int auctionId,
            BigDecimal maxBid,
            BigDecimal increment,
            Runnable onSuccess
    ) {

        if (socketClient == null
                || !socketClient.isConnected()) {
            return false;
        }

        try {

            setBidMessage(
                    "Đang gửi Auto-Bid..."
            );

            pendingSocketSuccessAction =
                    onSuccess;

            socketClient.sendAutoBid(
                    auctionId,
                    getCurrentUsername(),
                    maxBid,
                    increment
            );

            return true;

        } catch (Exception e) {

            pendingSocketSuccessAction = null;

            setBidMessage(
                    "Socket lỗi: "
                            + e.getMessage()
            );

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

            RealtimeAuctionRepository.BidResponse response =
                    action.get();

            Platform.runLater(() -> {

                setBidMessage(response.message());

                if (response.success()) {
                    onSuccess.run();
                }
            });

            if (response.success()) {
                refreshRealtimeDataInBackground();
            }
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

        AuctionView selected =
                auctionTable == null
                        ? null
                        : auctionTable
                          .getSelectionModel()
                          .getSelectedItem();

        Integer id = parseAuctionId(selected);

        if (id != null) {

            selectedAuctionIdForBid = id;

            return id;
        }

        if (selectedAuctionIdForBid != null) {
            return selectedAuctionIdForBid;
        }

        return parseAuctionId(selectedAuctionForBid);
    }

    private String getCurrentUsername() {

        User current =
                appContext.getCurrentUser();

        return current == null
                ? null
                : current.getUsername();
    }

    private BigDecimal parseMoney(String raw) {

        if (raw == null || raw.isBlank()) {
            throw new NumberFormatException("empty");
        }

        return new BigDecimal(
                raw.trim()
                        .replace(".", "")
                        .replace(",", "")
        );
    }

    private String formatMoney(BigDecimal amount) {

        if (amount == null) {
            return "";
        }

        return currencyFormat.format(amount)
                + " VNĐ";
    }

    private String formatDateTime(String rawDateTime) {

        if (rawDateTime == null
                || rawDateTime.isBlank()) {
            return "";
        }

        try {

            String normalized =
                    rawDateTime.replace(' ', 'T');

            if (normalized.length() > 19) {
                normalized =
                        normalized.substring(0, 19);
            }

            LocalDateTime dateTime =
                    LocalDateTime.parse(
                            normalized,
                            DateTimeFormatter.ISO_LOCAL_DATE_TIME
                    );

            return dateTime.format(
                    DateTimeFormatter.ofPattern(
                            "dd/MM/yyyy HH:mm:ss"
                    )
            );

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

    private void openAllTabsForDemo() {

        if (managementTabPane == null) {
            return;
        }

        for (Tab tab : managementTabPane.getTabs()) {

            tab.setDisable(false);

            tab.setStyle("-fx-opacity: 1;");
        }
    }

    private void showAlert(String message) {

        Alert alert =
                new Alert(Alert.AlertType.INFORMATION);

        alert.setHeaderText(null);

        alert.setContentText(message);

        alert.showAndWait();
    }

    private <S, T> void centerColumn(
            TableColumn<S, T> column
    ) {

        column.setCellFactory(tc -> {

            TableCell<S, T> cell =
                    new TableCell<>() {

                        @Override
                        protected void updateItem(
                                T item,
                                boolean empty
                        ) {

                            super.updateItem(item, empty);

                            setText(
                                    empty || item == null
                                            ? null
                                            : item.toString()
                            );
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

    // =========================
    // SELLER / ADMIN
    // =========================

    @FXML
    private void handleCreateItem() {
        System.out.println("handleCreateItem");
    }

    @FXML
    private void handleUpdateItem() {
        System.out.println("handleUpdateItem");
    }

    @FXML
    private void handleDeleteItem() {
        System.out.println("handleDeleteItem");
    }

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