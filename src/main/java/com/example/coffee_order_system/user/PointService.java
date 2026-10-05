package com.example.coffee_order_system.user;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PointService {

    private final UserRepository userRepository;
    private final PointHistoryRepository pointHistoryRepository;

    @Transactional
    public int charge(Long userId, int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("충전 포인트는 0보다 커야 합니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        user.addPoint(amount);

        PointHistory history = new PointHistory(
                user,
                PointHistoryType.CHARGE,
                amount,
                user.getPoint()
        );
        pointHistoryRepository.save(history);

        return user.getPoint();
    }

    @Transactional(readOnly = true)
    public Page<PointHistoryResponse> getHistories(
            Long userId,
            int page,
            int size
    ) {
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }

        return pointHistoryRepository
                .findByUserIdOrderByCreatedAtDescIdDesc(
                        userId,
                        PageRequest.of(page, size)
                )
                .map(PointHistoryResponse::from);
    }
}
