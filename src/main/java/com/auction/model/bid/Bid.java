package com.auction.model.bid;


import java.time.LocalDateTime;

public class Bid {

    private int bidId;
    private int auctionId;
    private String bidderName;
    private double bidAmount;
    private LocalDateTime bidTime;

    public Bid(int bidId,
               int auctionId,
               String bidderName,
               double bidAmount,
               LocalDateTime bidTime) {

        this.bidId = bidId;
        this.auctionId = auctionId;
        this.bidderName = bidderName;
        this.bidAmount = bidAmount;
        this.bidTime = bidTime;
    }

    public int getBidId() {
        return bidId;
    }

    public int getAuctionId() {
        return auctionId;
    }

    public String getBidderName() {
        return bidderName;
    }

    public double getBidAmount() {
        return bidAmount;
    }

    public LocalDateTime getBidTime() {
        return bidTime;
    }
}
