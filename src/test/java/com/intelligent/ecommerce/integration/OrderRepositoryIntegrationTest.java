package com.intelligent.ecommerce.integration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import com.intelligent.ecommerce.entity.Order;
import com.intelligent.ecommerce.entity.OrderItem;
import com.intelligent.ecommerce.entity.Payment;
import com.intelligent.ecommerce.entity.Product;
import com.intelligent.ecommerce.entity.User;
import com.intelligent.ecommerce.enums.OrderStatus;
import com.intelligent.ecommerce.enums.PaymentMethod;
import com.intelligent.ecommerce.repository.OrderRepository;
import com.intelligent.ecommerce.repository.PaymentRepository;
import com.intelligent.ecommerce.repository.ProductRepository;
import com.intelligent.ecommerce.repository.UserRepository;
import com.intelligent.ecommerce.repository.projection.OrderReportRow;

@DataJpaTest
@ActiveProfiles("test")
class OrderRepositoryIntegrationTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserRepository userRepository;

    private Product product1;
    private Product product2;
    private Product product3;
    private User customer1;
    private User customer2;
    private User customer3;

    @BeforeEach
    void setUp() {
        // Clean up existing data
        paymentRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();

        // Create test products
        product1 = Product.builder()
                .name("High-Value Product")
                .price(java.math.BigDecimal.valueOf(1500.0))
                .stockQuantity(10)
                .build();

        product2 = Product.builder()
                .name("Low-Value Product")
                .price(java.math.BigDecimal.valueOf(25.0))
                .stockQuantity(50)
                .build();

        product3 = Product.builder()
                .name("Medium-Value Product")
                .price(java.math.BigDecimal.valueOf(75.0))
                .stockQuantity(30)
                .build();

        productRepository.saveAll(List.of(product1, product2, product3));

        // Create test customers
        customer1 = User.builder()
                .email("customer1@test.com")
                .username("customer1")
                .build();
        customer2 = User.builder()
                .email("customer2@test.com")
                .username("customer2")
                .build();
        customer3 = User.builder()
                .email("customer3@test.com")
                .username("customer3")
                .build();

        userRepository.saveAll(List.of(customer1, customer2, customer3));
    }

    @Test
    void findHighValueOrders_shouldReturnOrdersWithTotalAmountGreaterThan1000() {
        // Arrange - Create orders with different amounts
        createTestOrder(customer1.getId(), java.math.BigDecimal.valueOf(1200.0), OrderStatus.CREATED); // High value
        createTestOrder(customer2.getId(), java.math.BigDecimal.valueOf(800.0), OrderStatus.CREATED);  // Low value
        createTestOrder(customer3.getId(), java.math.BigDecimal.valueOf(1500.0), OrderStatus.CONFIRMED); // High value
        createTestOrder(customer1.getId(), java.math.BigDecimal.valueOf(500.0), OrderStatus.CREATED);  // Low value
        createTestOrder(customer2.getId(), java.math.BigDecimal.valueOf(2000.0), OrderStatus.DELIVERED); // High value

        // Act
        List<OrderReportRow> result = orderRepository.findHighValueOrders();

        // Assert
        assertThat(result).hasSize(3);
        assertThat(result).extracting(OrderReportRow::getTotalAmount)
                .usingComparatorForType(java.math.BigDecimal::compareTo, java.math.BigDecimal.class)
                .containsExactlyInAnyOrder(
                    java.math.BigDecimal.valueOf(1200.0), 
                    java.math.BigDecimal.valueOf(1500.0), 
                    java.math.BigDecimal.valueOf(2000.0)
                );
        assertThat(result).extracting(OrderReportRow::getCustomerId)
                .containsExactlyInAnyOrder(customer1.getId(), customer3.getId(), customer2.getId());
        assertThat(result).extracting(OrderReportRow::getStatus)
                .containsExactlyInAnyOrder("CREATED", "CONFIRMED", "DELIVERED");
    }

    @Test
    void findHighValueOrders_shouldReturnEmptyList_whenNoHighValueOrders() {
        // Arrange - Create only low value orders
        createTestOrder(customer1.getId(), java.math.BigDecimal.valueOf(500.0), OrderStatus.CREATED);
        createTestOrder(customer2.getId(), java.math.BigDecimal.valueOf(800.0), OrderStatus.CREATED);
        createTestOrder(customer3.getId(), java.math.BigDecimal.valueOf(900.0), OrderStatus.CONFIRMED);

        // Act
        List<OrderReportRow> result = orderRepository.findHighValueOrders();

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void findAll_shouldRespectSorting() {
        // Arrange - Create orders with different creation times
        createTestOrder(customer1.getId(), java.math.BigDecimal.valueOf(1000.0), OrderStatus.CREATED);
        // Add small delay to ensure different timestamps
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        createTestOrder(customer2.getId(), java.math.BigDecimal.valueOf(2000.0), OrderStatus.CREATED);
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        createTestOrder(customer3.getId(), java.math.BigDecimal.valueOf(3000.0), OrderStatus.CREATED);

        // Act - Sort by createdAt descending
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        Page<Order> result = orderRepository.findAll(pageRequest);

        // Assert
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent().get(0).getCustomer().getId()).isEqualTo(customer3.getId());
        assertThat(result.getContent().get(1).getCustomer().getId()).isEqualTo(customer2.getId());
        assertThat(result.getContent().get(2).getCustomer().getId()).isEqualTo(customer1.getId());
    }

    @Test
    void findAll_shouldHandleEmptyResult() {
        // Act
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<Order> result = orderRepository.findAll(pageRequest);

        // Assert
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getTotalPages()).isZero();
    }

    @Test
    void findHighValueOrders_shouldHandleEmptyDatabase() {
        // Act
        List<OrderReportRow> result = orderRepository.findHighValueOrders();

        // Assert
        assertThat(result).isEmpty();
    }

    private Order createTestOrder(Long customerId, java.math.BigDecimal totalAmount, OrderStatus status) {
        // Get customer
        User customer = userRepository.findById(customerId).orElseThrow();
        
        // Create order
        Order order = Order.builder()
                .customer(customer)
                .totalAmount(totalAmount)
                .status(status)
                .createdAt(java.time.Instant.now())
                .build();

        // Create order items
        OrderItem orderItem = OrderItem.builder()
                .product(product1)
                .quantity(1)
                .price(totalAmount)
                .order(order)
                .build();

        order.setItems(List.of(orderItem));

        // Save order
        Order savedOrder = orderRepository.save(order);

        // Create payment
        Payment payment = Payment.builder()
                .order(savedOrder)
                .amount(totalAmount)
                .status("PENDING")
                .paymentMethod(PaymentMethod.CARD)
                .build();

        paymentRepository.save(payment);

        return savedOrder;
    }
}
