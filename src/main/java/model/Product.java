package model;

import java.time.LocalDateTime;

public class Product {
    private static int AUTO_ID = 1;

    private int id;
    private String name;
    private String description;
    private double startingPrice;
    private double highestPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Seller seller;

    public Product(String name, String description, double startingPrice,
                   LocalDateTime startTime, LocalDateTime endTime, Seller seller) {
        this.id = AUTO_ID++;
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.highestPrice = startingPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.seller = seller;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    public double getHighestPrice() {
        return highestPrice;
    }

    public void setHighestPrice(double highestPrice) {
        this.highestPrice = highestPrice;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public Seller getSeller() {
        return seller;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStartingPrice(double startingPrice) {
        this.startingPrice = startingPrice;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public void printInfo() {
        System.out.println("Product ID: " + id);
        System.out.println("Name: " + name);
        System.out.println("Description: " + description);
        System.out.println("Starting Price: " + startingPrice);
        System.out.println("Highest Price: " + highestPrice);
        System.out.println("Start Time: " + startTime);
        System.out.println("End Time: " + endTime);
        System.out.println("Seller: " + seller.getUsername());
    }
}