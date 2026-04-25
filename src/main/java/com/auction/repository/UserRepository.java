package com.auction.repository;

import com.auction.model.user.User;

import java.util.Optional;

public interface UserRepository extends CrudRepository<User, String> {
    Optional<User> findByUsername(String username);
}

// Lưu trữ người dùng