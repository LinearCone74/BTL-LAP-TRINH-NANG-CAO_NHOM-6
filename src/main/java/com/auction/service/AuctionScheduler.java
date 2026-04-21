package com.auction.service;

import com.auction.app.AuctionManager;
import com.auction.model.auction.Auction;
import com.auction.model.auction.AuctionStatus;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AuctionScheduler {
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final AuctionManager auctionManager = AuctionManager.getInstance();

    public void start() {
        executor.scheduleAtFixedRate(() -> {
            for (Auction auction : auctionManager.getAllAuctions()) {
                if (auction.getStatus() == AuctionStatus.RUNNING && auction.isExpired()) {
                    auction.finishAuction();
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    public void shutdown() {
        executor.shutdown();
    }
}

// Công cụ tạo lịch đấu giá