package chuc_nang_bat_buoc;

import java.time.LocalDateTime;

public class MainTest {
    public static void main(String[] args) {
        UserService userService = new UserService();
        AuctionItemService itemService = new AuctionItemService();

        Seller seller1 = (Seller) userService.register("sellerA", "123", Role.SELLER);
        Bidder bidder1 = (Bidder) userService.register("bidderA", "456", Role.BIDDER);
        Admin admin1 = (Admin) userService.register("adminA", "789", Role.ADMIN);

        System.out.println("=== USERS ===");
        userService.showAllUsers();

        System.out.println("=== LOGIN ===");
        User loggedIn = userService.login("sellerA", "123");
        System.out.println("Login success: " + loggedIn.getUsername() + " - " + loggedIn.getRole());

        itemService.addItem(
                "Laptop Dell",
                "Laptop gaming",
                1000,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(3),
                seller1
        );

        itemService.addItem(
                "iPhone 14",
                "Used phone, good condition",
                500,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(2),
                seller1
        );

        System.out.println("\n=== ITEMS AFTER ADD ===");
        itemService.showAllItems();

        itemService.updateItem(
                1,
                "Laptop Dell G15",
                "Updated description",
                1200,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(5)
        );

        System.out.println("\n=== ITEMS AFTER UPDATE ===");
        itemService.showAllItems();

        itemService.deleteItem(2);

        System.out.println("\n=== ITEMS AFTER DELETE ===");
        itemService.showAllItems();
    }
}