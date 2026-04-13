package model;

public class Seller extends User {
    public Seller(String username, String password) {
        super(username, password, Role.SELLER);
    }
}