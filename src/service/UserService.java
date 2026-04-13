package service;

import exception.AuthenticationException;
import model.*;

import java.util.ArrayList;
import java.util.List;

public class UserService {
    private List<User> users = new ArrayList<>();

    public User register(String username, String password, Role role) {
        User user;

        switch (role) {
            case BIDDER:
                user = new Bidder(username, password);
                break;
            case SELLER:
                user = new Seller(username, password);
                break;
            case ADMIN:
                user = new Admin(username, password);
                break;
            default:
                throw new IllegalArgumentException("Role khong hop le");
        }

        users.add(user);
        return user;
    }

    public User login(String username, String password) throws AuthenticationException {
        for (User user : users) {
            if (user.getUsername().equals(username) && user.getPassword().equals(password)) {
                return user;
            }
        }
        throw new AuthenticationException("Sai username hoac password.");
    }

    public List<User> getUsers() {
        return users;
    }

    public void printAllUsers() {
        for (User user : users) {
            user.printInfo();
        }
    }
}