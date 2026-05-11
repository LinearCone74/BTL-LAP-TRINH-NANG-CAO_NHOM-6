package com.auction.controller;

import com.auction.app.AppContext;
import com.auction.model.auction.AuctionView;
import com.auction.model.auction.BidTransaction;
import com.auction.model.user.User;
import com.auction.repository.JdbcAuctionRepository;
import java.math.BigDecimal;
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

    public DashboardController(AppContext appContext) {
        this.appContext = appContext;
    }

    @FXML
    public void initialize() {
        openAllTabsForDemo();

        setupPriceChart();

        if (auctionTable != null) {
            setupAuctionTable();
            loadAuctionTable();

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

    private void addRealtimeBidToChart(BigDecimal price) {
        if (priceSeries == null || price == null) {
            return;
        }

        int nextIndex = priceSeries.getData().size() + 1;

        priceSeries.getData().add(
                new XYChart.Data<>(
                        nextIndex,
                        price
                )
        );
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

            selectedAuction.setCurrentPrice(bidAmount);

            auctionTable.refresh();
            manualBidField.clear();

            addRealtimeBidToChart(bidAmount);

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
                new PropertyValueFactory<>("endTime")
        );
    }

    private void loadAuctionTable() {
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