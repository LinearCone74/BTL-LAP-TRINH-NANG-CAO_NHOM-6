package model;

public class Bidder extends User {
    public Bidder(String username, String password) {
        super(username, password, Role.BIDDER);
    }
}