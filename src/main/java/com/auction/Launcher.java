package com.auction;

import com.auction.socket.AuctionSocketServer;

public class Launcher {
    public static void main(String[] args) {

        // 1. Bật Server trên một Luồng (Thread) riêng biệt
        Thread serverThread = new Thread(() -> {
            try {
                System.out.println("[HỆ THỐNG] Đang khởi động Server Socket...");
                // Gọi class Server của nhóm bạn
                AuctionSocketServer.main(new String[]{});
            } catch (Exception e) {
                System.out.println("[LỖI] Không thể khởi động Server: " + e.getMessage());
                e.printStackTrace();
            }
        });

        // Bắt đầu chạy Server ngầm
        serverThread.start();

        // 2. Tạm dừng luồng chính 2 giây để chờ Server mở cổng kết nối xong
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 3. Khởi động giao diện Client (JavaFX)
        System.out.println("[HỆ THỐNG] Đang khởi động Client...");
        AuctionApp.main(args);
    }
}