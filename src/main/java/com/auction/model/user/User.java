package com.auction.model.user;
import com.auction.model.base.Entity;
public abstract class User extends Entity{
    private final String username;
    private String passwordHash;
    private String fullName;
    private boolean active;
    private String email;
    
    protected User(String username, String passwordHash, String fullName, String email) {
        super();
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.email = email;
        this.active = true;
    }

    public abstract Role getRole();

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
        touch();
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
        touch();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
        touch();
    }

    public boolean isActive() {
        return active;
    }

    public void activate() {
        this.active = true;
        touch();
    }

    public void deactivate() {
        this.active = false;
        touch();
    }

    public String printInfo() {
        return "%s | %s | %s | %s".formatted(getRole(), username, fullName, email);
    }
}