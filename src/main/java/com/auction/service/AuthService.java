package com.auction.service;

import com.auction.exception.ValidationException;
import com.auction.model.user.Admin;
import com.auction.model.user.Bidder;
import com.auction.model.user.Role;
import com.auction.model.user.Seller;
import com.auction.model.user.User;
import com.auction.repository.UserRepository;

public class AuthService {
    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User register(String username, String passwordHash, String fullName, String email, Role role) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new ValidationException("Username already exists");
        }

        User user = switch (role) {
            case BIDDER -> new Bidder(username, passwordHash, fullName, email);
            case SELLER -> new Seller(username, passwordHash, fullName, email);
            case ADMIN -> new Admin(username, passwordHash, fullName, email);
        };

        return userRepository.save(user);
    }

    public User login(String username, String passwordHash) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ValidationException("User not found"));

        if (!user.getPasswordHash().equals(passwordHash)) {
            throw new ValidationException("Invalid password");
        }

        return user;
    }
}

// xác thực tài khoàn