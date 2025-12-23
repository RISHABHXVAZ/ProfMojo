package com.profmojo.services.impl;

import com.profmojo.models.CanteenFolder.Canteen;
import com.profmojo.repositories.CanteenMasterRepository;
import com.profmojo.repositories.CanteenRepository;
import com.profmojo.services.CanteenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CanteenServiceImpl implements CanteenService {

    private final CanteenRepository canteenRepository;
    private final CanteenMasterRepository canteenMasterRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Canteen registerCanteen(Canteen canteen) {

        // 1️⃣ Validate canteen exists in master
        if (!canteenMasterRepository.existsById(canteen.getCanteenId())) {
            throw new RuntimeException("Canteen ID not found in master list");
        }

        // 2️⃣ Prevent duplicate registration
        if (canteenRepository.existsById(canteen.getCanteenId())) {
            throw new RuntimeException("Canteen already registered");
        }

        // 3️⃣ Encode password
        canteen.setPassword(passwordEncoder.encode(canteen.getPassword()));

        // 🔥 4️⃣ FORCE DEFAULTS (THIS IS THE FIX)
        canteen.setActive(true);
        canteen.setRole("CANTEEN");

        return canteenRepository.save(canteen);
    }

    @Override
    public boolean existsByCanteenId(String canteenId) {
        return canteenRepository.existsById(canteenId);
    }
}
