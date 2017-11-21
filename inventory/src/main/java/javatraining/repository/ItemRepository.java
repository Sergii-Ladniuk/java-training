package javatraining.repository;

import javatraining.model.Item;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemRepository extends CrudRepository<Item, Long> {

    @EntityGraph(value = "joinCategory", type = EntityGraph.EntityGraphType.FETCH)
    @Override
    Iterable<Item> findAll();
}
