package com.auction.model.item;
import java.math.BigDecimal;

import com.auction.model.base.Entity;
import com.auction.model.user.Seller;

public abstract class Item extends Entity {
    private String name;
    private String description;
    private BigDecimal startingPrice;
    private BigDecimal currentHighestPrice;
    private final Seller seller;
    private ItemStatus status;

    protected Item(String name, String description, BigDecimal startingPrice, Seller seller) {
        super();
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.currentHighestPrice = startingPrice;
        this.seller = seller;
        this.status = ItemStatus.DRAFT;
    }

    public abstract String getCategory();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        touch();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        touch();
    }

    public BigDecimal getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(BigDecimal startingPrice) {
        this.startingPrice = startingPrice;
        touch();
    }

    public BigDecimal getCurrentHighestPrice() {
        return currentHighestPrice;
    }

    public void setCurrentHighestPrice(BigDecimal currentHighestPrice) {
        this.currentHighestPrice = currentHighestPrice;
        touch();
    }

    public Seller getSeller() {
        return seller;
    }

    public ItemStatus getStatus() {
        return status;
    }

    public void setStatus(ItemStatus status) {
        this.status = status;
        touch();
    }

    public String printInfo() {
        return "%s | %s | start=%s | current=%s".formatted(getCategory(), name, startingPrice, currentHighestPrice);
    }
}