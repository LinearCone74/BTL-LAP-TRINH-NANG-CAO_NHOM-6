package com.auction.service;

import com.auction.exception.InvalidBidException;
import com.auction.model.auction.Auction;
import com.auction.model.auction.AutoBidConfig;
import com.auction.model.user.Bidder;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
        Objects.requireNonNull(auction);

        Bidder currentLeader = auction.getHighestBidder();
        BigDecimal currentPrice = auction.getCurrentPrice();

        List<AutoBidConfig> eligibleConfigs =
                getEligibleConfigs(auction, currentLeader, currentPrice);

        if (eligibleConfigs.isEmpty()) {
            return;
        }

        AutoBidConfig winner = eligibleConfigs.get(0);

        BigDecimal nextAmount =
                currentPrice.add(winner.getIncrement()).min(winner.getMaxBid());

        if (nextAmount.compareTo(currentPrice) > 0) {
            auction.placeBid(winner.getBidder(), nextAmount, true);
        }
    }

    public void applyAntiSniping(Auction auction) {
        if (auction == null) {
            return;
        }

        long remaining = java.time.Duration
                .between(java.time.LocalDateTime.now(), auction.getEndTime())
                .toSeconds();

        if (remaining >= 0
                && remaining <= auction.getAntiSnipingThresholdSeconds()) {
            auction.setEndTime(
                    auction.getEndTime()
                            .plusSeconds(auction.getAntiSnipingExtendSeconds())
            );
        }
    }

    public List<AutoBidConfig> getEligibleConfigs(Auction auction,
                                                  Bidder currentLeader,
                                                  BigDecimal currentPrice) {
        return getConfigs(auction)
                .values()
                .stream()
                .filter(config ->
                        currentLeader == null
                                || !config.getBidder().getId()
                                .equals(currentLeader.getId())
                )
                .filter(config -> config.getMaxBid().compareTo(currentPrice) > 0)
                .sorted(
                        Comparator.comparing(AutoBidConfig::getMaxBid)
                                .reversed()
                                .thenComparing(AutoBidConfig::getRegisteredAt)
                )
                .toList();
    }

    private void validateConfig(Auction auction, AutoBidConfig config) {
        if (auction == null || config == null || config.getBidder() == null) {
            throw new InvalidBidException("Auto-Bid không hợp lệ.");
        }

        if (config.getIncrement() == null
                || config.getIncrement().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidBidException("Increment phải lớn hơn 0.");
        }

        if (config.getMaxBid() == null
                || config.getMaxBid().compareTo(auction.getCurrentPrice()) <= 0) {
            throw new InvalidBidException("Max bid phải lớn hơn giá hiện tại.");
        }

        if (config.getBidder().getWalletBalance()
                .compareTo(config.getMaxBid()) < 0) {
            throw new InvalidBidException("Số dư ví không đủ cho max bid.");
        }
    }

    private Map<String, AutoBidConfig> getConfigs(Auction auction) {
        return auction.getAutoBidConfigs();
    }
}
