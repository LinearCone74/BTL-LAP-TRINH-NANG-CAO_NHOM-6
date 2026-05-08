package com.auction.controller;

import com.auction.app.AppContext;
import com.auction.exception.AuctionException;
import com.auction.model.auction.Auction;
import com.auction.model.auction.AutoBidConfig;
import com.auction.model.auction.BidResult;
import com.auction.model.auction.BidTransaction;
import com.auction.model.item.Item;
import com.auction.model.user.Bidder;
import com.auction.model.user.Role;
import com.auction.model.user.Seller;
import com.auction.model.user.User;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardController {
    private static final DateTimeFormatter UI_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final AppContext appContext;

    @FXML private Label welcomeLabel;

    @FXML private TextField searchField;
    @FXML private ChoiceBox<String> statusFilterChoiceBox;

    @FXML private TableView<Auction> auctionTable;
    @FXML private TableColumn<Auction, String> colAuctionItem;
    @FXML private TableColumn<Auction, String> colAuctionSeller;
    @FXML private TableColumn<Auction, String> colAuctionCurrentPrice;
    @FXML private TableColumn<Auction, String> colAuctionStatus;
    @FXML private TableColumn<Auction, String> colAuctionEndTime;

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

    @FXML private TableView<BidTransaction> bidHistoryTable;
    @FXML private TableColumn<BidTransaction, String> colBidTime;
    @FXML private TableColumn<BidTransaction, String> colBidder;
    @FXML private TableColumn<BidTransaction, String> colBidAmount;
    @FXML private TableColumn<BidTransaction, String> colBidType;

    @FXML private LineChart<Number, Number> priceChart;
    @FXML private NumberAxis xAxis;
    @FXML private NumberAxis yAxis;

    @FXML private TabPane managementTabPane;

    @FXML private ChoiceBox<String> itemTypeChoiceBox;
    @FXML private TextField sellerItemNameField;
    @FXML private TextField sellerStartingPriceField;
    @FXML private TextField sellerStartTimeField;
    @FXML private TextField sellerEndTimeField;
    @FXML private TextArea sellerItemDescriptionArea;
    @FXML private TextArea sellerMetadataArea;
    @FXML private Label sellerMessageLabel;

    @FXML private TableView<Item> sellerItemTable;
    @FXML private TableColumn<Item, String> colSellerItemName;
    @FXML private TableColumn<Item, String> colSellerItemType;
    @FXML private TableColumn<Item, String> colSellerItemPrice;

    @FXML private Label adminMessageLabel;
    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colFullName;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, String> colUserStatus;

    public DashboardController(AppContext appContext) {
        this.appContext = appContext;
    }

    @FXML
    public void initialize() {
        User current = appContext.getCurrentUser();

        welcomeLabel.setText(
                "Xin chào, "
                        + (current == null
                        ? "Guest"
                        : current.getFullName() + " (" + current.getRole() + ")")
        );

        statusFilterChoiceBox.getItems().setAll(
                "ALL",
                "OPEN",
                "RUNNING",
                "FINISHED",
                "PAID",
                "CANCELED"
        );
        statusFilterChoiceBox.setValue("ALL");

        itemTypeChoiceBox.getItems().setAll(
                "ELECTRONICS",
                "ART",
                "VEHICLE"
        );
        itemTypeChoiceBox.setValue("ELECTRONICS");

        configureTables();
        configureRoleAccess(current);
        handleRefresh();
    }

    private void configureTables() {
        colAuctionItem.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getItem().getName()));

        colAuctionSeller.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getItem().getSeller().getUsername()
                ));

        colAuctionCurrentPrice.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getCurrentPrice().toPlainString()
                ));

        colAuctionStatus.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getStatus().name()));

        colAuctionEndTime.setCellValueFactory(data ->
                new SimpleStringProperty(format(data.getValue().getEndTime())));

        auctionTable.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldValue, auction) ->
                        showAuctionDetails(auction)
                );

        colBidTime.setCellValueFactory(data ->
                new SimpleStringProperty(format(data.getValue().getBidTime())));

        colBidder.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getBidder().getUsername()
                ));

        colBidAmount.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getAmount().toPlainString()
                ));

        colBidType.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().isAutoBid() ? "AUTO" : "MANUAL"
                ));

        colSellerItemName.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getName()));

        colSellerItemType.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getCategory()));

        colSellerItemPrice.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getStartingPrice().toPlainString()
                ));

        colUsername.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getUsername()));

        colFullName.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getFullName()));

        colEmail.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getEmail()));

        colRole.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getRole().name()));

        colUserStatus.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().isActive()
                                ? "ACTIVE"
                                : "LOCKED/PENDING"
                ));
    }

    private void configureRoleAccess(User current) {
        if (current == null || managementTabPane.getTabs().size() < 2) {
            return;
        }

        Tab sellerTab = managementTabPane.getTabs().get(0);
        Tab adminTab = managementTabPane.getTabs().get(1);

        sellerTab.setDisable(
                current.getRole() != Role.SELLER
                        && current.getRole() != Role.ADMIN
        );

        adminTab.setDisable(current.getRole() != Role.ADMIN);
    }

    @FXML
    private void handleRefresh() {
        handleFilterAuctions();
        loadSellerItems();
        loadUsers();
    }

    @FXML
    private void handleLogout() {
        appContext.setCurrentUser(null);
        appContext.getNavigator().showLogin();
    }

    @FXML
    private void handleFilterAuctions() {
        String keyword = text(searchField).toLowerCase();
        String status = statusFilterChoiceBox.getValue();

        List<Auction> auctions = appContext
                .getAuctionService()
                .getAllAuctions()
                .stream()
                .filter(auction ->
                        keyword.isBlank()
                                || auction.getItem()
                                .getName()
                                .toLowerCase()
                                .contains(keyword)
                )
                .filter(auction ->
                        "ALL".equals(status)
                                || auction.getStatus().name().equals(status)
                )
                .toList();

        auctionTable.setItems(FXCollections.observableArrayList(auctions));
    }

    @FXML
    private void handlePlaceBid() {
        Auction selected = auctionTable.getSelectionModel().getSelectedItem();
        User user = appContext.getCurrentUser();

        if (!(user instanceof Bidder bidder)) {
            setBidMessage(false, "Chỉ Bidder được đặt giá.");
            return;
        }

        if (selected == null) {
            setBidMessage(false, "Vui lòng chọn phiên đấu giá.");
            return;
        }

        try {
            BidResult result = appContext
                    .getAuctionService()
                    .placeManualBid(
                            selected.getId(),
                            bidder,
                            new BigDecimal(text(manualBidField))
                    );

            setBidMessage(result.success(), result.message());

            appContext.getAutoBidService().process(selected);

            showAuctionDetails(selected);
            handleFilterAuctions();

        } catch (AuctionException | NumberFormatException ex) {
            setBidMessage(false, ex.getMessage());
        }
    }

    @FXML
    private void handleRegisterAutoBid() {
        Auction selected = auctionTable.getSelectionModel().getSelectedItem();
        User user = appContext.getCurrentUser();

        if (!(user instanceof Bidder bidder)) {
            setBidMessage(false, "Chỉ Bidder được đăng ký Auto-Bid.");
            return;
        }

        if (selected == null) {
            setBidMessage(false, "Vui lòng chọn phiên đấu giá.");
            return;
        }

        try {
            AutoBidConfig config = new AutoBidConfig(
                    bidder,
                    new BigDecimal(text(autoBidMaxField)),
                    new BigDecimal(text(autoBidIncrementField))
            );

            appContext.getAutoBidService().register(selected, config);
            setBidMessage(true, "Đã đăng ký Auto-Bid.");

        } catch (AuctionException | NumberFormatException ex) {
            setBidMessage(false, ex.getMessage());
        }
    }

    @FXML
    private void handleCreateItem() {
        User user = appContext.getCurrentUser();

        if (!(user instanceof Seller seller)) {
            setSellerMessage(false, "Chỉ Seller được thêm sản phẩm.");
            return;
        }

        try {
            Item item = appContext.getItemService().create(
                    itemTypeChoiceBox.getValue(),
                    text(sellerItemNameField),
                    text(sellerItemDescriptionArea),
                    new BigDecimal(text(sellerStartingPriceField)),
                    seller,
                    parseMetadata(text(sellerMetadataArea))
            );

            Auction auction = appContext.getAuctionService().createAuction(
                    item,
                    LocalDateTime.parse(text(sellerStartTimeField), UI_TIME),
                    LocalDateTime.parse(text(sellerEndTimeField), UI_TIME)
            );

            setSellerMessage(
                    true,
                    "Đã tạo sản phẩm và phiên đấu giá: " + auction.getId()
            );

            handleRefresh();

        } catch (Exception ex) {
            setSellerMessage(false, ex.getMessage());
        }
    }

    @FXML
    private void handleUpdateItem() {
        Item item = sellerItemTable.getSelectionModel().getSelectedItem();

        if (item == null) {
            setSellerMessage(false, "Chọn sản phẩm cần cập nhật.");
            return;
        }

        item.setName(text(sellerItemNameField));
        item.setDescription(text(sellerItemDescriptionArea));
        item.setStartingPrice(new BigDecimal(text(sellerStartingPriceField)));

        appContext.getItemService().update(item);

        setSellerMessage(true, "Đã cập nhật sản phẩm.");
        handleRefresh();
    }

    @FXML
    private void handleDeleteItem() {
        Item item = sellerItemTable.getSelectionModel().getSelectedItem();

        if (item == null) {
            setSellerMessage(false, "Chọn sản phẩm cần xóa.");
            return;
        }

        appContext.getItemService().delete(item.getId());

        setSellerMessage(true, "Đã xóa sản phẩm.");
        handleRefresh();
    }

    @FXML
    private void handleApproveUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            setAdminMessage(false, "Chọn user cần duyệt.");
            return;
        }

        selected.activate();
        appContext.getUserService().save(selected);

        setAdminMessage(true, "Đã duyệt/mở khóa user.");
        loadUsers();
    }

    @FXML
    private void handleLockUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            setAdminMessage(false, "Chọn user cần khóa.");
            return;
        }

        selected.deactivate();
        appContext.getUserService().save(selected);

        setAdminMessage(true, "Đã khóa user.");
        loadUsers();
    }

    @FXML
    private void handleRemoveAuction() {
        Auction selected = auctionTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            setAdminMessage(false, "Chọn auction cần xóa/hủy.");
            return;
        }

        appContext.getAuctionService().cancelAuction(selected.getId());

        setAdminMessage(true, "Đã hủy auction.");
        handleRefresh();
    }

    private void showAuctionDetails(Auction auction) {
        if (auction == null) {
            return;
        }

        itemNameLabel.setText(auction.getItem().getName());
        itemDescriptionLabel.setText(auction.getItem().getDescription());
        startingPriceLabel.setText(
                auction.getItem().getStartingPrice().toPlainString()
        );
        currentPriceLabel.setText(auction.getCurrentPrice().toPlainString());
        leaderLabel.setText(
                auction.getHighestBidder() == null
                        ? "Chưa có"
                        : auction.getHighestBidder().getUsername()
        );
        startTimeLabel.setText(format(auction.getStartTime()));
        endTimeLabel.setText(format(auction.getEndTime()));
        auctionStatusLabel.setText(auction.getStatus().name());

        bidHistoryTable.setItems(
                FXCollections.observableArrayList(auction.getBidHistory())
        );

        drawChart(auction.getBidHistory());
    }

    private void drawChart(List<BidTransaction> bids) {
        priceChart.getData().clear();

        XYChart.Series<Number, Number> series = new XYChart.Series<>();

        for (int i = 0; i < bids.size(); i++) {
            series.getData().add(
                    new XYChart.Data<>(i + 1, bids.get(i).getAmount())
            );
        }

        priceChart.getData().add(series);
    }

    private void loadSellerItems() {
        User current = appContext.getCurrentUser();

        List<Item> items = appContext
                .getItemService()
                .getAll()
                .stream()
                .filter(item ->
                        current == null
                                || current.getRole() == Role.ADMIN
                                || item.getSeller().getId().equals(current.getId())
                )
                .toList();

        sellerItemTable.setItems(FXCollections.observableArrayList(items));
    }

    private void loadUsers() {
        userTable.setItems(
                FXCollections.observableArrayList(
                        appContext.getUserService().getAllUsers()
                )
        );
    }

    private Map<String, Object> parseMetadata(String raw) {
        Map<String, Object> map = new HashMap<>();

        if (raw == null || raw.isBlank()) {
            return map;
        }

        for (String part : raw.split(";")) {
            String[] pair = part.split("=", 2);

            if (pair.length == 2) {
                map.put(pair[0].trim(), pair[1].trim());
            }
        }

        return map;
    }

    private String text(TextInputControl control) {
        return control == null || control.getText() == null
                ? ""
                : control.getText().trim();
    }

    private String format(LocalDateTime time) {
        return time == null ? "" : time.format(UI_TIME);
    }

    private void setBidMessage(boolean ok, String message) {
        bidMessageLabel.getStyleClass().setAll(
                ok ? "success-label" : "error-label"
        );
        bidMessageLabel.setText(message);
    }

    private void setSellerMessage(boolean ok, String message) {
        sellerMessageLabel.getStyleClass().setAll(
                ok ? "success-label" : "error-label"
        );
        sellerMessageLabel.setText(message);
    }

    private void setAdminMessage(boolean ok, String message) {
        adminMessageLabel.getStyleClass().setAll(
                ok ? "success-label" : "error-label"
        );
        adminMessageLabel.setText(message);
    }
}