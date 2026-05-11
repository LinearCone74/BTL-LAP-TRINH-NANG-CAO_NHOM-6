package com.auction.repository;

import com.auction.model.auction.AuctionView;
import database.DBConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JdbcAuctionRepository {

    public List<AuctionView> findAll() {
        List<AuctionView> auctions = new ArrayList<>();
        String sql = "SELECT * FROM auctions";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()
        ) {
            while (resultSet.next()) {
                auctions.add(new AuctionView(
                        resultSet.getInt("auction_id"),
                        resultSet.getString("title"),
                        resultSet.getString("description"),
                        resultSet.getString("seller_name"),
                        resultSet.getDouble("current_price"),
                        resultSet.getString("status"),
                        resultSet.getString("end_time")
                ));
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return auctions;
    }

    public void save(AuctionView auctionView) {
        String sql = """
                INSERT INTO auctions
                (title, description, seller_name, current_price, status, end_time)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, auctionView.getTitle());
            statement.setString(2, auctionView.getDescription());
            statement.setString(3, auctionView.getSellerName());
            statement.setBigDecimal(4, auctionView.getCurrentPrice());
            statement.setString(5, String.valueOf(auctionView.getStatus()));
            statement.setString(6, auctionView.getEndTimeText());

            statement.executeUpdate();

        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public void update(AuctionView auctionView) {
        String sql = """
                UPDATE auctions
                SET title = ?,
                    description = ?,
                    seller_name = ?,
                    current_price = ?,
                    status = ?,
                    end_time = ?
                WHERE auction_id = ?
                """;

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, auctionView.getTitle());
            statement.setString(2, auctionView.getDescription());
            statement.setString(3, auctionView.getSellerName());
            statement.setBigDecimal(4, auctionView.getCurrentPrice());
            statement.setString(5, String.valueOf(auctionView.getStatus()));
            statement.setString(6, auctionView.getEndTimeText());
            statement.setInt(7, Integer.parseInt(auctionView.getId()));

            statement.executeUpdate();

        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public void deleteById(String auctionId) {
        String sql = "DELETE FROM auctions WHERE auction_id = ?";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, Integer.parseInt(auctionId));
            statement.executeUpdate();

        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}