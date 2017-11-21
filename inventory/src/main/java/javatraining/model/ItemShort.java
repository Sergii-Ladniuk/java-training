package javatraining.model;

import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;

@Data
@Entity(name = "items-short")
@Table(name = "items")
public class ItemShort {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "item_id")
    private Long id;
    private BigDecimal price;
    private String name;
}
