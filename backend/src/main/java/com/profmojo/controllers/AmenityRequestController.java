package com.profmojo.controllers;

import com.profmojo.models.AmenityRequest;
import com.profmojo.models.Professor;
import com.profmojo.models.dto.AmenityRequestDTO;
import com.profmojo.services.AmenityRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/amenities")
@RequiredArgsConstructor
@CrossOrigin("http://localhost:5173")
public class AmenityRequestController {

    private final AmenityRequestService amenityRequestService;

    // PROFESSOR → Raise request
    @PostMapping("/request")
    public ResponseEntity<?> raiseRequest(
            @RequestBody AmenityRequestDTO dto,
            @AuthenticationPrincipal Professor professor
    ) {
        return ResponseEntity.ok(
                amenityRequestService.raiseRequest(dto, professor)
        );
    }

    // PROFESSOR → View my requests
    @GetMapping("/my")
    public List<AmenityRequest> myRequests(
            @AuthenticationPrincipal Professor professor
    ) {
        return amenityRequestService.getMyRequests(
                professor.getProfId()
        );
    }
}
