mport javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class AuctionApp extends Application {
    @Override
    public void start(Stage stage) {
        Label title = new Label("He thong dau gia truc tuyen");

        Button auctionListBtn = new Button("Danh sach phien dau gia");
        Button productDetailBtn = new Button("Chi tiet san pham");
        Button liveBidBtn = new Button("Dau gia truc tiep");
        Button manageProductBtn = new Button("Quan ly san pham");

        VBox root = new VBox(10);
        root.getChildren().addAll(title, auctionListBtn, productDetailBtn, liveBidBtn, manageProductBtn);

        Scene scene = new Scene(root, 400, 250);
        stage.setTitle("Auction System");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}