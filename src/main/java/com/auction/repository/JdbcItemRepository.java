package com.auction.repository;

import database.DBConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC repository để Seller quản lý sản phẩm đấu giá.
 * Mỗi sản phẩm khi được tạo sẽ tự động tạo một bản ghi trong bảng auctions
 * để Bidder có thể thấy và tham gia đấu giá ngay lập tức.
 */
public class JdbcItemRepository {

    public JdbcItemRepository() {
        ensureSchema();
    }

    // ==================== Schema ====================

    private void ensureSchema() {
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement()) {
            // Bảng items lưu thông tin sản phẩm gốc của Seller
            st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS items (" +
                            "  item_id     INT AUTO_INCREMENT PRIMARY KEY," +
                            "  name        VARCHAR(255) NOT NULL," +
                            "  category    VARCHAR(100) NOT NULL," +
                            "  description TEXT," +
                            "  starting_price DECIMAL(18,2) NOT NULL," +
                            "  seller_username VARCHAR(100) NOT NULL," +
                            "  metadata    TEXT," +
                            "  created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                            ")"
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==================== Data Model ====================

    public record ItemRow(
            int itemId,
            String name,
            String category,
            String description,
            BigDecimal startingPrice,
            String sellerUsername,
            String metadata,
            Integer linkedAuctionId   // null nếu chưa tạo auction
    ) {}

    public record CreateResult(boolean success, String message, int auctionId) {}

    // ==================== CRUD ====================

    /**
     * Tạo sản phẩm MỚI + tự động tạo auction tương ứng.
     * Bidder sẽ thấy ngay ở tab "Phiên đấu giá".
     */
    public CreateResult createItemAndAuction(
            String name,
            String category,
            String description,
            BigDecimal startingPrice,
            String sellerUsername,
            String metadata,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        if (name == null || name.isBlank())
            return new CreateResult(false, "Tên sản phẩm không được để trống.", 0);
        if (startingPrice == null || startingPrice.compareTo(BigDecimal.ZERO) <= 0)
            return new CreateResult(false, "Giá mở phiên phải lớn hơn 0.", 0);
        if (startTime == null || endTime == null || !endTime.isAfter(startTime))
            return new CreateResult(false, "Thời gian không hợp lệ (kết thúc phải sau bắt đầu).", 0);

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Insert vào bảng items
                int itemId;
                String insertItem = "INSERT INTO items(name, category, description, starting_price, seller_username, metadata) VALUES (?,?,?,?,?,?)";
                try (PreparedStatement ps = conn.prepareStatement(insertItem, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, name);
                    ps.setString(2, category);
                    ps.setString(3, description);
                    ps.setBigDecimal(4, startingPrice);
                    ps.setString(5, sellerUsername);
                    ps.setString(6, metadata == null ? "" : metadata);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        rs.next();
                        itemId = rs.getInt(1);
                    }
                }

                // 2. Tạo auction tương ứng để Bidder thấy ngay
                int auctionId;
                String insertAuction =
                        "INSERT INTO auctions(title, description, seller_name, current_price, status, end_time) " +
                                "VALUES (?,?,?,?,'RUNNING',?)";
                try (PreparedStatement ps = conn.prepareStatement(insertAuction, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, name);
                    ps.setString(2, description == null ? "" : description);
                    ps.setString(3, sellerUsername);
                    ps.setBigDecimal(4, startingPrice);
                    ps.setTimestamp(5, Timestamp.valueOf(endTime));
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        rs.next();
                        auctionId = rs.getInt(1);
                    }
                }

                // 3. Liên kết item với auction
                try (PreparedStatement ps = conn.prepareStatement(
                        "ALTER TABLE items ADD COLUMN IF NOT EXISTS linked_auction_id INT NULL")) {
                    ps.executeUpdate();
                } catch (Exception ignored) {}

                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE items SET linked_auction_id = ? WHERE item_id = ?")) {
                    ps.setInt(1, auctionId);
                    ps.setInt(2, itemId);
                    ps.executeUpdate();
                }

                conn.commit();
                return new CreateResult(true,
                        "✓ Đã tạo sản phẩm và mở phiên đấu giá thành công! (Auction #" + auctionId + ")", auctionId);

            } catch (Exception e) {
                conn.rollback();
                e.printStackTrace();
                return new CreateResult(false, "Lỗi khi tạo: " + e.getMessage(), 0);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new CreateResult(false, "Không kết nối được database.", 0);
        }
    }

    /**
     * Cập nhật thông tin sản phẩm + auction liên kết.
     */
    public CreateResult updateItemAndAuction(
            int itemId,
            String name,
            String category,
            String description,
            BigDecimal startingPrice,
            String metadata,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        if (name == null || name.isBlank())
            return new CreateResult(false, "Tên sản phẩm không được để trống.", 0);

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Lấy linked_auction_id
                int auctionId = 0;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT linked_auction_id FROM items WHERE item_id = ?")) {
                    ps.setInt(1, itemId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) auctionId = rs.getInt("linked_auction_id");
                    }
                }

                // Update bảng items
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE items SET name=?, category=?, description=?, starting_price=?, metadata=? WHERE item_id=?")) {
                    ps.setString(1, name);
                    ps.setString(2, category);
                    ps.setString(3, description);
                    ps.setBigDecimal(4, startingPrice);
                    ps.setString(5, metadata == null ? "" : metadata);
                    ps.setInt(6, itemId);
                    ps.executeUpdate();
                }

                // Update auction
                if (auctionId > 0) {
                    String sql = "UPDATE auctions SET title=?, description=?, current_price=?" +
                            (endTime != null ? ", end_time=?" : "") +
                            " WHERE auction_id=?";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        int idx = 1;
                        ps.setString(idx++, name);
                        ps.setString(idx++, description == null ? "" : description);
                        ps.setBigDecimal(idx++, startingPrice);
                        if (endTime != null) ps.setTimestamp(idx++, Timestamp.valueOf(endTime));
                        ps.setInt(idx, auctionId);
                        ps.executeUpdate();
                    }
                }

                conn.commit();
                return new CreateResult(true, "✓ Đã cập nhật sản phẩm thành công!", auctionId);

            } catch (Exception e) {
                conn.rollback();
                e.printStackTrace();
                return new CreateResult(false, "Lỗi khi cập nhật: " + e.getMessage(), 0);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new CreateResult(false, "Không kết nối được database.", 0);
        }
    }

    /**
     * Xóa sản phẩm và đánh dấu auction là FINISHED.
     */
    public CreateResult deleteItem(int itemId) {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Lấy linked_auction_id
                int auctionId = 0;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT linked_auction_id FROM items WHERE item_id = ?")) {
                    ps.setInt(1, itemId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) auctionId = rs.getInt("linked_auction_id");
                    }
                }

                // Đánh dấu auction là FINISHED thay vì xóa (để giữ lịch sử bid)
                if (auctionId > 0) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE auctions SET status='FINISHED' WHERE auction_id=?")) {
                        ps.setInt(1, auctionId);
                        ps.executeUpdate();
                    }
                }

                // Xóa item
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM items WHERE item_id=?")) {
                    ps.setInt(1, itemId);
                    ps.executeUpdate();
                }

                conn.commit();
                return new CreateResult(true, "✓ Đã xóa sản phẩm và đóng phiên đấu giá!", 0);

            } catch (Exception e) {
                conn.rollback();
                e.printStackTrace();
                return new CreateResult(false, "Lỗi khi xóa: " + e.getMessage(), 0);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new CreateResult(false, "Không kết nối được database.", 0);
        }
    }

    /**
     * Lấy danh sách sản phẩm của một seller cụ thể.
     */
    public List<ItemRow> findBySeller(String sellerUsername) {
        List<ItemRow> list = new ArrayList<>();
        String sql = "SELECT i.*, i.linked_auction_id FROM items i " +
                "WHERE i.seller_username = ? ORDER BY i.item_id DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sellerUsername);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int auctionId = 0;
                    try { auctionId = rs.getInt("linked_auction_id"); } catch (Exception ignored) {}
                    list.add(new ItemRow(
                            rs.getInt("item_id"),
                            rs.getString("name"),
                            rs.getString("category"),
                            rs.getString("description"),
                            rs.getBigDecimal("starting_price"),
                            rs.getString("seller_username"),
                            rs.getString("metadata"),
                            auctionId > 0 ? auctionId : null
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}
