package com.auction.service;

import com.auction.model.auction.Auction;
import com.auction.model.auction.AuctionStatus;

public class PaymentService {
    public void pay(Auction auction) {
        if (auction.getStatus() != AuctionStatus.FINISHED) {
            throw new IllegalStateException("Auction must be finished before payment");
        }
        auction.markPaid();
    }
}