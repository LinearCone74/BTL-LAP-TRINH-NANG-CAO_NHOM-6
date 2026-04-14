package com.auction.model.user;

public class Admin extends User {
    public Admin(String username, String passwordHash, String fullName, String email) {
        super(username, passwordHash, fullName, email);
    }

    @Override
    public Role getRole() {
        return Role.ADMIN;
    }
}