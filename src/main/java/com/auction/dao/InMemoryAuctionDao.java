package com.auction.dao;

import com.auction.model.auction.Auction;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryAuctionDao implements AuctionDao {
    private final ConcurrentHashMap<String, Auction> database = new ConcurrentHashMap<>();

    @Override
    public void save(Auction auction) {
        database.put(auction.getId(), auction);
    }

    @Override
    public List<Auction> findAll() {
        return new ArrayList<>(database.values());
    }

    @Override
    public Optional<Auction> findById(String id) {
        return Optional.ofNullable(database.get(id));
    }

    @Override
    public void update(Auction auction) {
        database.put(auction.getId(), auction);
    }

    @Override
    public void delete(String id) {
        database.remove(id);
    }
}