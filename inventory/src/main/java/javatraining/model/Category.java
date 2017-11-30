package javatraining.model;

import lombok.Data;

import javax.persistence.*;
import java.util.List;

@Data
@Entity(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "category_id")
    private Long id;
    private String name;
    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinTable(name = "items_categories", joinColumns = {
            @JoinColumn(name = "category_id") }, inverseJoinColumns = {
            @JoinColumn(name = "item_id")
    })
    private List<ItemShort> items;
}
