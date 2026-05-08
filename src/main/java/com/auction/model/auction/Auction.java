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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

public class Auction extends Entity {
    private final Item item;
    private final LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionStatus status;
    private Bidder highestBidder;
    private BigDecimal currentPrice;

    private final List<BidTransaction> bidHistory = new ArrayList<>();
    private final Map<String, AutoBidConfig> autoBidConfigs = new LinkedHashMap<>();
    private final List<AuctionObserver> observers = new ArrayList<>();

    private final ReentrantLock bidLock = new ReentrantLock(true);

    private long antiSnipingThresholdSeconds = 120;
    private long antiSnipingExtendSeconds = 120;

    public Auction(Item item, LocalDateTime startTime, LocalDateTime endTime) {
        if (item == null) {
            throw new IllegalArgumentException("Item không được null.");
        }

        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("Thời gian đấu giá không hợp lệ.");
        }

        this.item = item;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = AuctionStatus.OPEN;
        this.currentPrice = item.getStartingPrice();

        item.setStatus(ItemStatus.READY);
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
        if (thresholdSeconds < 0 || extendSeconds < 0) {
            throw new IllegalArgumentException("Anti-sniping không được âm.");
        }

        this.antiSnipingThresholdSeconds = thresholdSeconds;
        this.antiSnipingExtendSeconds = extendSeconds;
    }

    public void addObserver(AuctionObserver observer) {
        if (observer != null) {
            observers.add(observer);
        }
    }

    public void removeObserver(AuctionObserver observer) {
        observers.remove(observer);
    }

    public void notifyObservers(AuctionEventType type, String message) {
        AuctionEvent event = new AuctionEvent(this, type, message);

        observers.forEach(observer -> observer.onAuctionEvent(event));
    }

    public void startAuction() {
        if (status != AuctionStatus.OPEN) {
            throw new AuctionClosedException("Phiên không ở trạng thái OPEN.");
        }

        status = AuctionStatus.RUNNING;
        item.setStatus(ItemStatus.AUCTIONING);
        notifyObservers(AuctionEventType.STARTED, "Phiên đấu giá đã bắt đầu.");
        touch();
    }

    public void cancelAuction() {
        if (status == AuctionStatus.FINISHED || status == AuctionStatus.PAID) {
            throw new AuctionClosedException(
                    "Không thể hủy phiên đã kết thúc/thanh toán."
            );
        }

        status = AuctionStatus.CANCELED;
        item.setStatus(ItemStatus.CANCELED);
        notifyObservers(AuctionEventType.CANCELED, "Phiên đấu giá đã bị hủy.");
        touch();
    }

    public void finishAuction() {
        if (status == AuctionStatus.PAID || status == AuctionStatus.CANCELED) {
            return;
        }

        status = AuctionStatus.FINISHED;

        if (highestBidder == null) {
            item.setStatus(ItemStatus.READY);
        } else {
            item.setStatus(ItemStatus.SOLD);
        }

        notifyObservers(AuctionEventType.FINISHED, "Phiên đấu giá đã kết thúc.");
        touch();
    }

    public void markPaid() {
        if (status != AuctionStatus.FINISHED) {
            throw new AuctionClosedException("Chỉ thanh toán phiên đã FINISHED.");
        }

        status = AuctionStatus.PAID;
        item.setStatus(ItemStatus.SOLD);
        notifyObservers(AuctionEventType.PAID, "Phiên đã được thanh toán.");
        touch();
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(endTime);
    }

    public void registerAutoBid(AutoBidConfig config) {
        Objects.requireNonNull(config, "Cấu hình Auto-Bid không được null.");

        if (status != AuctionStatus.RUNNING && status != AuctionStatus.OPEN) {
            throw new AuctionClosedException(
                    "Không thể đăng ký Auto-Bid cho phiên đã đóng."
            );
        }

        if (config.getMaxBid().compareTo(currentPrice) <= 0) {
            throw new InvalidBidException("Max bid phải lớn hơn giá hiện tại.");
        }

        autoBidConfigs.put(config.getBidder().getId(), config);
        notifyObservers(
                AuctionEventType.AUTO_BID_REGISTERED,
                "Auto-Bid đã được đăng ký."
        );
    }

    public BidResult placeBid(Bidder bidder, BigDecimal amount, boolean autoBid) {
        bidLock.lock();

        try {
            validateBid(amount, bidder);

            BidTransaction transaction =
                    new BidTransaction(bidder, amount, autoBid);

            bidHistory.add(transaction);
            highestBidder = bidder;
            currentPrice = amount;
            item.setCurrentHighestPrice(amount);

            applyAntiSnipingIfNeeded();

            notifyObservers(
                    AuctionEventType.BID_PLACED,
                    bidder.getUsername() + " đặt giá " + amount
            );

            touch();

            return new BidResult(true, "Đặt giá thành công.");

        } catch (RuntimeException ex) {
            return new BidResult(false, ex.getMessage());

        } finally {
            bidLock.unlock();
        }
    }

    private void validateBid(BigDecimal amount, Bidder bidder) {
        if (bidder == null) {
            throw new InvalidBidException("Bidder không hợp lệ.");
        }

        if (status != AuctionStatus.RUNNING) {
            throw new AuctionClosedException("Phiên đấu giá chưa chạy hoặc đã đóng.");
        }

        if (isExpired()) {
            throw new AuctionClosedException("Phiên đấu giá đã hết hạn.");
        }

        if (item.getSeller().getId().equals(bidder.getId())) {
            throw new InvalidBidException(
                    "Seller không được bid sản phẩm của chính mình."
            );
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidBidException("Giá bid phải lớn hơn 0.");
        }

        if (amount.compareTo(currentPrice) <= 0) {
            throw new InvalidBidException("Giá bid phải lớn hơn giá hiện tại.");
        }

        if (bidder.getWalletBalance().compareTo(amount) < 0) {
            throw new InvalidBidException("Số dư ví không đủ.");
        }
    }

    private void applyAntiSnipingIfNeeded() {
        long remaining = java.time.Duration
                .between(LocalDateTime.now(), endTime)
                .toSeconds();

        if (remaining >= 0 && remaining <= antiSnipingThresholdSeconds) {
            endTime = endTime.plusSeconds(antiSnipingExtendSeconds);
        }
    }
}