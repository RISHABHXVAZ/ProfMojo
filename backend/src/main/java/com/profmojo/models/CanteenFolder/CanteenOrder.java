package com.profmojo.models.CanteenFolder;

import com.profmojo.models.OrderItem;
import com.profmojo.models.enums.OrderStatus;
import com.profmojo.models.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "canteen_orders")
public class CanteenOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String professorId;
    private String professorName;


    private String canteenId;

    private String cabinLocation;

    @ElementCollection
    @CollectionTable(
            name = "canteen_order_items",
            joinColumns = @JoinColumn(name = "order_id")
    )
    private List<OrderItem> items;


    private double totalAmount;

    private String canteenContactNo;

    private String paymentMode;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;


    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private LocalDateTime createdAt;
}

