package com.auction.model.item;

import java.math.BigDecimal;

import com.auction.model.user.Seller;

public class Electronics extends Item {
    private final String brand;
    private final int warrantyMonths;

    public Electronics(String name, String description, BigDecimal startingPrice,Seller seller, String brand, int warrantyMonths) {
        super(name, description, startingPrice, seller);
        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
    }

    @Override
    public String getCategory() {
        return "Electronics";
    }

    public String getBrand() {
        return brand;
    }

    public int getWarrantyMonths() {
        return warrantyMonths;
    }
}