package com.auction.model.auction;

import com.auction.model.item.Item;

import java.time.LocalDateTime;

/**
 * Class chính cho logic đấu giá.
 * AuctionView đang chứa toàn bộ logic, nên Auction kế thừa lại để các service/repository cũ dùng được.
 */
public class Auction extends AuctionView {
    public Auction(Item item, LocalDateTime startTime, LocalDateTime endTime) {
        super(item, startTime, endTime);
    }
}