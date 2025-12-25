package com.profmojo.repositories;

import com.profmojo.models.CanteenFolder.CanteenOrder;
import com.profmojo.models.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CanteenOrderRepository
        extends JpaRepository<CanteenOrder, Long> {

    List<CanteenOrder> findByCanteenIdOrderByCreatedAtDesc(String canteenId);

    List<CanteenOrder> findByCanteenIdAndStatusNot(
            String canteenId,
            OrderStatus status
    );

    List<CanteenOrder> findByCanteenIdAndStatusIn(String canteenId, List<OrderStatus> status);
    List<CanteenOrder> findByCanteenIdAndStatus(String canteenId, OrderStatus status);

    @Query("""
SELECT o FROM CanteenOrder o
WHERE o.canteenId = :canteenId
AND o.status IN ('PLACED', 'PREPARING', 'READY')
ORDER BY o.createdAt DESC
""")
    List<CanteenOrder> findActiveOrders(String canteenId);


}
