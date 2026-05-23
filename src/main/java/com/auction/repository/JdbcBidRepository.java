package com.auction.repository;

import database.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class JdbcBidRepository {

    public void saveBid(int auctionId,
                        String bidderName,
                        double bidAmount) {

        String sql =
                "INSERT INTO bids(auction_id, bidder_name, bid_amount) VALUES (?, ?, ?)";

        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {

            ps.setInt(1, auctionId);
            ps.setString(2, bidderName);
            ps.setDouble(3, bidAmount);

            ps.executeUpdate();

            System.out.println("Lưu bid thành công");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}