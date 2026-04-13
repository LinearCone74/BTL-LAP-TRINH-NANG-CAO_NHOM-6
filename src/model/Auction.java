package model;

import exception.AuctionClosedException;
import exception.InvalidBidException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Auction {
    private Product product;
    private List<Bid> bids;
    private Bid highestBid;
    private AuctionStatus status;

    public Auction(Product product) {
        this.product = product;
        this.bids = new ArrayList<>();
        this.status = AuctionStatus.OPEN;
    }

    public Product getProduct() {
        return product;
    }

    public List<Bid> getBids() {
        return bids;
    }

    public Bid getHighestBid() {
        return highestBid;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public void updateStatusByTime() {
        LocalDateTime now = LocalDateTime.now();

        if (status == AuctionStatus.CANCELED || status == AuctionStatus.PAID) {
            return;
        }

        if (now.isBefore(product.getStartTime())) {
            status = AuctionStatus.OPEN;
        } else if ((now.isEqual(product.getStartTime()) || now.isAfter(product.getStartTime()))
                && now.isBefore(product.getEndTime())) {
            status = AuctionStatus.RUNNING;
        } else if (now.isEqual(product.getEndTime()) || now.isAfter(product.getEndTime())) {
            status = AuctionStatus.FINISHED;
        }
    }

    public void placeBid(Bid bid) throws InvalidBidException, AuctionClosedException {
        updateStatusByTime();

        if (status != AuctionStatus.RUNNING) {
            throw new AuctionClosedException("Phien dau gia khong mo de dat gia.");
        }

        double currentPrice = (highestBid == null)
                ? product.getStartingPrice()
                : highestBid.getAmount();

        if (bid.getAmount() <= currentPrice) {
            throw new InvalidBidException("Gia dau phai lon hon gia hien tai.");
        }

        bids.add(bid);
        highestBid = bid;
        product.setHighestPrice(bid.getAmount());
    }

    public Bidder getWinner() {
        if (status == AuctionStatus.FINISHED && highestBid != null) {
            return highestBid.getBidder();
        }
        return null;
    }

    public void markPaid() {
        if (status == AuctionStatus.FINISHED) {
            status = AuctionStatus.PAID;
        }
    }

    public void cancelAuction() {
        status = AuctionStatus.CANCELED;
    }

    public void printAuctionInfo() {
        updateStatusByTime();

        System.out.println("===== AUCTION INFO =====");
        product.printInfo();
        System.out.println("Status: " + status);

        if (highestBid != null) {
            System.out.println("Current Leader: " + highestBid.getBidder().getUsername());
            System.out.println("Highest Bid: " + highestBid.getAmount());
        } else {
            System.out.println("Chua co ai dat gia.");
        }
    }
}