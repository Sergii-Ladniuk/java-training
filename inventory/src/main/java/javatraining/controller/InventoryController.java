package javatraining.controller;

import javatraining.dto.ItemDto;
import javatraining.model.Item;
import javatraining.service.ItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/items")
public class InventoryController {

    private final ItemService itemService;

    @Autowired
    public InventoryController(ItemService itemService) {
        this.itemService = itemService;
    }

    @GetMapping
    public Iterable<ItemDto> items() {
        return itemService.findAll();
    }

    @PostMapping
    public Item save(@RequestBody Item item) {
        return itemService.save(item);
    }
}
