package com.auction.model.auction;

import com.auction.exception.AuctionClosedException;
import com.auction.exception.InvalidBidException;
import com.auction.model.base.Entity;
import com.auction.model.item.Item;
import com.auction.model.item.ItemStatus;
import com.auction.model.notification.AuctionEvent;
import com.auction.model.notification.AuctionEventType;
import com.auction.model.notification.AuctionObserver;
import com.auction.model.user.Bidder;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

public class Auction extends Entity {
    private final Item item;
    private final LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionStatus status;
    private Bidder highestBidder;
    private BigDecimal currentPrice;
    private final List<BidTransaction> bidHistory;
    private final Map<String, AutoBidConfig> autoBidConfigs;
    private final List<AuctionObserver> observers;
    private final ReentrantLock bidLock;
    private final PriorityBlockingQueue<BidTransaction> realtimeBidQueue;

    private long antiSnipingThresholdSeconds = 15;
    private long antiSnipingExtendSeconds = 30;

    public Auction(Item item, LocalDateTime startTime, LocalDateTime endTime) {
        super();
        this.item = item;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = AuctionStatus.OPEN;
        this.currentPrice = item.getStartingPrice();
        this.bidHistory = new ArrayList<>();
        this.autoBidConfigs = new ConcurrentHashMap<>();
        this.observers = new CopyOnWriteArrayList<>();
        this.bidLock = new ReentrantLock(true);
        this.realtimeBidQueue = new PriorityBlockingQueue<>();
    }

    public Item getItem() {
        return item;
    }

}