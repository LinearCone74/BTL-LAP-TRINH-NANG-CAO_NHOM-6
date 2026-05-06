package com.auction.repository;

import database.DBConnection;
import com.auction.model.user.Admin;
import com.auction.model.user.Bidder;
import com.auction.model.user.Role;
import com.auction.model.user.Seller;
import com.auction.model.user.User;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcUserRepository implements UserRepository {

    // Lưu user vào MySQL
    @Override
    public User save(User user) {
        try {
            Connection conn = DBConnection.getConnection();

            String sql = "INSERT INTO users(username, password, role) VALUES (?, ?, ?)";

            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getRole().name());

            ps.executeUpdate();

            System.out.println("Lưu user vào MySQL thành công!");

        } catch (Exception e) {
            e.printStackTrace();
        }

        return user;
    }
    // Tìm user theo username để đăng nhập
    @Override
    public Optional<User> findByUsername(String username) {
        try {
            Connection conn = DBConnection.getConnection();

            String sql = "SELECT * FROM users WHERE username = ?";

            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1, username);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                User user = createUserFromResultSet(rs);
                return Optional.of(user);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    // Hàm phụ: chuyển dữ liệu MySQL thành object User
    private User createUserFromResultSet(ResultSet rs) throws Exception {
        String username = rs.getString("username");
        String password = rs.getString("password");
        String role = rs.getString("role");

        return switch (Role.valueOf(role)) {
            case BIDDER -> new Bidder(username, password, username, "");
            case SELLER -> new Seller(username, password, username, "");
            case ADMIN -> new Admin(username, password, username, "");
        };
    }

    @Override
    public Optional<User> findById(String id) {
        return Optional.empty();
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>();
    }

    @Override
    public void deleteById(String id) {
    }
}