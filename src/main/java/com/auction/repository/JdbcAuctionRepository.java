package com.auction.repository;

import database.DBConnection;
import com.auction.model.auction.AuctionView;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JdbcAuctionRepository {

    public List<AuctionView> findAll() {
        List<AuctionView> list = new ArrayList<>();

        String sql = "SELECT * FROM auctions";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(new AuctionView(
                        rs.getInt("auction_id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("seller_name"),
                        rs.getDouble("current_price"),
                        rs.getString("status"),
                        rs.getString("end_time")
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }
}