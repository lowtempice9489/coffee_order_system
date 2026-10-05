package com.example.coffee_order_system.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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

    @GetMapping("/{userId}/points/history")
    public ResponseEntity<Page<PointHistoryResponse>> getHistories(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<PointHistoryResponse> histories =
                pointService.getHistories(userId, page, size);

        return ResponseEntity.ok(histories);
    }
}
