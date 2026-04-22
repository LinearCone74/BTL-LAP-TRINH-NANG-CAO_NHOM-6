package com.auction.service;

import com.auction.model.auction.Auction;
import com.auction.model.auction.AuctionStatus;

public class PaymentService {
    public void pay(Auction auction) {
        if (auction.getStatus() != AuctionStatus.FINISHED) {
            throw new IllegalStateException("Phiên đấu giá phải kết thúc trước khi thanh toán");
        }
        auction.markPaid();
    }
}