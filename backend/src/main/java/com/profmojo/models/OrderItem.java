package com.profmojo.models;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class OrderItem {
    private Long itemId;
    private String itemName;
    private Integer quantity;
}
