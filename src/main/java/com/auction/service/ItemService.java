package com.auction.service;

import com.auction.factory.ItemFactory;
import com.auction.model.item.Item;
import com.auction.model.item.ItemStatus;
import com.auction.model.user.Seller;
import com.auction.repository.ItemRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class ItemService {
    private final ItemRepository itemRepository;

    public ItemService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    public Item create(String type, String name, String description, BigDecimal startingPrice,
                       Seller seller, Map<String, Object> extra) {
        Item item = ItemFactory.createItem(type, name, description, startingPrice, seller, extra);
        item.setStatus(ItemStatus.READY);
        return itemRepository.save(item);
    }

    public Item update(Item item) {
        return itemRepository.save(item);
    }

    public void delete(String id) {
        itemRepository.deleteById(id);
    }

    public List<Item> getAll() {
        return itemRepository.findAll();
    }
}