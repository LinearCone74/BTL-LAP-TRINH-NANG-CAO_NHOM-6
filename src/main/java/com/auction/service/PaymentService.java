package com.auction.service;

import com.auction.exception.AuctionClosedException;
import com.auction.exception.ValidationException;
import com.auction.model.auction.Auction;
import com.auction.model.auction.AuctionStatus;
import com.auction.model.user.Bidder;

public class PaymentService {
    public void pay(Auction auction) {
        if (auction == null) {
            throw new ValidationException("Phiên đấu giá không hợp lệ.");
        }

        if (auction.getStatus() != AuctionStatus.FINISHED) {
            throw new AuctionClosedException("Chỉ thanh toán phiên đã kết thúc.");
        }

        Bidder winner = auction.getHighestBidder();

        if (winner == null) {
            throw new ValidationException("Phiên không có người thắng.");
        }

        winner.withdraw(auction.getCurrentPrice());
        auction.markPaid();
    }
}