package com.auction.service;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.auction.model.auction.Auction;
import com.auction.model.auction.AutoBidConfig;
import com.auction.model.auction.BidResult;
import com.auction.model.notification.AuctionEventType;
import com.auction.model.user.Bidder;
import com.auction.util.TimeUtils;

public class AutoBidService {

    public void register(Auction auction, AutoBidConfig config) {
        validateConfig(auction, config);
        auction.registerAutoBid(config);
    }

    public void remove(Auction auction, String bidderId) {
        getConfigs(auction).remove(bidderId);
    }

    public boolean exists(Auction auction, String bidderId) {
        return getConfigs(auction).containsKey(bidderId);
    }

    public AutoBidConfig getByBidderId(Auction auction, String bidderId) {
        return getConfigs(auction).get(bidderId);
    }
    public void process(Auction auction) {
        while (true) {
            Bidder currentLeader = auction.getHighestBidder();
            BigDecimal currentPrice = auction.getCurrentPrice();

            List<AutoBidConfig> candidates = getEligibleConfigs(auction, currentLeader, currentPrice);
            if (candidates.isEmpty()) {
                break;
            }

            AutoBidConfig selected = candidates.get(0);
            BigDecimal nextBid = currentPrice.add(selected.getIncrement());

            if (nextBid.compareTo(selected.getMaxBid()) > 0) {
                break;
            }

            BidResult result = auction.placeBid(selected.getBidder(), nextBid, true);
            if (!result.success()) {
                break;
            }

            applyAntiSniping(auction);
        }
    }
    public void applyAntiSniping(Auction auction) {
        if (TimeUtils.isInLastSeconds(auction.getEndTime(), auction.getAntiSnipingThresholdSeconds())) {
            auction.setEndTime(TimeUtils.extend(auction.getEndTime(), auction.getAntiSnipingExtendSeconds()));
            auction.notifyObservers(AuctionEventType.AUCTION_EXTENDED,
                    "Phiên đấu giá được gia hạn bởi " + auction.getAntiSnipingExtendSeconds() + " giây");
        }
    }

    public List<AutoBidConfig> getEligibleConfigs(Auction auction, Bidder excludeBidder, BigDecimal currentPrice) {
        return getConfigs(auction).values()
                .stream()
                .filter(config -> excludeBidder == null
                        || !config.getBidder().getId().equals(excludeBidder.getId()))
                .filter(config -> currentPrice.add(config.getIncrement()).compareTo(config.getMaxBid()) <= 0)
                .sorted(Comparator.comparing(AutoBidConfig::getRegisteredAt))
                .collect(Collectors.toList());
    }

    private void validateConfig(Auction auction, AutoBidConfig config) {
        if (auction == null) {
            throw new IllegalArgumentException("Phiên đấu giá không được phép trống");
        }
        if (config == null) {
            throw new IllegalArgumentException("Thông tin đấu giá tự động không được để trống");
        }
        if (config.getBidder() == null) {
            throw new IllegalArgumentException("Người đấu giá không được để trống");
        }
        if (config.getMaxBid() == null || config.getMaxBid().signum() <= 0) {
            throw new IllegalArgumentException("Giá đấu tối đa phải lớn hơn 0");
        }
        if (config.getIncrement() == null || config.getIncrement().signum() <= 0) {
            throw new IllegalArgumentException("Bước nhảy phải lớn hơn 0");
        }
        if (config.getMaxBid().compareTo(auction.getCurrentPrice()) <= 0) {
            throw new IllegalArgumentException("Giá đấu tối đa phải lớn hơn giá đấu hiện tại");
        }
    }

    private Map<String, AutoBidConfig> getConfigs(Auction auction) {
        return auction.getAutoBidConfigs();
    }
}


// Đấu giá tự động

