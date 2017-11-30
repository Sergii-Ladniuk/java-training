package javatraining.model;

import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Set;

@Data
@Entity(name = "items")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "item_id")
    private Long id;
    private BigDecimal price;
    private String name;
    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinTable(name = "items_categories", joinColumns = {
            @JoinColumn(name = "item_id") }, inverseJoinColumns = {
            @JoinColumn(name = "category_id")
    })
    private Set<Category> categories;

}
