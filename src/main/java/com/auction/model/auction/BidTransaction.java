package com.auction.model.auction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.auction.model.base.Entity;
import com.auction.model.user.Bidder;

public class BidTransaction extends Entity implements Comparable<BidTransaction> {
    private final Bidder bidder;
    private final BigDecimal amount;
    private final LocalDateTime bidTime;
    private final boolean autoBid;

    public BidTransaction(Bidder bidder, BigDecimal amount, boolean autoBid) {
        super();
        this.bidder = bidder;
        this.amount = amount;
        this.autoBid = autoBid;
        this.bidTime = LocalDateTime.now();
    }

    public Bidder getBidder() {
        return bidder;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getBidTime() {
        return bidTime;
    }

    public boolean isAutoBid() {
        return autoBid;
    }

    @Override
    public int compareTo(BidTransaction other) {
        int cmp = other.amount.compareTo(this.amount);
        if (cmp != 0) return cmp;
        return this.bidTime.compareTo(other.bidTime);
    }

    @Override
    public String toString() {
        return "%s bid %s at %s%s".formatted(bidder.getUsername(), amount, bidTime, autoBid ? " [AUTO]" : "");
    }
}