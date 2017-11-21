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
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", referencedColumnName = "category_id")
    private List<ItemShort> items;
}
