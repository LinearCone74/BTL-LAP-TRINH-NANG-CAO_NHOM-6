package com.auction.exception;
public class AuctionClosedException extends AuctionException {
    public AuctionClosedException(String message) {
        super(message);
    }
}

// Ngoại lệ khi kết thúc đấu giá