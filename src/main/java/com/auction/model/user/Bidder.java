package com.auction.model.user;

import java.math.BigDecimal;

public class Bidder extends User {
    private BigDecimal walletBalance;

    public Bidder(String username, String passwordHash, String fullName, String email) {
        super(username, passwordHash, fullName, email);
        this.walletBalance = BigDecimal.ZERO;
    }

    @Override
    public Role getRole() {
        return Role.BIDDER;
    }

    public BigDecimal getWalletBalance() {
        return walletBalance;
    }

    public void deposit(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Số tiền phải là số dương");
        }
        walletBalance = walletBalance.add(amount);
        touch();
    }

    public void withdraw(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Số tiền phải là số dương");
        }
        if (walletBalance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Số dư không đủ");
        }
        walletBalance = walletBalance.subtract(amount);
        touch();
    }
}

