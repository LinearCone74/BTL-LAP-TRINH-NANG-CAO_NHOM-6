package com.auction.repository;

import com.auction.model.user.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryUserRepository implements UserRepository {
    private final Map<String, User> data = new ConcurrentHashMap<>();

    @Override
    public User save(User entity) {
        data.put(entity.getId(), entity);
        return entity;
    }

    @Override
    public Optional<User> findById(String id) {
        return Optional.ofNullable(data.get(id));
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(data.values());
    }

    @Override
    public void deleteById(String id) {
        data.remove(id);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return data.values().stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst();
    }
}