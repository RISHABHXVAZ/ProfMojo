package com.profmojo.models.dto;

import lombok.Data;

import java.util.List;

@Data
public class PlaceOrderRequest {
    private String canteenId;
    private String cabinLocation;
    private List<OrderItemRequest> items;
    private Double totalAmount;
    private String paymentMode;
}
