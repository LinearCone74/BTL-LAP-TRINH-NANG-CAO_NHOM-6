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
                    "Auction extended by " + auction.getAntiSnipingExtendSeconds() + " seconds");
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
            throw new IllegalArgumentException("Auction must not be null");
        }
        if (config == null) {
            throw new IllegalArgumentException("AutoBidConfig must not be null");
        }
        if (config.getBidder() == null) {
            throw new IllegalArgumentException("Bidder must not be null");
        }
        if (config.getMaxBid() == null || config.getMaxBid().signum() <= 0) {
            throw new IllegalArgumentException("Max bid must be greater than 0");
        }
        if (config.getIncrement() == null || config.getIncrement().signum() <= 0) {
            throw new IllegalArgumentException("Increment must be greater than 0");
        }
        if (config.getMaxBid().compareTo(auction.getCurrentPrice()) <= 0) {
            throw new IllegalArgumentException("Max bid must be greater than current price");
        }
    }

    private Map<String, AutoBidConfig> getConfigs(Auction auction) {
        return auction.getAutoBidConfigs();
    }
}


