module com.example.dau_gia_truc_tuyen {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.example.dau_gia_truc_tuyen to javafx.fxml;
    exports com.example.dau_gia_truc_tuyen;
}