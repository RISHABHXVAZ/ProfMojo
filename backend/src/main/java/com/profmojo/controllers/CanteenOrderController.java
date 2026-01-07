package com.profmojo.controllers;

import com.profmojo.models.CanteenFolder.CanteenOrder;
import com.profmojo.models.OrderItem;
import com.profmojo.models.Professor;
import com.profmojo.models.dto.PlaceOrderRequest;
import com.profmojo.models.dto.UpdateOrderStatusRequest;
import com.profmojo.models.enums.OrderStatus;
import com.profmojo.models.enums.PaymentStatus;
import com.profmojo.repositories.CanteenOrderRepository;
import com.profmojo.repositories.CanteenRepository;
import com.profmojo.repositories.ProfessorRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

import static com.profmojo.models.enums.OrderStatus.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class CanteenOrderController {

    private final CanteenOrderRepository orderRepository;
    private final ProfessorRepository professorRepository;
    private final CanteenRepository canteenRepository;
    // ================= PROFESSOR =================
    @PostMapping("/place")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<?> placeOrder(
            @RequestBody PlaceOrderRequest request,
            HttpServletRequest httpRequest
    ) {
        String professorId = (String) httpRequest.getAttribute("username");

        Professor professor = professorRepository.findById(professorId)
                .orElseThrow(() -> new RuntimeException("Professor not found"));

        // 🔹 NEW: Fetch Canteen details to get the contact number
        var canteen = canteenRepository.findById(request.getCanteenId())
                .orElseThrow(() -> new RuntimeException("Canteen not found"));

        List<OrderItem> orderItems = request.getItems().stream()
                .map(dto -> {
                    OrderItem item = new OrderItem();
                    item.setItemId(dto.getItemId());
                    item.setItemName(dto.getItemName());
                    item.setQuantity(dto.getQuantity());
                    return item;
                })
                .toList();

        CanteenOrder order = CanteenOrder.builder()
                .professorId(professorId)
                .professorName(professor.getName())
                .canteenId(request.getCanteenId())
                .canteenContactNo(canteen.getContactNo())
                .cabinLocation(request.getCabinLocation())
                .paymentMode(request.getPaymentMode())
                .paymentStatus(
                        request.getPaymentMode().equals("UPI")
                                ? PaymentStatus.PENDING
                                : PaymentStatus.PAID
                )
                .items(orderItems)
                .totalAmount(request.getTotalAmount())
                .status(OrderStatus.PLACED)
                .createdAt(LocalDateTime.now())
                .build();


        return ResponseEntity.ok(orderRepository.save(order));
    }

    // ================= CANTEEN =================
    @GetMapping("/canteen/my")
    @PreAuthorize("hasRole('CANTEEN')")
    public ResponseEntity<?> getMyOrders(HttpServletRequest request) {
        String canteenId = (String) request.getAttribute("username");

        return ResponseEntity.ok(
                orderRepository.findByCanteenIdAndStatusNot(
                        canteenId,
                        OrderStatus.DELIVERED
                )
        );
    }

    @PutMapping("/status")
    @PreAuthorize("hasRole('CANTEEN')")
    public ResponseEntity<?> updateStatus(
            @RequestBody UpdateOrderStatusRequest request
    ) {
        CanteenOrder order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setStatus(OrderStatus.valueOf(request.getStatus()));
        return ResponseEntity.ok(orderRepository.save(order));
    }

    @GetMapping("/active")
    @PreAuthorize("hasRole('CANTEEN')")
    public List<CanteenOrder> getActiveOrders(HttpServletRequest req) {
        String canteenId = (String) req.getAttribute("username");

        return orderRepository.findByCanteenIdAndStatusIn(
                canteenId,
                List.of(PLACED, PREPARING, READY)
        );
    }

    @GetMapping("/history")
    @PreAuthorize("hasRole('CANTEEN')")
    public List<CanteenOrder> getOrderHistory(HttpServletRequest req) {
        String canteenId = (String) req.getAttribute("username");

        return orderRepository.findByCanteenIdAndStatus(
                canteenId,
                OrderStatus.DELIVERED
        );
    }

    @GetMapping
    public List<CanteenOrder> currentOrders(HttpServletRequest req) {
        String canteenId = (String) req.getAttribute("username");

        return orderRepository.findByCanteenIdAndStatusIn(
                canteenId,
                List.of(PLACED, PREPARING, READY)
        );
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long orderId,
            @RequestParam OrderStatus status,
            HttpServletRequest req
    ) {
        String canteenId = (String) req.getAttribute("username");

        CanteenOrder order = orderRepository.findById(orderId)
                .orElseThrow();

        if (!order.getCanteenId().equals(canteenId)) {
            return ResponseEntity.status(403).build();
        }

        order.setStatus(status);
        orderRepository.save(order);

        return ResponseEntity.ok().build();
    }

    // ================= PROFESSOR =================
    @GetMapping("/my/active")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<?> getMyActiveOrder(HttpServletRequest req) {
        String professorId = (String) req.getAttribute("username");

        return ResponseEntity.ok(
                orderRepository
                        .findTopByProfessorIdAndStatusInOrderByCreatedAtDesc(
                                professorId,
                                List.of(PLACED, PREPARING, READY)
                        )
                        .orElse(null)
        );
    }

    @PutMapping("/{orderId}/confirm-payment")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<?> confirmUpiPayment(
            @PathVariable Long orderId,
            HttpServletRequest request
    ) {
        String professorId = (String) request.getAttribute("username");

        CanteenOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // 1️⃣ Ownership check
        if (!order.getProfessorId().equals(professorId)) {
            return ResponseEntity.status(403).build();
        }

        // 2️⃣ Only UPI orders allowed
        if (!"UPI".equalsIgnoreCase(order.getPaymentMode())) {
            return ResponseEntity.badRequest()
                    .body("Payment confirmation only allowed for UPI orders");
        }

        // 3️⃣ Already paid check
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            return ResponseEntity.ok(order);
        }

        // 4️⃣ Mark as PAID
        order.setPaymentStatus(PaymentStatus.PAID);
        orderRepository.save(order);

        return ResponseEntity.ok(order);
    }


}

