package com.auction.repository.memory;

import com.auction.model.user.User;
import com.auction.repository.UserRepository;

import java.util.Optional;

public class InMemoryUserRepository extends InMemoryCrudRepository<User> implements UserRepository {
    @Override
    public Optional<User> findByUsername(String username) {
        return storage.values().stream()
                .filter(user -> user.getUsername().equalsIgnoreCase(username))
                .findFirst();
    }
}