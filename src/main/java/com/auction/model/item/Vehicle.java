package com.auction.model.item;

import java.math.BigDecimal;

import com.auction.model.user.Seller;

public class Vehicle extends Item {
    private final String manufacturer;
    private final int mileage;

    public Vehicle(String name, String description, BigDecimal startingPrice,Seller seller, String manufacturer, int mileage) {
        super(name, description, startingPrice, seller);
        this.manufacturer = manufacturer;
        this.mileage = mileage;
    }

    @Override
    public String getCategory() {
        return "Vehicle";
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public int getMileage() {
        return mileage;
    }
}