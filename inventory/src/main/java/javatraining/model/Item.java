package javatraining.model;

import lombok.Data;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.math.BigDecimal;

@Data
@Entity(name = "items")
@Table(name = "items")
@NamedEntityGraph(name="joinCategory", attributeNodes = {
        @NamedAttributeNode("category")
})
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "item_id")
    private Long id;
    private BigDecimal price;
    private String name;
    @ManyToOne(fetch = FetchType.EAGER)
    @Fetch(FetchMode.JOIN)
    @JoinColumn(name = "category_id", referencedColumnName = "category_id")
    private Category category;
}
