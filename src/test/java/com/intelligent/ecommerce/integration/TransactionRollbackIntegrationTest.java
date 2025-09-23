package com.intelligent.ecommerce.integration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import com.intelligent.ecommerce.dto.order.request.CreateOrderItemRequest;
import com.intelligent.ecommerce.entity.Order;
import com.intelligent.ecommerce.entity.Payment;
import com.intelligent.ecommerce.entity.Product;
import com.intelligent.ecommerce.entity.User;
import com.intelligent.ecommerce.enums.OrderStatus;
import com.intelligent.ecommerce.enums.PaymentMethod;
import com.intelligent.ecommerce.event.OrderCreatedEvent;
import com.intelligent.ecommerce.exception.InsufficientStockException;
import com.intelligent.ecommerce.repository.OrderRepository;
import com.intelligent.ecommerce.repository.PaymentRepository;
import com.intelligent.ecommerce.repository.ProductRepository;
import com.intelligent.ecommerce.repository.UserRepository;
import com.intelligent.ecommerce.service.OrderService;

@SpringBootTest
@ActiveProfiles("test")
@RecordApplicationEvents
class TransactionRollbackIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApplicationEvents applicationEvents;

    private Product product1;
    private Product product2;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Clean up existing data
        paymentRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = User.builder()
                .email("test@example.com")
                .username("testuser")
                .build();
        testUser = userRepository.save(testUser);

        // Create test products
        product1 = Product.builder()
                .name("Product 1")
                .price(java.math.BigDecimal.valueOf(100.0))
                .stockQuantity(5)
                .build();

        product2 = Product.builder()
                .name("Product 2")
                .price(java.math.BigDecimal.valueOf(200.0))
                .stockQuantity(3)
                .build();

        productRepository.saveAll(List.of(product1, product2));
    }

    @Test
    void createOrder_shouldRollbackTransaction_whenInsufficientStockException() {
        // Arrange
        Long customerId = testUser.getId();
        List<CreateOrderItemRequest> items = List.of(
                new CreateOrderItemRequest(product1.getId(), 3), // Valid quantity
                new CreateOrderItemRequest(product2.getId(), 5)  // Invalid quantity (stock: 3)
        );

        // Act & Assert
        assertThatThrownBy(() -> orderService.createOrder(customerId, items, PaymentMethod.CARD))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Insufficient stock for product");

        // Verify complete rollback
        assertThat(orderRepository.count()).isZero();
        assertThat(paymentRepository.count()).isZero();

        // Verify stock was not updated
        Product unchangedProduct1 = productRepository.findById(product1.getId()).orElseThrow();
        Product unchangedProduct2 = productRepository.findById(product2.getId()).orElseThrow();

        assertThat(unchangedProduct1.getStockQuantity()).isEqualTo(5);
        assertThat(unchangedProduct2.getStockQuantity()).isEqualTo(3);

        // Verify no event was published
        assertThat(applicationEvents.stream(OrderCreatedEvent.class)).isEmpty();
    }

    @Test
    void createOrder_shouldRollbackTransaction_whenProductNotFound() {
        // Arrange
        Long customerId = testUser.getId();
        List<CreateOrderItemRequest> items = List.of(
                new CreateOrderItemRequest(product1.getId(), 2), // Valid product
                new CreateOrderItemRequest(999L, 1)             // Invalid product
        );

        // Act & Assert
        assertThatThrownBy(() -> orderService.createOrder(customerId, items, PaymentMethod.CARD))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product not found: 999");

        // Verify complete rollback
        assertThat(orderRepository.count()).isZero();
        assertThat(paymentRepository.count()).isZero();

        // Verify stock was not updated
        Product unchangedProduct1 = productRepository.findById(product1.getId()).orElseThrow();
        assertThat(unchangedProduct1.getStockQuantity()).isEqualTo(5);

        // Verify no event was published
        assertThat(applicationEvents.stream(OrderCreatedEvent.class)).isEmpty();
    }

    @Test
    void createOrder_shouldRollbackTransaction_whenMultipleProductsHaveInsufficientStock() {
        // Arrange
        Long customerId = testUser.getId();
        List<CreateOrderItemRequest> items = List.of(
                new CreateOrderItemRequest(product1.getId(), 10), // Insufficient stock (stock: 5)
                new CreateOrderItemRequest(product2.getId(), 5)   // Insufficient stock (stock: 3)
        );

        // Act & Assert
        assertThatThrownBy(() -> orderService.createOrder(customerId, items, PaymentMethod.CARD))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Insufficient stock for product");

        // Verify complete rollback
        assertThat(orderRepository.count()).isZero();
        assertThat(paymentRepository.count()).isZero();

        // Verify stock was not updated
        Product unchangedProduct1 = productRepository.findById(product1.getId()).orElseThrow();
        Product unchangedProduct2 = productRepository.findById(product2.getId()).orElseThrow();

        assertThat(unchangedProduct1.getStockQuantity()).isEqualTo(5);
        assertThat(unchangedProduct2.getStockQuantity()).isEqualTo(3);

        // Verify no event was published
        assertThat(applicationEvents.stream(OrderCreatedEvent.class)).isEmpty();
    }

    @Test
    @Rollback(false)
    void createOrder_shouldSucceed_whenAllProductsHaveSufficientStock() {
        // Arrange
        Long customerId = testUser.getId();
        List<CreateOrderItemRequest> items = List.of(
                new CreateOrderItemRequest(product1.getId(), 2), // Valid quantity
                new CreateOrderItemRequest(product2.getId(), 1)  // Valid quantity
        );

        // Act
        Order result = orderService.createOrder(customerId, items, PaymentMethod.CARD);

        // Assert - Transaction should succeed
        assertThat(result).isNotNull();
        assertThat(result.getCustomer().getId()).isEqualTo(customerId);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(result.getTotalAmount()).isEqualByComparingTo(java.math.BigDecimal.valueOf(400.0)); // 2 * 100 + 1 * 200

        // Verify order was created
        assertThat(orderRepository.count()).isEqualTo(1);

        // Verify payment was created
        Payment payment = paymentRepository.findByOrderId(result.getId()).orElseThrow();
        assertThat(payment).isNotNull();
        assertThat(payment.getAmount()).isEqualByComparingTo(java.math.BigDecimal.valueOf(400.0));

        // Verify stock was updated
        Product updatedProduct1 = productRepository.findById(product1.getId()).orElseThrow();
        Product updatedProduct2 = productRepository.findById(product2.getId()).orElseThrow();

        assertThat(updatedProduct1.getStockQuantity()).isEqualTo(3); // 5 - 2
        assertThat(updatedProduct2.getStockQuantity()).isEqualTo(2); // 3 - 1

        // Verify event was published
        assertThat(applicationEvents.stream(OrderCreatedEvent.class)).hasSize(1);
    }

    @Test
    void createOrder_shouldRollbackTransaction_whenEmptyItemList() {
        // Arrange
        Long customerId = testUser.getId();
        List<CreateOrderItemRequest> items = List.of();

        // Act & Assert
        assertThatThrownBy(() -> orderService.createOrder(customerId, items, PaymentMethod.CARD))
                .isInstanceOf(jakarta.validation.ConstraintViolationException.class);

        // Verify complete rollback
        assertThat(orderRepository.count()).isZero();
        assertThat(paymentRepository.count()).isZero();

        // Verify no event was published
        assertThat(applicationEvents.stream(OrderCreatedEvent.class)).isEmpty();
    }

    @Test
    void createOrder_shouldRollbackTransaction_whenNullItemList() {
        // Arrange
        Long customerId = testUser.getId();
        List<CreateOrderItemRequest> items = null;

        // Act & Assert
        assertThatThrownBy(() -> orderService.createOrder(customerId, items, PaymentMethod.CARD))
                .isInstanceOf(NullPointerException.class);

        // Verify complete rollback
        assertThat(orderRepository.count()).isZero();
        assertThat(paymentRepository.count()).isZero();

        // Verify no event was published
        assertThat(applicationEvents.stream(OrderCreatedEvent.class)).isEmpty();
    }

    @Test
    void createOrder_shouldRollbackTransaction_whenNegativeQuantity() {
        // Arrange
        Long customerId = testUser.getId();
        List<CreateOrderItemRequest> items = List.of(
                new CreateOrderItemRequest(product1.getId(), -1) // Negative quantity
        );

        // Act & Assert
        assertThatThrownBy(() -> orderService.createOrder(customerId, items, PaymentMethod.CARD))
                .isInstanceOf(jakarta.validation.ConstraintViolationException.class);

        // Verify complete rollback
        assertThat(orderRepository.count()).isZero();
        assertThat(paymentRepository.count()).isZero();
        assertThat(applicationEvents.stream(OrderCreatedEvent.class)).isEmpty();
    }

    @Test
    void createOrder_shouldRollbackTransaction_whenZeroQuantity() {
        // Arrange
        Long customerId = testUser.getId();
        List<CreateOrderItemRequest> items = List.of(
                new CreateOrderItemRequest(product1.getId(), 0) // Zero quantity
        );

        // Act & Assert
        assertThatThrownBy(() -> orderService.createOrder(customerId, items, PaymentMethod.CARD))
                .isInstanceOf(jakarta.validation.ConstraintViolationException.class);

        // Verify complete rollback
        assertThat(orderRepository.count()).isZero();
        assertThat(paymentRepository.count()).isZero();
        assertThat(applicationEvents.stream(OrderCreatedEvent.class)).isEmpty();
    }
}
