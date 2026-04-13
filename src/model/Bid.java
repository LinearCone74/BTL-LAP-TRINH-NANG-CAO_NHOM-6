package model;

import java.time.LocalDateTime;

public class Bid {
    private Bidder bidder;
    private double amount;
    private LocalDateTime bidTime;

    public Bid(Bidder bidder, double amount) {
        this.bidder = bidder;
        this.amount = amount;
        this.bidTime = LocalDateTime.now();
    }

    public Bidder getBidder() {
        return bidder;
    }

    public double getAmount() {
        return amount;
    }

    public LocalDateTime getBidTime() {
        return bidTime;
    }

    public void printInfo() {
        System.out.println("Bidder: " + bidder.getUsername()
                + ", Amount: " + amount
                + ", Time: " + bidTime);
    }
}