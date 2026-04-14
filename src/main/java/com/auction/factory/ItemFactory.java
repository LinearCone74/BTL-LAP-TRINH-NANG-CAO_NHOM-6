package com.auction.factory;

import java.math.BigDecimal;
import java.util.Map;

import com.auction.model.item.Art;
import com.auction.model.item.Electronics;
import com.auction.model.item.Item;
import com.auction.model.item.Vehicle;
import com.auction.model.user.Seller;

public final class ItemFactory {
    private ItemFactory() {
    }

    public static Item createItem(String type,String name,String description,BigDecimal startingPrice,Seller seller,Map<String, Object> extra) {
        return switch (type.toLowerCase()) {
            case "electronics" -> new Electronics(
                    name,
                    description,
                    startingPrice,
                    seller,
                    (String) extra.getOrDefault("brand", "Unknown"),
                    (Integer) extra.getOrDefault("warrantyMonths", 0)
            );
            case "art" -> new Art(
                    name,
                    description,
                    startingPrice,
                    seller,
                    (String) extra.getOrDefault("artist", "Unknown"),
                    (Integer) extra.getOrDefault("yearCreated", 0)
            );
            case "vehicle" -> new Vehicle(
                    name,
                    description,
                    startingPrice,
                    seller,
                    (String) extra.getOrDefault("manufacturer", "Unknown"),
                    (Integer) extra.getOrDefault("mileage", 0)
            );
            default -> throw new IllegalArgumentException("Unknown item type: " + type);
        };
    }
}