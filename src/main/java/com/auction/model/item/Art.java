package com.auction.model.item;

import java.math.BigDecimal;

import com.auction.model.user.Seller;

public class Art extends Item {
    private final String artist;
    private final int yearCreated;

    public Art(String name, String description, BigDecimal startingPrice,Seller seller, String artist, int yearCreated) {
        super(name, description, startingPrice, seller);
        this.artist = artist;
        this.yearCreated = yearCreated;
    }

    @Override
    public String getCategory() {
        return "Art";
    }

    public String getArtist() {
        return artist;
    }

    public int getYearCreated() {
        return yearCreated;
    }
}