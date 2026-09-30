package com.example.coffee_order_system.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;

    @PostMapping("/{userId}/points")
    public ResponseEntity<Integer> charge(
            @PathVariable Long userId,
            @Valid @RequestBody PointChargeRequest request
    ) {
        int currentPoint = pointService.charge(userId, request.amount());

        return ResponseEntity.ok(currentPoint);
    }
}
