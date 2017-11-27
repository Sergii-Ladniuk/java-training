package javatraining.dao;

import org.hibernate.Criteria;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.List;

@Transactional
public class BaseDao<PK extends Serializable, T> extends AbstractDao<PK, T> {

    public T findById(PK id) {
        return getByKey(id);
    }

    public void save(T object) {
        persist(object);
    }

    public void delete(T object) {
        super.delete(object);
    }

    public void delete(PK id) {
        delete(getByKey(id));
    }

    public List<T> findAll() {
        Criteria criteria = createEntityCriteria();
        return (List<T>) criteria.list();
    }
}
