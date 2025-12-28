package com.profmojo.models.dto;

import lombok.Data;

@Data
public class OrderItemRequest {
    private Long itemId;
    private String itemName;
    private Integer quantity;
}

