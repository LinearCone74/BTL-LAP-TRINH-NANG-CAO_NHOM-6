package com.auction.model.notification;
public interface AuctionObserver {
    void onAuctionEvent(AuctionEvent event);
}