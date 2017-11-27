package javatraining.dao;

import javatraining.model.Category;
import org.springframework.stereotype.Repository;

@Repository("categoryDao")
public class CategoryDao extends BaseDao<Long, Category> {

}
