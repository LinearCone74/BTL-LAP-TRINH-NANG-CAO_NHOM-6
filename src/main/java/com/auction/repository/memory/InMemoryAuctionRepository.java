package com.auction.repository.memory;

import com.auction.model.auction.Auction;
import com.auction.repository.AuctionRepository;

public class InMemoryAuctionRepository extends InMemoryCrudRepository<Auction> implements AuctionRepository {
}