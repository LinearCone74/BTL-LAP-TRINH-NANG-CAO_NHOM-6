package com.auction.repository;

import com.auction.model.auction.AuctionView;
import database.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RealtimeAuctionRepository {
    private static final long ANTI_SNIPING_SECONDS = 120;
    private static final long EXTEND_SECONDS = 120;

    public RealtimeAuctionRepository() {
        ensureRealtimeSchema();
    }

    public List<AuctionView> findAll() {
        List<AuctionView> list = new ArrayList<>();
        String sql = "SELECT * FROM auctions ORDER BY auction_id DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(toAuctionView(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public AuctionView findById(int auctionId) {
        String sql = "SELECT * FROM auctions WHERE auction_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return toAuctionView(rs);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public synchronized BidResponse placeBid(int auctionId, String bidderUsername, BigDecimal amount) {
        if (bidderUsername == null || bidderUsername.isBlank()) {
            return BidResponse.fail("Bạn cần đăng nhập trước khi bid.");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return BidResponse.fail("Số tiền bid phải lớn hơn 0.");
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                AuctionState state = lockAuction(conn, auctionId);
                if (state == null) {
                    conn.rollback();
                    return BidResponse.fail("Không tìm thấy phiên đấu giá.");
                }

                String validationError = validateBid(state, bidderUsername, amount);
                if (validationError != null) {
                    conn.rollback();
                    return BidResponse.fail(validationError);
                }

                updateAuctionPrice(conn, auctionId, amount, bidderUsername);
                insertBid(conn, auctionId, bidderUsername, amount, false);
                applyAntiSniping(conn, auctionId, state.endTime);
                processAutoBids(conn, auctionId, bidderUsername, amount);

                conn.commit();
                return BidResponse.ok("Đặt bid thành công. Giá đã được cập nhật realtime cho mọi người.");
            } catch (Exception e) {
                conn.rollback();
                e.printStackTrace();
                return BidResponse.fail("Không thể đặt bid: " + e.getMessage());
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return BidResponse.fail("Không kết nối được database.");
        }
    }

    public synchronized BidResponse registerAutoBid(int auctionId,
                                                    String bidderUsername,
                                                    BigDecimal maxBid,
                                                    BigDecimal increment) {
        if (bidderUsername == null || bidderUsername.isBlank()) {
            return BidResponse.fail("Bạn cần đăng nhập trước khi bật Auto-Bid.");
        }
        if (maxBid == null || increment == null
                || maxBid.compareTo(BigDecimal.ZERO) <= 0
                || increment.compareTo(BigDecimal.ZERO) <= 0) {
            return BidResponse.fail("Max bid và bước nhảy phải lớn hơn 0.");
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                AuctionState state = lockAuction(conn, auctionId);
                if (state == null) {
                    conn.rollback();
                    return BidResponse.fail("Không tìm thấy phiên đấu giá.");
                }
                if (maxBid.compareTo(state.currentPrice) <= 0) {
                    conn.rollback();
                    return BidResponse.fail("Max bid phải lớn hơn giá hiện tại.");
                }

                String sql = "INSERT INTO auto_bid_configs(auction_id, bidder_username, max_bid, increment_amount) "
                        + "VALUES (?, ?, ?, ?) "
                        + "ON DUPLICATE KEY UPDATE max_bid = VALUES(max_bid), "
                        + "increment_amount = VALUES(increment_amount), active = TRUE";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, auctionId);
                    ps.setString(2, bidderUsername);
                    ps.setBigDecimal(3, maxBid);
                    ps.setBigDecimal(4, increment);
                    ps.executeUpdate();
                }

                processAutoBids(conn, auctionId, state.highestBidder, state.currentPrice);
                conn.commit();
                return BidResponse.ok("Đã bật Auto-Bid. Hệ thống sẽ tự bid khi có người vượt giá của bạn.");
            } catch (Exception e) {
                conn.rollback();
                e.printStackTrace();
                return BidResponse.fail("Không thể bật Auto-Bid: " + e.getMessage());
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return BidResponse.fail("Không kết nối được database.");
        }
    }

    public List<BidHistoryRow> findBidHistory(int auctionId) {
        List<BidHistoryRow> rows = new ArrayList<>();
        String sql = "SELECT bidder_username, amount, auto_bid, bid_time FROM bid_transactions "
                + "WHERE auction_id = ? ORDER BY bid_time DESC, bid_id DESC LIMIT 50";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new BidHistoryRow(
                            rs.getTimestamp("bid_time").toLocalDateTime().toString(),
                            rs.getString("bidder_username"),
                            rs.getBigDecimal("amount").toPlainString(),
                            rs.getBoolean("auto_bid") ? "AUTO" : "MANUAL"
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return rows;
    }

    public int findLastBidId(int auctionId) {
        String sql = "SELECT COALESCE(MAX(bid_id), 0) AS last_bid_id FROM bid_transactions WHERE auction_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("last_bid_id");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    private void processAutoBids(Connection conn,
                                 int auctionId,
                                 String currentLeader,
                                 BigDecimal currentPrice) throws SQLException {
        for (int i = 0; i < 100; i++) {
            AutoBidRow winner = findBestAutoBid(conn, auctionId, currentLeader, currentPrice);
            if (winner == null) {
                return;
            }

            BigDecimal nextAmount = currentPrice.add(winner.increment);
            if (nextAmount.compareTo(winner.maxBid) > 0) {
                nextAmount = winner.maxBid;
            }
            if (nextAmount.compareTo(currentPrice) <= 0) {
                return;
            }

            updateAuctionPrice(conn, auctionId, nextAmount, winner.bidderUsername);
            insertBid(conn, auctionId, winner.bidderUsername, nextAmount, true);

            currentLeader = winner.bidderUsername;
            currentPrice = nextAmount;
        }
    }

    private AutoBidRow findBestAutoBid(Connection conn,
                                       int auctionId,
                                       String currentLeader,
                                       BigDecimal currentPrice) throws SQLException {
        String sql = "SELECT bidder_username, max_bid, increment_amount FROM auto_bid_configs "
                + "WHERE auction_id = ? AND active = TRUE AND max_bid > ? "
                + "AND bidder_username <> COALESCE(?, '') "
                + "ORDER BY max_bid DESC, registered_at ASC LIMIT 1 FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            ps.setBigDecimal(2, currentPrice);
            ps.setString(3, currentLeader);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new AutoBidRow(
                        rs.getString("bidder_username"),
                        rs.getBigDecimal("max_bid"),
                        rs.getBigDecimal("increment_amount")
                );
            }
        }
    }

    private AuctionState lockAuction(Connection conn, int auctionId) throws SQLException {
        String sql = "SELECT * FROM auctions WHERE auction_id = ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Timestamp endTimestamp = rs.getTimestamp("end_time");
                return new AuctionState(
                        rs.getInt("auction_id"),
                        rs.getBigDecimal("current_price"),
                        getStringIfExists(rs, "highest_bidder"),
                        rs.getString("seller_name"),
                        rs.getString("status"),
                        endTimestamp == null ? null : endTimestamp.toLocalDateTime()
                );
            }
        }
    }

    private String validateBid(AuctionState state, String bidderUsername, BigDecimal amount) {
        if (state.status != null
                && !(state.status.equalsIgnoreCase("OPEN") || state.status.equalsIgnoreCase("RUNNING"))) {
            return "Phiên đấu giá không còn mở.";
        }
        if (state.endTime != null && LocalDateTime.now().isAfter(state.endTime)) {
            return "Phiên đấu giá đã hết hạn.";
        }
        if (state.sellerName != null && state.sellerName.equalsIgnoreCase(bidderUsername)) {
            return "Seller không được bid sản phẩm của chính mình.";
        }
        if (amount.compareTo(state.currentPrice) <= 0) {
            return "Giá bid phải cao hơn giá hiện tại.";
        }
        return null;
    }

    private void updateAuctionPrice(Connection conn,
                                    int auctionId,
                                    BigDecimal amount,
                                    String bidderUsername) throws SQLException {
        String sql = "UPDATE auctions SET current_price = ?, highest_bidder = ?, status = 'RUNNING' WHERE auction_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, amount);
            ps.setString(2, bidderUsername);
            ps.setInt(3, auctionId);
            ps.executeUpdate();
        }
    }

    private void insertBid(Connection conn,
                           int auctionId,
                           String bidderUsername,
                           BigDecimal amount,
                           boolean autoBid) throws SQLException {
        String sql = "INSERT INTO bid_transactions(auction_id, bidder_username, amount, auto_bid) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            ps.setString(2, bidderUsername);
            ps.setBigDecimal(3, amount);
            ps.setBoolean(4, autoBid);
            ps.executeUpdate();
        }
    }

    private void applyAntiSniping(Connection conn, int auctionId, LocalDateTime endTime) throws SQLException {
        if (endTime == null) {
            return;
        }
        long remaining = java.time.Duration.between(LocalDateTime.now(), endTime).toSeconds();
        if (remaining >= 0 && remaining <= ANTI_SNIPING_SECONDS) {
            String sql = "UPDATE auctions SET end_time = DATE_ADD(end_time, INTERVAL ? SECOND) WHERE auction_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, EXTEND_SECONDS);
                ps.setInt(2, auctionId);
                ps.executeUpdate();
            }
        }
    }

    private AuctionView toAuctionView(ResultSet rs) throws SQLException {
        AuctionView view = new AuctionView(
                rs.getInt("auction_id"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getString("seller_name"),
                rs.getDouble("current_price"),
                rs.getString("status"),
                rs.getString("end_time")
        );
        view.setHighestBidderName(getStringIfExists(rs, "highest_bidder"));
        return view;
    }

    private String getStringIfExists(ResultSet rs, String column) {
        try {
            return rs.getString(column);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void ensureRealtimeSchema() {
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement()) {
            try {
                st.executeUpdate("ALTER TABLE auctions ADD COLUMN highest_bidder VARCHAR(100) NULL");
            } catch (Exception ignored) {
            }
            st.executeUpdate("CREATE TABLE IF NOT EXISTS bid_transactions ("
                    + "bid_id INT AUTO_INCREMENT PRIMARY KEY,"
                    + "auction_id INT NOT NULL,"
                    + "bidder_username VARCHAR(100) NOT NULL,"
                    + "amount DECIMAL(18,2) NOT NULL,"
                    + "auto_bid BOOLEAN NOT NULL DEFAULT FALSE,"
                    + "bid_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                    + "INDEX idx_bid_auction_time(auction_id, bid_time)"
                    + ")");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS auto_bid_configs ("
                    + "auction_id INT NOT NULL,"
                    + "bidder_username VARCHAR(100) NOT NULL,"
                    + "max_bid DECIMAL(18,2) NOT NULL,"
                    + "increment_amount DECIMAL(18,2) NOT NULL,"
                    + "active BOOLEAN NOT NULL DEFAULT TRUE,"
                    + "registered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                    + "PRIMARY KEY(auction_id, bidder_username)"
                    + ")");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private record AuctionState(int auctionId,
                                BigDecimal currentPrice,
                                String highestBidder,
                                String sellerName,
                                String status,
                                LocalDateTime endTime) {
    }

    private record AutoBidRow(String bidderUsername, BigDecimal maxBid, BigDecimal increment) {
    }

    public record BidResponse(boolean success, String message) {
        public static BidResponse ok(String message) {
            return new BidResponse(true, message);
        }

        public static BidResponse fail(String message) {
            return new BidResponse(false, message);
        }
    }

    public record BidHistoryRow(String time, String bidder, String amount, String type) {
    }
}
