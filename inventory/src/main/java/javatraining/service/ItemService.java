package javatraining.service;

import javatraining.dao.ItemDao;
import javatraining.model.Item;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ItemService {

    private final ItemDao dao;

    @Autowired
    public ItemService(ItemDao itemDao) {
        this.dao = itemDao;
    }

    public Item save(Item category) {
        dao.save(category);
        return findOne(category.getId());
    }

    public Item findOne(Long id) {
        return dao.findById(id);
    }

    public Iterable<Item> findAll() {
        return dao.findAll();
    }

    public void delete(Long id) {
        dao.delete(id);
    }
}
