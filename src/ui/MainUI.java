package ui;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.application.Application;
public class MainUI {

    public class MainUI extends Application {

        BorderPane root = new BorderPane();

        @Override
        public void start(Stage stage) {

            // Menu trái
            VBox menu = new VBox(10);
            Button btnList = new Button("Danh sách đấu giá");
            Button btnDetail = new Button("Chi tiết sản phẩm");
            Button btnBid = new Button("Đấu giá");
            Button btnManage = new Button("Quản lý sản phẩm");

            menu.getChildren().addAll(btnList, btnDetail, btnBid, btnManage);

            // Mặc định
            root.setCenter(new Label("Chọn chức năng bên trái"));

            // Event chuyển màn hình
            btnList.setOnAction(e -> root.setCenter(AuctionListUI.getUI()));
            btnDetail.setOnAction(e -> root.setCenter(ProductDetailUI.getUI()));
            btnBid.setOnAction(e -> root.setCenter(BiddingUI.getUI()));
            btnManage.setOnAction(e -> root.setCenter(ManageProductUI.getUI()));

            root.setLeft(menu);

            Scene scene = new Scene(root, 800, 500);
            stage.setScene(scene);
            stage.setTitle("Auction System");
            stage.show();
        }

        public static void main(String[] args) {
            launch();
        }
    }
}
