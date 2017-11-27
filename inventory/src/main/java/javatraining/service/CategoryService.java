package javatraining.service;

import javatraining.dao.CategoryDao;
import javatraining.model.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CategoryService {

    private final CategoryDao dao;

    @Autowired
    public CategoryService(CategoryDao categoryDao) {
        this.dao = categoryDao;
    }

    public Category save(Category category) {
        dao.save(category);
        return findOne(category.getId());
    }

    public Category findOne(Long id) {
        return dao.findById(id);
    }

    public Iterable<Category> findAll() {
        return dao.findAll();
    }

    public void delete(Long id) {
        dao.delete(id);
    }
}
