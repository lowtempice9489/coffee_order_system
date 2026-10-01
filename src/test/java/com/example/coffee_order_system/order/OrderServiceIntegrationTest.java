package com.example.coffee_order_system.order;

import com.example.coffee_order_system.menu.Menu;
import com.example.coffee_order_system.menu.MenuRepository;
import com.example.coffee_order_system.user.User;
import com.example.coffee_order_system.user.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderOutboxRepository orderOutboxRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @Transactional
    void 주문에_성공하면_포인트_주문_Outbox가_함께_반영된다() {
        User user = userRepository.save(new User(10_000));
        Menu menu = menuRepository.save(new Menu("아메리카노", 4_500));

        OrderRequest request = new OrderRequest(
                menu.getId(),
                1
        );

        OrderResponse response = orderService.order(
                user.getId(),
                request
        );

        entityManager.flush();
        entityManager.clear();

        User updatedUser = userRepository.findById(user.getId())
                .orElseThrow();

        assertThat(updatedUser.getPoint()).isEqualTo(5_500);
        assertThat(orderRepository.findById(response.orderId())).isPresent();

        assertThat(
                orderOutboxRepository.findByStatus(OutboxStatus.PENDING)
        ).anyMatch(outbox ->
                outbox.getOrderId().equals(response.orderId())
        );
    }

    @Test
    @Transactional
    void 포인트가_부족하면_주문이_생성되지_않고_포인트도_유지된다() {
        User user = userRepository.save(new User(1_000));
        Menu menu = menuRepository.save(new Menu("아메리카노", 4_500));

        OrderRequest request = new OrderRequest(
                menu.getId(),
                1
        );

        long orderCountBefore = orderRepository.count();
        long outboxCountBefore = orderOutboxRepository.count();

        assertThatThrownBy(() ->
                orderService.order(user.getId(), request)
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("포인트가 부족합니다.");


        User unchangedUser = userRepository.findById(user.getId())
                .orElseThrow();

        assertThat(unchangedUser.getPoint()).isEqualTo(1_000);
        assertThat(orderRepository.count()).isEqualTo(orderCountBefore);
        assertThat(orderOutboxRepository.count()).isEqualTo(outboxCountBefore);
    }

    @Test
    void 동시에_3번_주문하면_포인트_범위_안에서_2번만_성공한다() throws Exception {
        User user = userRepository.save(new User(10_000));
        Menu menu = menuRepository.save(new Menu("아메리카노", 4_500));


        OrderRequest request = new OrderRequest(
                menu.getId(),
                1
        );

        int requestCount = 3;

        ExecutorService executorService = Executors.newFixedThreadPool(requestCount);
        CountDownLatch readyLatch = new CountDownLatch(requestCount);
        CountDownLatch startLatch = new CountDownLatch(1);

        List<Future<Boolean>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < requestCount; i++) {
                futures.add(executorService.submit(() -> {
                    readyLatch.countDown();
                    startLatch.await();

                    try {
                        orderService.order(user.getId(), request);
                        return true;
                    } catch (IllegalStateException e) {
                        if ("포인트가 부족합니다.".equals(e.getMessage())) {
                            return false;
                        }
                        throw e;
                    }
                }));
            }

            readyLatch.await();
            startLatch.countDown();

            int successCount = 0;

            for (Future<Boolean> future : futures) {
                if (future.get()) {
                    successCount++;
                }
            }

            entityManager.clear();

            User updatedUser = userRepository.findById(user.getId())
                    .orElseThrow();

            assertThat(successCount).isEqualTo(2);
            assertThat(updatedUser.getPoint()).isEqualTo(1_000);

        } finally {
            executorService.shutdown();
        }
    }

    @Test
    @Transactional
    void 인기메뉴는_최근_7일_밖의_주문을_집계하지_않는다() {
        orderOutboxRepository.deleteAll();
        orderRepository.deleteAll();
        menuRepository.deleteAll();
        userRepository.deleteAll();

        entityManager.flush();
        entityManager.clear();

        User user = userRepository.save(new User(100_000));

        Menu recentMenu = menuRepository.save(
                new Menu("최근메뉴", 4_500)
        );

        Menu oldMenu = menuRepository.save(
                new Menu("오래된메뉴", 5_000)
        );

        Order recentOrder = orderRepository.save(
                new Order(user, recentMenu, 1, 4_500)
        );

        Order oldOrder = orderRepository.save(
                new Order(user, oldMenu, 1, 5_000)
        );

        entityManager.flush();

        entityManager.createNativeQuery("""
        UPDATE orders
        SET ordered_at = ?1
        WHERE id = ?2
        """)
                .setParameter(
                        1,
                        Timestamp.valueOf(LocalDateTime.now().minusDays(8))
                )
                .setParameter(
                        2,
                        oldOrder.getId()
                )
                .executeUpdate();

        entityManager.flush();
        entityManager.clear();

        LocalDateTime updatedOrderedAt = (LocalDateTime) entityManager
                .createNativeQuery("""
                SELECT ordered_at
                FROM orders
                WHERE id = ?1
                """)
                .setParameter(1, oldOrder.getId())
                .getSingleResult();

        System.out.println("oldOrder id = " + oldOrder.getId());
        System.out.println("oldOrder orderedAt = " + updatedOrderedAt);
        System.out.println("expected startDateTime = "
                + LocalDate.now().minusDays(6).atStartOfDay());

        List<PopularMenuResponse> result =
                orderService.getPopularMenus();

        assertThat(result)
                .extracting(PopularMenuResponse::menuId)
                .contains(recentMenu.getId())
                .doesNotContain(oldMenu.getId());
    }

    @Test
    @Transactional
    void 인기메뉴_주문횟수가_같으면_최근_주문이_있는_메뉴가_먼저_조회된다() {
        orderOutboxRepository.deleteAll();
        orderRepository.deleteAll();
        menuRepository.deleteAll();
        userRepository.deleteAll();

        entityManager.flush();
        entityManager.clear();

        User user = userRepository.save(new User(100_000));

        Menu oldMenu = menuRepository.save(
                new Menu("먼저주문된메뉴", 4_500)
        );

        Menu recentMenu = menuRepository.save(
                new Menu("나중주문된메뉴", 5_000)
        );

        Order oldOrder = orderRepository.save(
                new Order(user, oldMenu, 1, 4_500)
        );

        Order recentOrder = orderRepository.save(
                new Order(user, recentMenu, 1, 5_000)
        );

        entityManager.flush();

        entityManager.createNativeQuery("""
            UPDATE orders
            SET ordered_at = ?1
            WHERE id = ?2
            """)
                .setParameter(
                        1,
                        Timestamp.valueOf(LocalDateTime.now().minusDays(2))
                )
                .setParameter(
                        2,
                        oldOrder.getId()
                )
                .executeUpdate();

        entityManager.createNativeQuery("""
            UPDATE orders
            SET ordered_at = ?1
            WHERE id = ?2
            """)
                .setParameter(
                        1,
                        Timestamp.valueOf(LocalDateTime.now().minusDays(1))
                )
                .setParameter(
                        2,
                        recentOrder.getId()
                )
                .executeUpdate();

        entityManager.flush();
        entityManager.clear();

        List<PopularMenuResponse> result =
                orderService.getPopularMenus();

        assertThat(result)
                .extracting(PopularMenuResponse::menuId)
                .containsExactly(
                        recentMenu.getId(),
                        oldMenu.getId()
                );
    }

    @Test
    @Transactional
    void 인기메뉴_주문횟수와_최근주문시각이_같으면_menuId가_작은_메뉴가_먼저_조회된다() {
        orderOutboxRepository.deleteAll();
        orderRepository.deleteAll();
        menuRepository.deleteAll();
        userRepository.deleteAll();

        entityManager.flush();
        entityManager.clear();

        User user = userRepository.save(new User(100_000));

        Menu firstMenu = menuRepository.save(
                new Menu("첫번째메뉴", 4_500)
        );

        Menu secondMenu = menuRepository.save(
                new Menu("두번째메뉴", 5_000)
        );

        Order firstOrder = orderRepository.save(
                new Order(user, firstMenu, 1, 4_500)
        );

        Order secondOrder = orderRepository.save(
                new Order(user, secondMenu, 1, 5_000)
        );

        entityManager.flush();

        LocalDateTime sameOrderedAt = LocalDateTime.now().minusDays(1);

        entityManager.createNativeQuery("""
            UPDATE orders
            SET ordered_at = ?1
            WHERE id IN (?2, ?3)
            """)
                .setParameter(1, Timestamp.valueOf(sameOrderedAt))
                .setParameter(2, firstOrder.getId())
                .setParameter(3, secondOrder.getId())
                .executeUpdate();

        entityManager.flush();
        entityManager.clear();

        List<PopularMenuResponse> result =
                orderService.getPopularMenus();

        assertThat(result)
                .extracting(PopularMenuResponse::menuId)
                .containsExactly(
                        firstMenu.getId(),
                        secondMenu.getId()
                );
    }
}
