package com.auction;

import com.auction.app.AppContext;
import javafx.application.Application;
import javafx.stage.Stage;

public class AuctionApp extends Application {
    @Override
    public void start(Stage primaryStage) {
        AppContext appContext = AppContext.bootstrap(primaryStage);
        appContext.getNavigator().showLogin();
        primaryStage.setTitle("AuctionHub - Hệ thống đấu giá trực tuyến");
        primaryStage.setMinWidth(1200);
        primaryStage.setMinHeight(780);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}