package model;

public abstract class User {
    private static int AUTO_ID = 1;

    private int id;
    private String username;
    private String password;
    private Role role;

    public User(String username, String password, Role role) {
        this.id = AUTO_ID++;
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