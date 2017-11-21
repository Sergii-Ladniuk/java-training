package javatraining.service;

import javatraining.model.Item;
import javatraining.repository.ItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

@Service
public class ItemService {

    private final ItemRepository itemRepository;
    private final EntityManager entityManager;

    @Autowired
    public ItemService(ItemRepository itemRepository, EntityManager entityManager) {
        this.itemRepository = itemRepository;
        this.entityManager = entityManager;
    }

    public <S extends Item> S save(S s) {
        return itemRepository.save(s);
    }

    public Item findOne(Long aLong) {
        return itemRepository.findOne(aLong);
    }

    public Iterable<Item> findAll() {
        Iterable<Item> all = itemRepository.findAll();
        return all;
    }

    public void delete(Long id) {
        itemRepository.delete(id);
    }
}
