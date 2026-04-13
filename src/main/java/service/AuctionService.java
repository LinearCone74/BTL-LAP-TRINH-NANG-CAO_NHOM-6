package service;

import exception.AuctionClosedException;
import exception.InvalidBidException;
import model.*;

import java.util.ArrayList;
import java.util.List;

public class AuctionService {
    private List<Auction> auctions = new ArrayList<>();

    public Auction createAuction(Product product) {
        Auction auction = new Auction(product);
        auctions.add(auction);
        return auction;
    }

    public void placeBid(Auction auction, Bidder bidder, double amount) {
        try {
            Bid bid = new Bid(bidder, amount);
            auction.placeBid(bid);
            System.out.println("Dat gia thanh cong: " + bidder.getUsername() + " -> " + amount);
        } catch (InvalidBidException | AuctionClosedException e) {
            System.out.println("Loi dat gia: " + e.getMessage());
        }
    }

    public void updateAllAuctionStatus() {
        for (Auction auction : auctions) {
            auction.updateStatusByTime();
        }
    }

    public void finishAuction(Auction auction) {
        auction.updateStatusByTime();

        if (auction.getStatus() == AuctionStatus.FINISHED) {
            Bidder winner = auction.getWinner();
            if (winner != null) {
                System.out.println("Nguoi thang: " + winner.getUsername());
            } else {
                System.out.println("Phien dau gia ket thuc nhung khong co ai dat gia.");
            }
        } else {
            System.out.println("Phien dau gia chua ket thuc.");
        }
    }

    public List<Auction> getAuctions() {
        return auctions;
    }

    public void printAllAuctions() {
        for (Auction auction : auctions) {
            auction.printAuctionInfo();
            System.out.println("========================");
        }
    }
}