package javatraining.service;

import javatraining.dto.ItemDto;
import javatraining.model.Category;
import javatraining.model.Item;
import javatraining.repository.ItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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

    public List<ItemDto> findAll() {
        Iterable<Item> items = itemRepository.findAll();
        return StreamSupport.stream(items.spliterator(), false)
                .map(item -> {
                    ItemDto dto = new ItemDto();
                    Category category = item.getCategory();
                    dto.setCategoryId(category.getId());
                    dto.setCategoryName(category.getName());
                    dto.setId(item.getId());
                    dto.setName(item.getName());
                    dto.setPrice(item.getPrice());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public void delete(Long id) {
        itemRepository.delete(id);
    }
}
