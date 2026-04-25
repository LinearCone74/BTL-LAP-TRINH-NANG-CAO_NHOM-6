package com.auction.repository.memory;

import com.auction.model.item.Item;
import com.auction.repository.ItemRepository;

public class InMemoryItemRepository extends InMemoryCrudRepository<Item> implements ItemRepository {
}