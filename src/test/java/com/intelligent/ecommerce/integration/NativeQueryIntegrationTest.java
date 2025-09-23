package com.intelligent.ecommerce.integration;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.intelligent.ecommerce.entity.Order;
import com.intelligent.ecommerce.entity.OrderItem;
import com.intelligent.ecommerce.entity.Product;
import com.intelligent.ecommerce.entity.User;
import com.intelligent.ecommerce.enums.OrderStatus;
import com.intelligent.ecommerce.repository.OrderRepository;
import com.intelligent.ecommerce.repository.ProductRepository;
import com.intelligent.ecommerce.repository.UserRepository;
import com.intelligent.ecommerce.repository.projection.OrderReportRow;

@DataJpaTest
@ActiveProfiles("test")
class NativeQueryIntegrationTest {

    @Autowired private OrderRepository orderRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ProductRepository productRepository;

    // label -> persisted user
    private final Map<Long, User> usersByLabel = new LinkedHashMap<>();
    private Product sharedProduct;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();

        // users 1..10
        for (long i = 1; i <= 10; i++) {
            User u = User.builder()
                    .email("c" + i + "@example.com")
                    .password("x")
                    .isVerified(true)
                    .build();
            usersByLabel.put(i, userRepository.save(u));
        }

        // منتج واحد مشترك
        sharedProduct = productRepository.save(
                Product.builder()
                        .name("Report Item")
                        .price(new BigDecimal("1.00"))
                        .stockQuantity(1_000_000)
                        .build()
        );
    }

    @Test
    void findHighValueOrders_shouldReturnOrdersWithTotalAmountGreaterThan1000() {
        createTestOrder(1L, bd("500.00"),  OrderStatus.CREATED);    // Low
        createTestOrder(2L, bd("1200.00"), OrderStatus.CREATED);    // High
        createTestOrder(3L, bd("800.00"),  OrderStatus.CONFIRMED);  // Low
        createTestOrder(4L, bd("1500.00"), OrderStatus.DELIVERED);  // High
        createTestOrder(5L, bd("2000.00"), OrderStatus.CREATED);    // High
        createTestOrder(6L, bd("999.00"),  OrderStatus.CREATED);    // Low

        List<OrderReportRow> result = orderRepository.findHighValueOrders();

        assertThat(result).hasSize(3);
        assertThat(result).extracting(OrderReportRow::getTotalAmount)
                .containsExactlyInAnyOrder(bd("1200.00"), bd("1500.00"), bd("2000.00"));
        assertThat(result).extracting(OrderReportRow::getCustomerId)
                .containsExactlyInAnyOrder(
                        usersByLabel.get(2L).getId(),
                        usersByLabel.get(4L).getId(),
                        usersByLabel.get(5L).getId()
                );
        assertThat(result).extracting(OrderReportRow::getStatus)
                .containsExactlyInAnyOrder("CREATED", "DELIVERED", "CREATED");
    }

    @Test
    void findHighValueOrders_shouldReturnEmptyList_whenNoHighValueOrders() {
        createTestOrder(1L, bd("500.00"),  OrderStatus.CREATED);
        createTestOrder(2L, bd("800.00"),  OrderStatus.CONFIRMED);
        createTestOrder(3L, bd("999.00"),  OrderStatus.DELIVERED);
        createTestOrder(4L, bd("100.00"),  OrderStatus.CREATED);

        List<OrderReportRow> result = orderRepository.findHighValueOrders();

        assertThat(result).isEmpty();
    }

    @Test
    void findHighValueOrders_shouldReturnEmptyList_whenNoOrders() {
        List<OrderReportRow> result = orderRepository.findHighValueOrders();
        assertThat(result).isEmpty();
    }

    @Test
    void findHighValueOrders_shouldReturnOrdersWithExactThreshold() {
        // 1000.00 لا تُحتسب (strictly greater than 1000)
        createTestOrder(1L, bd("1000.00"), OrderStatus.CREATED);   // not included
        createTestOrder(2L, bd("1000.01"), OrderStatus.CREATED);   // included
        createTestOrder(3L, bd("999.99"),  OrderStatus.CREATED);   // not included

        List<OrderReportRow> result = orderRepository.findHighValueOrders();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTotalAmount()).isEqualByComparingTo(bd("1000.01"));
        assertThat(result.get(0).getCustomerId()).isEqualTo(usersByLabel.get(2L).getId());
    }

    @Test
    void findHighValueOrders_shouldReturnOrdersWithDifferentStatuses() {
        createTestOrder(1L, bd("1200.00"), OrderStatus.CREATED);
        createTestOrder(2L, bd("1500.00"), OrderStatus.CONFIRMED);
        createTestOrder(3L, bd("2000.00"), OrderStatus.DELIVERED);
        createTestOrder(4L, bd("1800.00"), OrderStatus.CANCELLED);
        createTestOrder(5L, bd("1100.00"), OrderStatus.DELIVERED);

        List<OrderReportRow> result = orderRepository.findHighValueOrders();

        assertThat(result).hasSize(5);
        assertThat(result).extracting(OrderReportRow::getTotalAmount)
                .containsExactlyInAnyOrder(bd("1200.00"), bd("1500.00"), bd("2000.00"), bd("1800.00"), bd("1100.00"));
        assertThat(result).extracting(OrderReportRow::getStatus)
                .containsExactlyInAnyOrder("CREATED", "CONFIRMED", "DELIVERED", "CANCELLED", "DELIVERED");
    }

    @Test
    void findHighValueOrders_shouldReturnOrdersWithDifferentCustomerIds() {
        createTestOrder(1L, bd("1200.00"), OrderStatus.CREATED);
        createTestOrder(2L, bd("1500.00"), OrderStatus.CREATED);
        createTestOrder(3L, bd("2000.00"), OrderStatus.CREATED);
        createTestOrder(4L, bd("1800.00"), OrderStatus.CREATED);
        createTestOrder(5L, bd("1100.00"), OrderStatus.CREATED);

        List<OrderReportRow> result = orderRepository.findHighValueOrders();

        assertThat(result).hasSize(5);
        assertThat(result).extracting(OrderReportRow::getCustomerId)
                .containsExactlyInAnyOrder(
                        usersByLabel.get(1L).getId(),
                        usersByLabel.get(2L).getId(),
                        usersByLabel.get(3L).getId(),
                        usersByLabel.get(4L).getId(),
                        usersByLabel.get(5L).getId()
                );
    }

    @Test
    void findHighValueOrders_shouldHandleLargeAmounts() {
        createTestOrder(1L, bd("10000.00"),  OrderStatus.CREATED);
        createTestOrder(2L, bd("50000.00"),  OrderStatus.CREATED);
        createTestOrder(3L, bd("100000.00"), OrderStatus.CREATED);
        createTestOrder(4L, bd("500.00"),    OrderStatus.CREATED); // Low

        List<OrderReportRow> result = orderRepository.findHighValueOrders();

        assertThat(result).hasSize(3);
        assertThat(result).extracting(OrderReportRow::getTotalAmount)
                .containsExactlyInAnyOrder(bd("10000.00"), bd("50000.00"), bd("100000.00"));
    }

    @Test
    void findHighValueOrders_shouldHandleDecimalAmounts() {
        createTestOrder(1L, bd("1000.50"), OrderStatus.CREATED);  // included
        createTestOrder(2L, bd("1500.75"), OrderStatus.CREATED);  // included
        createTestOrder(3L, bd("2000.25"), OrderStatus.CREATED);  // included
        createTestOrder(4L, bd("999.99"),  OrderStatus.CREATED);  // not included

        List<OrderReportRow> result = orderRepository.findHighValueOrders();

        assertThat(result).hasSize(3);
        assertThat(result).extracting(OrderReportRow::getTotalAmount)
                .containsExactlyInAnyOrder(bd("1000.50"), bd("1500.75"), bd("2000.25"));
    }

    // ===== helpers =====

    private static BigDecimal bd(String v) { return new BigDecimal(v); }

    private void createTestOrder(Long customerLabel, BigDecimal totalAmount, OrderStatus status) {
        createTestOrderWithTime(customerLabel, totalAmount, status, LocalDateTime.now());
    }

    private Order createTestOrderWithTime(Long customerLabel, BigDecimal totalAmount, OrderStatus status, LocalDateTime createdAt) {
        User customer = usersByLabel.get(customerLabel);
        if (customer == null) throw new IllegalArgumentException("No user for label: " + customerLabel);

        Order order = Order.builder()
                .customer(customer)
                .totalAmount(totalAmount)
                .status(status)
                .createdAt(createdAt.atZone(java.time.ZoneId.systemDefault()).toInstant())
                .build();

        OrderItem item = OrderItem.builder()
                .order(order)
                .product(sharedProduct)
                .price(totalAmount)
                .quantity(1)
                .build();

        order.getItems().add(item);

        return orderRepository.save(order);
    }
}
