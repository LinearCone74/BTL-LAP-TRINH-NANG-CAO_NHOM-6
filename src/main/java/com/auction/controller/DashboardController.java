package com.auction.controller;


import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.TableCell;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import com.auction.app.AppContext;
import com.auction.model.auction.AuctionView;
import com.auction.model.user.User;
import com.auction.repository.JdbcAuctionRepository;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.math.BigDecimal;

public class DashboardController {
    @FXML
    private TabPane mainTabPane;

    @FXML
    private Tab bidRealtimeTab;

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
    private Label statusLabel;

    private AuctionView selectedAuctionForBid;

    @FXML
    private TextField manualBidField;

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

    @FXML
    private Label welcomeLabel;

    @FXML
    private TabPane managementTabPane;

    private final AppContext appContext;

    public DashboardController(AppContext appContext) {
        this.appContext = appContext;
    }

    private void updateBidRealtimeTab(AuctionView auction) {
        if (auction == null) {
            return;
        }

        itemNameLabel.setText(auction.getTitle());

        itemDescriptionLabel.setText(
                auction.getDescription() == null ? "" : auction.getDescription()
        );

        // Nếu AuctionView chưa có startingPrice thì để tạm bằng giá hiện tại
        startingPriceLabel.setText(formatMoney(auction.getCurrentPrice()));

        currentPriceLabel.setText(formatMoney(auction.getCurrentPrice()));

        // Nếu chưa có người dẫn đầu thì hiển thị tạm
        leaderLabel.setText("Chưa có");

        // Nếu chưa có startTime thì để trống
        startTimeLabel.setText("");

        endTimeLabel.setText(formatDateTime(auction.getEndTimeText()));

        statusLabel.setText(auction.getStatus().toString());
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) {
            return "";
        }

        NumberFormat formatter =
                NumberFormat.getNumberInstance(new Locale("vi", "VN"));

        return formatter.format(amount) + " VNĐ";
    }

    private String formatDateTime(String rawDateTime) {
        if (rawDateTime == null || rawDateTime.isBlank()) {
            return "";
        }

        try {
            DateTimeFormatter inputFormatter =
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME;

            DateTimeFormatter outputFormatter =
                    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            LocalDateTime dateTime =
                    LocalDateTime.parse(rawDateTime, inputFormatter);

            return dateTime.format(outputFormatter);
        } catch (Exception e) {
            return rawDateTime;
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

            updateBidRealtimeTab(selectedAuctionForBid);

            mainTabPane.getSelectionModel().select(bidRealtimeTab);
        });
    }

    @FXML
    public void initialize() {
        setupAuctionTableClick();
        openAllTabsForDemo();

        if (auctionTable != null) {
            setupAuctionTable();
            loadAuctionTable();

            auctionTable.getSelectionModel().selectedItemProperty().addListener(
                    (obs, oldSelection, newSelection) -> {
                        if (newSelection != null) {
                            showAuctionDetail(newSelection);
                        }
                    }
            );
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
        loadAuctionTable();
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
        AuctionView selectedAuction =
                auctionTable.getSelectionModel().getSelectedItem();

        if (selectedAuction == null) {
            showAlert("Vui lòng chọn một phiên đấu giá!");
            return;
        }

        String bidText = manualBidField.getText();

        if (bidText == null || bidText.isBlank()) {
            showAlert("Vui lòng nhập số tiền bid!");
            return;
        }

        try {
            BigDecimal bidAmount = new BigDecimal(bidText.trim());

            if (selectedAuction.getCurrentPrice() != null
                    && bidAmount.compareTo(selectedAuction.getCurrentPrice()) <= 0) {
                showAlert("Giá bid phải cao hơn giá hiện tại!");
                return;
            }

            selectedAuction.setCurrentPrice(bidAmount);
            auctionTable.refresh();
            manualBidField.clear();

            showAlert("Đặt bid thành công!");

        } catch (NumberFormatException e) {
            showAlert("Số tiền bid không hợp lệ!");
        }
    }

    private void showAuctionDetail(AuctionView auction) {
        System.out.println("Đã chọn phiên: " + auction.getTitle());
        showAlert("Đã chọn phiên đấu giá: " + auction.getTitle());
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
        colAuctionItem.setCellValueFactory(
                new PropertyValueFactory<>("title")
        );

        colAuctionSeller.setCellValueFactory(
                new PropertyValueFactory<>("sellerName")
        );

        NumberFormat currencyFormat =
                NumberFormat.getNumberInstance(new Locale("vi", "VN"));

        colAuctionCurrentPrice.setCellValueFactory(cellData -> {
            BigDecimal price = cellData.getValue().getCurrentPrice();

            if (price == null) {
                return new SimpleStringProperty("");
            }

            return new SimpleStringProperty(currencyFormat.format(price) + " VNĐ");
        });

        colAuctionStatus.setCellValueFactory(
                new PropertyValueFactory<>("status")
        );
        centerColumn(colAuctionItem);
        centerColumn(colAuctionSeller);
        centerColumn(colAuctionCurrentPrice);
        centerColumn(colAuctionStatus);
        centerColumn(colAuctionEndTime);

        DateTimeFormatter inputFormatter =
                DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        DateTimeFormatter outputFormatter =
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        colAuctionEndTime.setCellValueFactory(cellData -> {
            String rawEndTime = cellData.getValue().getEndTimeText();

            if (rawEndTime == null || rawEndTime.isBlank()) {
                return new SimpleStringProperty("");
            }

            try {
                LocalDateTime parsedTime =
                        LocalDateTime.parse(rawEndTime, inputFormatter);

                return new SimpleStringProperty(parsedTime.format(outputFormatter));
            } catch (Exception e) {
                return new SimpleStringProperty(rawEndTime);
            }
        });
    }

    private void loadAuctionTable() {
        JdbcAuctionRepository repo = new JdbcAuctionRepository();

        auctionTable.setItems(
                FXCollections.observableArrayList(
                        repo.findAll()
                )
        );
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private <T> void centerColumn(TableColumn<AuctionView, T> column) {
        column.setCellFactory(tc -> {
            TableCell<AuctionView, T> cell = new TableCell<>() {
                @Override
                protected void updateItem(T item, boolean empty) {
                    super.updateItem(item, empty);

                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item.toString());
                    }
                }
            };

            cell.setAlignment(Pos.CENTER);
            return cell;
        });
    }
}