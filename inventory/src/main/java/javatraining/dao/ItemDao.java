package javatraining.dao;

import javatraining.model.Item;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class ItemDao extends BaseDao<Long, Item> {

}
