package com.auction.dao;

import com.auction.model.auction.Auction;
import java.util.List;
import java.util.Optional;

public interface AuctionDao {
    void save(Auction auction);

    List<Auction> findAll();

    Optional<Auction> findById(String id);

    void update(Auction auction);

    void delete(String id);
}
