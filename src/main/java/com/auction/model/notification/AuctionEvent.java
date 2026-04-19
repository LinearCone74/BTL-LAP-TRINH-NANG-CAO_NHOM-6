package com.auction.model.notification;
import com.auction.model.auction.Auction;
public class AuctionEvent {
    private final Auction auction;
    private final AuctionEventType type;
    private final String message;

    public AuctionEvent(Auction auction, AuctionEventType type, String message) {
        this.auction = auction;
        this.type = type;
        this.message = message;
    }

    public Auction getAuction() {
        return auction;
    }

    public AuctionEventType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }
}