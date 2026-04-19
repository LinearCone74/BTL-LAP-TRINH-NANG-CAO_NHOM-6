package com.auction.model.auction;

import com.auction.model.user.Bidder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AutoBidConfig {
    private final Bidder bidder;
    private final BigDecimal maxBid;
    private final BigDecimal increment;
    private final LocalDateTime registeredAt;

    public AutoBidConfig(Bidder bidder, BigDecimal maxBid, BigDecimal increment) {
        this.bidder = bidder;
        this.maxBid = maxBid;
        this.increment = increment;
        this.registeredAt = LocalDateTime.now();
    }

    public Bidder getBidder() {
        return bidder;
    }

    public BigDecimal getMaxBid() {
        return maxBid;
    }

    public BigDecimal getIncrement() {
        return increment;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }
}