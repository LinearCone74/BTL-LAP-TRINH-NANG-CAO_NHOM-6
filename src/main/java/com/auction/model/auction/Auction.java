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
import com.auction.util.TimeUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
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
    }
    public Item getItem() {
        return item;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
        touch();
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public Bidder getHighestBidder() {
        return highestBidder;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public List<BidTransaction> getBidHistory() {
        return Collections.unmodifiableList(bidHistory);
    }

    public Map<String, AutoBidConfig> getAutoBidConfigs() {
        return autoBidConfigs;
    }

    public long getAntiSnipingThresholdSeconds() {
        return antiSnipingThresholdSeconds;
    }
    public long getAntiSnipingExtendSeconds() {
        return antiSnipingExtendSeconds;
    }

    public void configureAntiSniping(long thresholdSeconds, long extendSeconds) {
        this.antiSnipingThresholdSeconds = thresholdSeconds;
        this.antiSnipingExtendSeconds = extendSeconds;
    }

    public void addObserver(AuctionObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(AuctionObserver observer) {
        observers.remove(observer);
    }

    public void notifyObservers(AuctionEventType type, String message) {
        AuctionEvent event = new AuctionEvent(this, type, message);
        for (AuctionObserver observer : observers) {
            observer.onAuctionEvent(event);
        }
    }

    public void startAuction() {
        if (status == AuctionStatus.OPEN) {
            status = AuctionStatus.RUNNING;
            item.setStatus(ItemStatus.AUCTIONING);
            notifyObservers(AuctionEventType.STATUS_CHANGED, "Auction started");
        }
    }
    public void cancelAuction() {
        if (status == AuctionStatus.OPEN || status == AuctionStatus.RUNNING) {
            status = AuctionStatus.CANCELED;
            item.setStatus(ItemStatus.CANCELED);
            notifyObservers(AuctionEventType.STATUS_CHANGED, "Auction canceled");
        }
    }

    public void finishAuction() {
        if (status == AuctionStatus.RUNNING) {
            status = AuctionStatus.FINISHED;
            if (highestBidder != null) {
                item.setStatus(ItemStatus.SOLD);
            } else {
                item.setStatus(ItemStatus.READY);
            }
            notifyObservers(AuctionEventType.AUCTION_FINISHED, "Auction finished");
        }
    }

    public void markPaid() {
        if (status == AuctionStatus.FINISHED) {
            status = AuctionStatus.PAID;
            notifyObservers(AuctionEventType.STATUS_CHANGED, "Auction paid");
        }
    }

    public boolean isExpired() {
        return TimeUtils.isExpired(endTime);
    }

    public void registerAutoBid(AutoBidConfig config) {
        autoBidConfigs.put(config.getBidder().getId(), config);
    }

    public BidResult placeBid(Bidder bidder, BigDecimal amount, boolean autoBid) {
        bidLock.lock();
        try {
            validateBid(amount, bidder);
            BidTransaction transaction = new BidTransaction(bidder, amount, autoBid);
            bidHistory.add(transaction);
            currentPrice = amount;
            highestBidder = bidder;
            item.setCurrentHighestPrice(amount);
            notifyObservers(AuctionEventType.NEW_BID,bidder.getUsername() + " placed bid: " + amount + (autoBid ? " [AUTO]" : ""));
            return new BidResult(true, "Bid placed successfully");
        } finally {
            bidLock.unlock();
        }
    }
    private void validateBid(BigDecimal amount, Bidder bidder) {
        if (status != AuctionStatus.RUNNING) {
            throw new AuctionClosedException("Auction is not running");
        }
        if (isExpired()) {
            status = AuctionStatus.FINISHED;
            throw new AuctionClosedException("Auction already ended");
        }
        if (amount == null || amount.compareTo(currentPrice) <= 0) {
            throw new InvalidBidException("Bid must be higher than current price");
        }
        if (highestBidder != null && highestBidder.getId().equals(bidder.getId())) {
            throw new InvalidBidException("Current leader cannot bid again immediately");
        }
    }
}  