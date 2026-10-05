package com.example.coffee_order_system.user;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class PointServiceIntegrationTest {

    @Autowired
    private PointService pointService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PointHistoryRepository pointHistoryRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @Transactional
    void 포인트를_충전하면_잔액과_CHARGE_이력이_함께_반영된다() {
        User user = userRepository.save(new User(1_000));

        int chargedPoint = pointService.charge(
                user.getId(),
                5_000
        );

        entityManager.flush();
        entityManager.clear();

        User updatedUser = userRepository.findById(user.getId())
                .orElseThrow();

        assertThat(chargedPoint).isEqualTo(6_000);
        assertThat(updatedUser.getPoint()).isEqualTo(6_000);

        List<PointHistory> histories = pointHistoryRepository.findAll();

        assertThat(histories)
                .anySatisfy(history -> {
                    assertThat(history.getUser().getId()).isEqualTo(user.getId());
                    assertThat(history.getType()).isEqualTo(PointHistoryType.CHARGE);
                    assertThat(history.getAmount()).isEqualTo(5_000);
                    assertThat(history.getBalanceAfter()).isEqualTo(6_000);
                });
    }

    @Test
    @Transactional
    void 충전_포인트가_0이하면_포인트와_이력이_변경되지_않는다() {
        User user = userRepository.save(new User(1_000));

        long pointHistoryCountBefore = pointHistoryRepository.count();

        assertThatThrownBy(() ->
                pointService.charge(user.getId(), 0)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("충전 포인트는 0보다 커야 합니다.");

        entityManager.flush();
        entityManager.clear();

        User unchangedUser = userRepository.findById(user.getId())
                .orElseThrow();

        assertThat(unchangedUser.getPoint()).isEqualTo(1_000);
        assertThat(pointHistoryRepository.count())
                .isEqualTo(pointHistoryCountBefore);
    }

    @Test
    @Transactional
    void 포인트_이력은_사용자별로_최신순_페이징_조회된다() {
        User user = userRepository.save(new User(1_000));
        User otherUser = userRepository.save(new User(1_000));

        PointHistory oldHistory = pointHistoryRepository.save(
                new PointHistory(
                        user,
                        PointHistoryType.CHARGE,
                        1_000,
                        2_000
                )
        );

        PointHistory recentHistory = pointHistoryRepository.save(
                new PointHistory(
                        user,
                        PointHistoryType.USE,
                        500,
                        1_500
                )
        );

        pointHistoryRepository.save(
                new PointHistory(
                        otherUser,
                        PointHistoryType.CHARGE,
                        5_000,
                        6_000
                )
        );

        entityManager.flush();

        entityManager.createNativeQuery("""
            UPDATE point_history
            SET created_at = ?1
            WHERE id = ?2
            """)
                .setParameter(
                        1,
                        LocalDateTime.now().minusDays(1)
                )
                .setParameter(
                        2,
                        oldHistory.getId()
                )
                .executeUpdate();

        entityManager.flush();
        entityManager.clear();

        Page<PointHistoryResponse> result =
                pointService.getHistories(
                        user.getId(),
                        0,
                        1
                );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(2);

        PointHistoryResponse response = result.getContent().get(0);

        assertThat(response.id()).isEqualTo(recentHistory.getId());
        assertThat(response.type()).isEqualTo(PointHistoryType.USE);
        assertThat(response.amount()).isEqualTo(500);
        assertThat(response.balanceAfter()).isEqualTo(1_500);
    }
}