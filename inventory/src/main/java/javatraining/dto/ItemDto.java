package javatraining.dto;

import javatraining.model.Category;
import lombok.Data;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.math.BigDecimal;

@Data
public class ItemDto {

    private Long id;
    private BigDecimal price;
    private String name;
    private Long categoryId;
    private String categoryName;

}
