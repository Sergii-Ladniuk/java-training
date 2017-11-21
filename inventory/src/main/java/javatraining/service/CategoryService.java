package javatraining.service;

import javatraining.model.Category;
import javatraining.model.Item;
import javatraining.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Autowired
    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public <S extends Category> S save(S s) {
        return categoryRepository.save(s);
    }

    public Category findOne(Long aLong) {
        return categoryRepository.findOne(aLong);
    }

    public Iterable<Category> findAll() {
        return categoryRepository.findAll();
    }

    public void delete(Long id) {
        categoryRepository.delete(id);
    }
}
