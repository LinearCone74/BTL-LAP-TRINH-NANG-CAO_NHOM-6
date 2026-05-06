package com.auction.app;

import com.auction.model.auction.Auction;
import com.auction.model.auction.AutoBidConfig;
import com.auction.model.item.Item;
import com.auction.model.user.Admin;
import com.auction.model.user.Bidder;
import com.auction.model.user.Role;
import com.auction.model.user.Seller;
import com.auction.model.user.User;
import com.auction.repository.AuctionRepository;
import com.auction.repository.ItemRepository;
import com.auction.repository.JdbcUserRepository;
import com.auction.repository.UserRepository;
import com.auction.repository.memory.InMemoryAuctionRepository;
import com.auction.repository.memory.InMemoryItemRepository;
import com.auction.repository.memory.InMemoryUserRepository;
import com.auction.service.AuctionScheduler;
import com.auction.service.AuctionService;
import com.auction.service.AuthService;
import com.auction.service.AutoBidService;
import com.auction.service.ItemService;
import com.auction.service.PaymentService;
import com.auction.service.UserService;
import com.auction.util.PasswordHasher;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public class AppContext {
    private final Stage primaryStage;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final AuctionRepository auctionRepository;
    private final AuthService authService;
    private final UserService userService;
    private final ItemService itemService;
    private final AuctionService auctionService;
    private final AutoBidService autoBidService;
    private final PaymentService paymentService;
    private final AuctionScheduler auctionScheduler;
    private final SceneNavigator navigator;
    private User currentUser;

    private AppContext(Stage primaryStage,
                       UserRepository userRepository,
                       ItemRepository itemRepository,
                       AuctionRepository auctionRepository,
                       AuthService authService,
                       UserService userService,
                       ItemService itemService,
                       AuctionService auctionService,
                       AutoBidService autoBidService,
                       PaymentService paymentService,
                       AuctionScheduler auctionScheduler) {
        this.primaryStage = primaryStage;
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
        this.auctionRepository = auctionRepository;
        this.authService = authService;
        this.userService = userService;
        this.itemService = itemService;
        this.auctionService = auctionService;
        this.autoBidService = autoBidService;
        this.paymentService = paymentService;
        this.auctionScheduler = auctionScheduler;
        this.navigator = new SceneNavigator(this, primaryStage);
    }

    public static AppContext bootstrap(Stage primaryStage) {
        UserRepository userRepository = new JdbcUserRepository();
        ItemRepository itemRepository = new InMemoryItemRepository();
        AuctionRepository auctionRepository = new InMemoryAuctionRepository();

        AuthService authService = new AuthService(userRepository);
        UserService userService = new UserService(userRepository);
        ItemService itemService = new ItemService(itemRepository);
        AuctionService auctionService = new AuctionService(auctionRepository);
        AutoBidService autoBidService = new AutoBidService();
        PaymentService paymentService = new PaymentService();
        AuctionScheduler auctionScheduler = new AuctionScheduler();

        AppContext context = new AppContext(
                primaryStage,
                userRepository,
                itemRepository,
                auctionRepository,
                authService,
                userService,
                itemService,
                auctionService,
                autoBidService,
                paymentService,
                auctionScheduler
        );

//        context.seedData();
        auctionScheduler.start();
        return context;
    }

    private void seedData() {
        Admin admin = (Admin) authService.register(
                "admin",
                PasswordHasher.hash("admin123"),
                "Quản trị viên",
                "admin@auctionhub.vn",
                Role.ADMIN
        );

        Seller seller = (Seller) authService.register(
                "seller",
                PasswordHasher.hash("seller123"),
                "Người bán demo",
                "seller@auctionhub.vn",
                Role.SELLER
        );

        Bidder bidder = (Bidder) authService.register(
                "bidder",
                PasswordHasher.hash("bidder123"),
                "Người mua demo",
                "bidder@auctionhub.vn",
                Role.BIDDER
        );

        bidder.deposit(BigDecimal.valueOf(500_000_000L));
        userService.save(bidder);

        Item laptop = itemService.create(
                "electronics",
                "Laptop Gaming LOQ",
                "i7-13650HX RTX 4060, 16GB RAM",
                BigDecimal.valueOf(18_000_000),
                seller,
                Map.of("Thương hiệu", "Lenovo", "Tháng bảo hành", 24)
        );

        Item painting = itemService.create(
                "art",
                "Tranh sơn dầu phố cổ",
                "Tác phẩm trang trí phòng khách",
                BigDecimal.valueOf(7_500_000),
                seller,
                Map.of("Họa sĩ", "Nguyễn An", "Năm sáng tác", 2022)
        );

        Item motorbike = itemService.create(
                "vehicle",
                "Honda SH 150i",
                "Xe đẹp, odo 12.000 km",
                BigDecimal.valueOf(72_000_000),
                seller,
                Map.of("Nhà sản xuất", "Honda", "Số km", 12000)
        );

        Auction auction1 = auctionService.createAuction(
                laptop,
                LocalDateTime.now().minusMinutes(2),
                LocalDateTime.now().plusMinutes(18)
        );
        auction1.configureAntiSniping(20, 60);
        auctionService.startAuction(auction1.getId());
        auctionService.placeManualBid(
                auction1.getId(),
                bidder,
                BigDecimal.valueOf(18_500_000)
        );

        Auction auction2 = auctionService.createAuction(
                painting,
                LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now().plusMinutes(8)
        );
        auction2.configureAntiSniping(15, 45);
        auctionService.startAuction(auction2.getId());

        Auction auction3 = auctionService.createAuction(
                motorbike,
                LocalDateTime.now().plusMinutes(5),
                LocalDateTime.now().plusMinutes(35)
        );
        auction3.registerAutoBid(
                new AutoBidConfig(
                        bidder,
                        BigDecimal.valueOf(80_000_000),
                        BigDecimal.valueOf(1_000_000)
                )
        );

        userService.save(admin);
        userService.save(seller);
        userService.save(bidder);
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public AuthService getAuthService() {
        return authService;
    }

    public UserService getUserService() {
        return userService;
    }

    public ItemService getItemService() {
        return itemService;
    }

    public AuctionService getAuctionService() {
        return auctionService;
    }

    public AutoBidService getAutoBidService() {
        return autoBidService;
    }

    public PaymentService getPaymentService() {
        return paymentService;
    }

    public SceneNavigator getNavigator() {
        return navigator;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }
}