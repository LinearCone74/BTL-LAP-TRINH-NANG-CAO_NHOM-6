package chuc_nang_bat_buoc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

class AuctionItem {
    private int id;
    private String name;
    private String description;
    private double startPrice;
    private double currentHighestPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Seller seller;

    public AuctionItem(int id, String name, String description, double startPrice,
                       LocalDateTime startTime, LocalDateTime endTime, Seller seller) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.startPrice = startPrice;
        this.currentHighestPrice = startPrice;
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

    public double getCurrentHighestPrice() {
        return currentHighestPrice;
    }

    public void setCurrentHighestPrice(double currentHighestPrice) {
        this.currentHighestPrice = currentHighestPrice;
    }

    public void updateInfo(String name, String description, double startPrice,
                           LocalDateTime startTime, LocalDateTime endTime) {
        this.name = name;
        this.description = description;
        this.startPrice = startPrice;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public void printInfo() {
        System.out.println("Item ID: " + id);
        System.out.println("Name: " + name);
        System.out.println("Description: " + description);
        System.out.println("Start price: " + startPrice);
        System.out.println("Current highest price: " + currentHighestPrice);
        System.out.println("Start time: " + startTime);
        System.out.println("End time: " + endTime);
        System.out.println("Seller: " + seller.getUsername());
        System.out.println("-------------------------");
    }
}

class AuctionItemService {
    private List<AuctionItem> items = new ArrayList<>();
    private int nextId = 1;

    public AuctionItem addItem(String name, String description, double startPrice,
                               LocalDateTime startTime, LocalDateTime endTime, Seller seller) {
        AuctionItem item = new AuctionItem(nextId++, name, description, startPrice, startTime, endTime, seller);
        items.add(item);
        return item;
    }

    public void updateItem(int id, String name, String description, double startPrice,
                           LocalDateTime startTime, LocalDateTime endTime) {
        AuctionItem item = findById(id);
        if (item == null) {
            throw new IllegalArgumentException("Item not found");
        }
        item.updateInfo(name, description, startPrice, startTime, endTime);
    }

    public void deleteItem(int id) {
        AuctionItem item = findById(id);
        if (item == null) {
            throw new IllegalArgumentException("Item not found");
        }
        items.remove(item);
    }

    public AuctionItem findById(int id) {
        for (AuctionItem item : items) {
            if (item.getId() == id) {
                return item;
            }
        }
        return null;
    }

    public void showAllItems() {
        for (AuctionItem item : items) {
            item.printInfo();
        }
    }
}