package com.auction.exception;
public class InvalidBidException extends AuctionException {
    public InvalidBidException(String message) {
        super(message);
    }
}

// Ngoại lệ cho lỗi đấu giá