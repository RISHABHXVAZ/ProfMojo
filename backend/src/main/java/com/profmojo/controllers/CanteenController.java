package com.profmojo.controllers;

import com.profmojo.models.CanteenFolder.Canteen;
import com.profmojo.models.CanteenFolder.CanteenMaster;
import com.profmojo.models.CanteenFolder.CanteenOrder;
import com.profmojo.models.dto.CanteenLoginRequest;
import com.profmojo.models.dto.LoginRequest;
import com.profmojo.models.enums.OrderStatus;
import com.profmojo.repositories.CanteenMasterRepository;
import com.profmojo.repositories.CanteenOrderRepository;
import com.profmojo.repositories.CanteenRepository;
import com.profmojo.security.jwt.JwtUtil;
import com.profmojo.services.CanteenService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/canteen")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class CanteenController {

    private final CanteenRepository canteenRepository;
    private final CanteenMasterRepository canteenMasterRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final CanteenService canteenService;
    private final CanteenOrderRepository orderRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody CanteenLoginRequest request) {

        Canteen canteen = canteenRepository.findById(request.getCanteenId())
                .orElseThrow(() -> new RuntimeException("Invalid Canteen ID"));

        if (!passwordEncoder.matches(request.getPassword(), canteen.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        String token = jwtUtil.generateToken(
                canteen.getCanteenId(),
                canteen.getRole()
        );

        return ResponseEntity.ok(Map.of("token", token));
    }


    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Canteen canteen) {
        Canteen saved = canteenService.registerCanteen(canteen);
        return ResponseEntity.ok(saved);
    }

    // 🔹 CHECK CANTEEN ID (for UI validation)
    @GetMapping("/check-id/{canteenId}")
    public ResponseEntity<?> checkCanteenId(@PathVariable String canteenId) {

        // Exists in master?
        boolean existsInMaster = canteenMasterRepository.existsById(canteenId);

        if (!existsInMaster) {
            return ResponseEntity.ok(Map.of("canRegister", false));
        }

        // Already registered?
        boolean alreadyRegistered = canteenRepository.existsById(canteenId);

        if (alreadyRegistered) {
            return ResponseEntity.ok(Map.of("canRegister", false));
        }

        return ResponseEntity.ok(Map.of("canRegister", true));
    }

    @GetMapping("/{canteenId}/upi")
    public ResponseEntity<?> getUpi(@PathVariable String canteenId) {

        CanteenMaster master =
                canteenMasterRepository.findById(canteenId).orElseThrow();

        return ResponseEntity.ok(
                Map.of(
                        "upiId", master.getUpiId(),
                        "canteenName", master.getCanteenName()
                )
        );
    }


    @GetMapping("/active")
    public ResponseEntity<?> getActiveCanteens() {

        return ResponseEntity.ok(
                canteenRepository.findAll()
                        .stream()
                        .filter(canteen ->
                                canteenMasterRepository.findById(canteen.getCanteenId())
                                        .map(CanteenMaster::isActive)
                                        .orElse(false)
                        )
                        .map(canteen -> {
                            CanteenMaster master =
                                    canteenMasterRepository.findById(canteen.getCanteenId()).get();

                            return Map.of(
                                    "canteenId", canteen.getCanteenId(),
                                    "canteenName", master.getCanteenName(),
                                    "location", master.getLocation()
                            );
                        })
                        .toList()
        );
    }

    @GetMapping("/orders")
    @PreAuthorize("hasRole('CANTEEN')")
    public ResponseEntity<?> getOrders(HttpServletRequest request) {

        String canteenId = (String) request.getAttribute("username");

        return ResponseEntity.ok(
                orderRepository.findByCanteenIdOrderByCreatedAtDesc(canteenId)
        );
    }



    @GetMapping("/me")
    @PreAuthorize("hasRole('CANTEEN')")
    public ResponseEntity<?> getCanteenDetails(HttpServletRequest req) {
        String canteenId = (String) req.getAttribute("username");

        CanteenMaster master = canteenMasterRepository.findById(canteenId).orElseThrow();

        return ResponseEntity.ok(
                Map.of(
                        "canteenId", canteenId,
                        "canteenName", master.getCanteenName()
                )
        );
    }



}
