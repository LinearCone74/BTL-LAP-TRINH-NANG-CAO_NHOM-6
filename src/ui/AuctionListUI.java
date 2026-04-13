package ui;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class AuctionListUI {

    public class AuctionListUI {

        public static VBox getUI() {
            VBox layout = new VBox(10);

            Label title = new Label("Danh sách đấu giá");

            TableView<String> table = new TableView<>();
            TableColumn<String, String> col1 = new TableColumn<>("Tên sản phẩm");
            TableColumn<String, String> col2 = new TableColumn<>("Giá");

            table.getColumns().addAll(col1, col2);

            layout.getChildren().addAll(title, table);
            return layout;
        }
    }
}
