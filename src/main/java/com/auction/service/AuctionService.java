package com.auction.service;

import com.auction.app.AuctionManager;
import com.auction.model.auction.AutoBidConfig;
import com.auction.model.auction.Auction;
import com.auction.model.auction.BidResult;
import com.auction.model.item.Item;
import com.auction.model.user.Bidder;
import com.auction.repository.AuctionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class AuctionService {
    private final AuctionRepository auctionRepository;
    private final AuctionManager auctionManager = AuctionManager.getInstance();

    public AuctionService(AuctionRepository auctionRepository) {
        this.auctionRepository = auctionRepository;
    }

    public Auction createAuction(Item item, LocalDateTime startTime, LocalDateTime endTime) {
        Auction auction = new Auction(item, startTime, endTime);
        auctionRepository.save(auction);
        auctionManager.addAuction(auction);
        return auction;
    }

    public void startAuction(String auctionId) {
        Auction auction = getRequiredAuction(auctionId);
        auction.startAuction();
    }

    public void finishAuction(String auctionId) {
        Auction auction = getRequiredAuction(auctionId);
        auction.finishAuction();
    }

    public void cancelAuction(String auctionId) {
        Auction auction = getRequiredAuction(auctionId);
        auction.cancelAuction();
    }

    public BidResult placeManualBid(String auctionId, Bidder bidder, BigDecimal amount) {
        Auction auction = getRequiredAuction(auctionId);
        return auction.placeBid(bidder, amount, false);
    } // đặt giá thủ công

    public void registerAutoBid(String auctionId, AutoBidConfig config) {
        Auction auction = getRequiredAuction(auctionId);
        auction.registerAutoBid(config);
    }

    public List<Auction> getAllAuctions() {
        return auctionRepository.findAll();
    }

    public Auction getRequiredAuction(String auctionId) {
        return auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiên đấu giá: " + auctionId));
    }
}