package com.auction.controller;

import com.auction.app.AppContext;
import com.auction.model.auction.AuctionView;
import com.auction.model.user.User;
import com.auction.repository.JdbcAuctionRepository;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.text.NumberFormat;
import java.util.Locale;

public class DashboardController {

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

    @FXML
    private Label welcomeLabel;

    @FXML
    private TabPane mainTabPane;

    @FXML
    private TabPane managementTabPane;

    private final AppContext appContext;

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

        colAuctionCurrentPrice.setCellFactory(column -> new TableCell<AuctionView, Double>() {
            private final NumberFormat vndFormat =
                    NumberFormat.getNumberInstance(new Locale("vi", "VN"));

            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);

                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(vndFormat.format(price) + " đ");
                }
            }
        });

        colAuctionStatus.setCellValueFactory(
                new PropertyValueFactory<>("status")
        );

        colAuctionEndTime.setCellValueFactory(
                new PropertyValueFactory<>("endTime")
        );
    }

    private void loadAuctionTable() {
        JdbcAuctionRepository repo = new JdbcAuctionRepository();

        auctionTable.setItems(
                FXCollections.observableArrayList(
                        repo.findAll()
                )
        );
    }

    private void openAllTabsForDemo() {
        if (managementTabPane != null) {
            for (Tab tab : managementTabPane.getTabs()) {
                tab.setDisable(false);
                tab.setStyle("-fx-opacity: 1;");
            }
        }

        if (mainTabPane != null) {
            for (Tab tab : mainTabPane.getTabs()) {
                tab.setDisable(false);
                tab.setStyle("-fx-opacity: 1;");
            }
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
        loadAuctionTable();
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

    @FXML
    private void handleCloseApp() {
        System.exit(0);
    }
}