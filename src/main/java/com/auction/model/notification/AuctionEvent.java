package com.auction.model.notification;
import com.auction.model.auction.AuctionView;

public class AuctionEvent {
    private final AuctionView auction;
    private final AuctionEventType type;
    private final String message;

    public AuctionEvent(AuctionView auction, AuctionEventType type, String message) {
        this.auction = auction;
        this.type = type;
        this.message = message;
    }

    public AuctionView getAuction() {
        return auction;
    }

    public AuctionEventType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }
}