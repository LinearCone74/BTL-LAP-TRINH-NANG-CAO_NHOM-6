package com.auction.model.user;

public class Seller extends User {
    public Seller(String username, String passwordHash, String fullName, String email) {
        super(username, passwordHash, fullName, email);
    }

    @Override
    public Role getRole() {
        return Role.SELLER;
    }
}