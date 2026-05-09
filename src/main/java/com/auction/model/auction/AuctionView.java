package com.auction.model.auction;

public class AuctionView {
    private int auctionId;
    private String title;
    private String description;
    private String sellerName;
    private double currentPrice;
    private String status;
    private String endTime;

    public AuctionView(int auctionId, String title, String description,
                       String sellerName, double currentPrice,
                       String status, String endTime) {
        this.auctionId = auctionId;
        this.title = title;
        this.description = description;
        this.sellerName = sellerName;
        this.currentPrice = currentPrice;
        this.status = status;
        this.endTime = endTime;
    }

    public int getAuctionId() { return auctionId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getSellerName() { return sellerName; }
    public double getCurrentPrice() { return currentPrice; }
    public String getStatus() { return status; }
    public String getEndTime() { return endTime; }
}