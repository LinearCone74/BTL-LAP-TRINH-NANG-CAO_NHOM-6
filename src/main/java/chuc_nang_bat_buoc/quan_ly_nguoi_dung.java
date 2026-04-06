package chuc_nang_bat_buoc;

import java.util.ArrayList;
import java.util.List;

enum Role {
    BIDDER, SELLER, ADMIN
}

abstract class User {
    private int id;
    private String username;
    private String password;
    private Role role;

    public User(int id, String username, String password, Role role) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public Role getRole() {
        return role;
    }

    public void printInfo() {
        System.out.println("ID: " + id + ", username: " + username + ", role: " + role);
    }
}

class Bidder extends User {
    public Bidder(int id, String username, String password) {
        super(id, username, password, Role.BIDDER);
    }
}

class Seller extends User {
    public Seller(int id, String username, String password) {
        super(id, username, password, Role.SELLER);
    }
}

class Admin extends User {
    public Admin(int id, String username, String password) {
        super(id, username, password, Role.ADMIN);
    }
}

class UserService {
    private List<User> users = new ArrayList<>();
    private int nextId = 1;

    public User register(String username, String password, Role role) {
        if (findByUsername(username) != null) {
            throw new IllegalArgumentException("Username already exists");
        }

        User user;
        if (role == Role.BIDDER) {
            user = new Bidder(nextId++, username, password);
        } else if (role == Role.SELLER) {
            user = new Seller(nextId++, username, password);
        } else {
            user = new Admin(nextId++, username, password);
        }

        users.add(user);
        return user;
    }

    public User login(String username, String password) {
        for (User user : users) {
            if (user.getUsername().equals(username) &&
                    user.getPassword().equals(password)) {
                return user;
            }
        }
        throw new IllegalArgumentException("Invalid username or password");
    }

    public User findByUsername(String username) {
        for (User user : users) {
            if (user.getUsername().equals(username)) {
                return user;
            }
        }
        return null;
    }

    public void showAllUsers() {
        for (User user : users) {
            user.printInfo();
        }
    }
}