import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class AuctionApp extends Application {

    @Override
    public void start(Stage stage) {
        showMainMenu(stage);
    }

    // ===== MENU CHÍNH =====
    public void showMainMenu(Stage stage) {
        Label title = new Label("He thong dau gia truc tuyen");

        Button auctionListBtn = new Button("Danh sach phien dau gia");
        Button productDetailBtn = new Button("Chi tiet san pham");
        Button liveBidBtn = new Button("Dau gia truc tiep");
        Button manageProductBtn = new Button("Quan ly san pham");

        auctionListBtn.setOnAction(e -> showAuctionList(stage));
        productDetailBtn.setOnAction(e -> showProductDetail(stage));
        liveBidBtn.setOnAction(e -> showBidScreen(stage));
        manageProductBtn.setOnAction(e -> showManageProduct(stage));

        VBox root = new VBox(10);
        root.getChildren().addAll(title, auctionListBtn, productDetailBtn, liveBidBtn, manageProductBtn);

        stage.setScene(new Scene(root, 400, 250));
        stage.setTitle("Auction System");
        stage.show();
    }

    // ===== DANH SÁCH PHIÊN =====
    public void showAuctionList(Stage stage) {
        Label label = new Label("Danh sach phien dau gia");

        ListView<String> list = new ListView<>();
        list.getItems().addAll("Laptop - 1200$", "Iphone - 900$");

        Button back = new Button("Quay lai");
        back.setOnAction(e -> showMainMenu(stage));

        VBox root = new VBox(10, label, list, back);
        stage.setScene(new Scene(root, 400, 300));
    }

    // ===== CHI TIẾT SẢN PHẨM =====
    public void showProductDetail(Stage stage) {
        Label label = new Label("Chi tiet san pham");

        Label info = new Label("Ten: Laptop\nGia: 1200$\nSeller: A");

        Button back = new Button("Quay lai");
        back.setOnAction(e -> showMainMenu(stage));

        VBox root = new VBox(10, label, info, back);
        stage.setScene(new Scene(root, 400, 300));
    }

    // ===== ĐẤU GIÁ =====
    public void showBidScreen(Stage stage) {
        Label label = new Label("Dat gia");

        TextField priceInput = new TextField();
        priceInput.setPromptText("Nhap gia");

        Button bidBtn = new Button("Dat gia");

        Label result = new Label();

        bidBtn.setOnAction(e -> {
            result.setText("Ban da dat gia: " + priceInput.getText());
        });

        Button back = new Button("Quay lai");
        back.setOnAction(e -> showMainMenu(stage));

        VBox root = new VBox(10, label, priceInput, bidBtn, result, back);
        stage.setScene(new Scene(root, 400, 300));
    }

    // ===== QUẢN LÝ SẢN PHẨM =====
    public void showManageProduct(Stage stage) {
        Label label = new Label("Quan ly san pham");

        Button addBtn = new Button("Them san pham");
        Button deleteBtn = new Button("Xoa san pham");

        Button back = new Button("Quay lai");
        back.setOnAction(e -> showMainMenu(stage));

        VBox root = new VBox(10, label, addBtn, deleteBtn, back);
        stage.setScene(new Scene(root, 400, 300));
    }

    public static void main(String[] args) {
        launch();
    }
}