package com.auction.app;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.auction.model.auction.Auction;
public class AuctionManager {
    private static volatile AuctionManager instance;
    private final Map<String, Auction> auctions;

    private AuctionManager() {
        this.auctions = new ConcurrentHashMap<>();
    }

    public static AuctionManager getInstance() {
        if (instance == null) {
            synchronized (AuctionManager.class) {
                if (instance == null) {
                    instance = new AuctionManager();
                }
            }
        }
        return instance;
    }

    public void addAuction(Auction auction) {
        auctions.put(auction.getId(), auction);
    }

    public Auction getAuction(String id) {
        return auctions.get(id);
    }

    public Collection<Auction> getAllAuctions() {
        return auctions.values();
    }

    public void removeAuction(String id) {
        auctions.remove(id);
    }
}