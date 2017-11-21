package javatraining.controller;

import com.google.common.collect.ImmutableList;
import javatraining.model.Category;
import javatraining.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;

    @Autowired
    public CategoryController(CategoryService CategoryService) {
        this.categoryService = CategoryService;
    }

    @GetMapping
    public Iterable<Category> categories() {
        return categoryService.findAll();
    }

    @PostMapping
    public Category save(@RequestBody Category category) {
        return categoryService.save(category);
    }
}
