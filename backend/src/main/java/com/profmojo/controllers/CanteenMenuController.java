package com.profmojo.controllers;

import com.profmojo.models.CanteenFolder.MenuItem;
import com.profmojo.repositories.MenuItemRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/canteen/menu")
@RequiredArgsConstructor
public class CanteenMenuController {

    private final MenuItemRepository menuItemRepository;

    // 🔹 CANTEEN – VIEW OWN MENU
    @GetMapping("/my")
    public ResponseEntity<?> getMyMenu(HttpServletRequest request) {

        String canteenId = (String) request.getAttribute("username");

        return ResponseEntity.ok(
                menuItemRepository.findByCanteenId(canteenId)
        );
    }

    // 🔹 CANTEEN – ADD ITEM
    @PostMapping("/add")
    public ResponseEntity<?> addItem(
            @RequestBody MenuItem item,
            HttpServletRequest request
    ) {
        String canteenId = (String) request.getAttribute("username");

        item.setId(null);                 // ensure new row
        item.setCanteenId(canteenId);     // enforce ownership
        if (item.getAvailable() == null) {
            item.setAvailable(true); // default only if frontend didn’t send it
        }

        MenuItem saved = menuItemRepository.save(item);
        return ResponseEntity.ok(saved);
    }


    // 🔹 PROFESSOR – VIEW MENU OF A CANTEEN
    @GetMapping("/{canteenId}")
    public ResponseEntity<?> getMenuForProfessor(
            @PathVariable String canteenId
    ) {
        return ResponseEntity.ok(
                menuItemRepository
                        .findByCanteenIdAndAvailableTrue(canteenId)
        );
    }

    @PutMapping("/availability")
    public ResponseEntity<?> updateAvailability(
            @RequestBody Map<String, Object> payload,
            HttpServletRequest request
    ) {
        String canteenId = (String) request.getAttribute("username");

        Long itemId = Long.valueOf(payload.get("itemId").toString());
        Boolean available = (Boolean) payload.get("available");

        MenuItem item = menuItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        // ownership check
        if (!item.getCanteenId().equals(canteenId)) {
            return ResponseEntity.status(403).build();
        }

        item.setAvailable(available);
        menuItemRepository.save(item);

        return ResponseEntity.ok(item);
    }

}

