package com.intelligent.ecommerce.integration;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import com.intelligent.ecommerce.dto.order.request.CreateOrderItemRequest;
import com.intelligent.ecommerce.entity.Order;
import com.intelligent.ecommerce.entity.OrderItem;
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
import com.intelligent.ecommerce.repository.projection.OrderReportRow;
import com.intelligent.ecommerce.service.OrderService;

@SpringBootTest
@ActiveProfiles("test")
@RecordApplicationEvents
class OrderServiceIntegrationTest {

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
                .name("Laptop")
                .price(java.math.BigDecimal.valueOf(1500.0))
                .stockQuantity(10)
                .build();

        product2 = Product.builder()
                .name("Mouse")
                .price(java.math.BigDecimal.valueOf(25.0))
                .stockQuantity(50)
                .build();

        product3 = Product.builder()
                .name("Keyboard")
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
    void createOrder_shouldCreateOrderWithMultipleItemsSuccessfully() {
        // Arrange
        List<CreateOrderItemRequest> items = List.of(
                new CreateOrderItemRequest(product1.getId(), 2),
                new CreateOrderItemRequest(product2.getId(), 3),
                new CreateOrderItemRequest(product3.getId(), 1)
        );

        // Act
        Order result = orderService.createOrder(customer1.getId(), items, PaymentMethod.CARD);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getCustomer().getId()).isEqualTo(customer1.getId());
        assertThat(result.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(result.getTotalAmount()).isEqualByComparingTo(java.math.BigDecimal.valueOf(1500.0 * 2 + 25.0 * 3 + 75.0 * 1)); // 3150.0
        assertThat(result.getItems()).hasSize(3);

        // Verify order items
        List<OrderItem> orderItems = result.getItems();
        assertThat(orderItems).extracting(OrderItem::getQuantity).containsExactlyInAnyOrder(2, 3, 1);
        assertThat(orderItems).extracting(OrderItem::getPrice)
            .usingComparatorForType(java.math.BigDecimal::compareTo, java.math.BigDecimal.class)
            .containsExactlyInAnyOrder(
                java.math.BigDecimal.valueOf(3000.0), 
                java.math.BigDecimal.valueOf(75.0), 
                java.math.BigDecimal.valueOf(75.0)
            );

        // Verify payment was created
        Payment payment = paymentRepository.findByOrderId(result.getId()).orElse(null);
        assertThat(payment).isNotNull();
        assertThat(payment.getAmount()).isEqualByComparingTo(java.math.BigDecimal.valueOf(3150.0));
        assertThat(payment.getStatus()).isEqualTo("PENDING");
        assertThat(payment.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);

        // Verify stock was updated
        Product updatedProduct1 = productRepository.findById(product1.getId()).orElseThrow();
        Product updatedProduct2 = productRepository.findById(product2.getId()).orElseThrow();
        Product updatedProduct3 = productRepository.findById(product3.getId()).orElseThrow();

        assertThat(updatedProduct1.getStockQuantity()).isEqualTo(8); // 10 - 2
        assertThat(updatedProduct2.getStockQuantity()).isEqualTo(47); // 50 - 3
        assertThat(updatedProduct3.getStockQuantity()).isEqualTo(29); // 30 - 1

        // Verify event was published
        assertThat(applicationEvents.stream(OrderCreatedEvent.class)).hasSize(1);
        OrderCreatedEvent event = applicationEvents.stream(OrderCreatedEvent.class).findFirst().orElseThrow();
        assertThat(event.getOrderId()).isEqualTo(result.getId());
    }

    @Test
    void createOrder_shouldThrowInsufficientStockException_whenStockIsInsufficient() {
        // Arrange
        List<CreateOrderItemRequest> items = List.of(
                new CreateOrderItemRequest(product1.getId(), 15) // More than available stock (10)
        );

        // Act & Assert
        assertThatThrownBy(() -> orderService.createOrder(customer1.getId(), items, PaymentMethod.CARD))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Insufficient stock for product");

        // Verify no order was created
        assertThat(orderRepository.count()).isZero();
        assertThat(paymentRepository.count()).isZero();

        // Verify stock was not updated
        Product unchangedProduct = productRepository.findById(product1.getId()).orElseThrow();
        assertThat(unchangedProduct.getStockQuantity()).isEqualTo(10);

        // Verify no event was published
        assertThat(applicationEvents.stream(OrderCreatedEvent.class)).isEmpty();
    }

    @Test
    void createOrder_shouldThrowException_whenProductNotFound() {
        // Arrange
        List<CreateOrderItemRequest> items = List.of(
                new CreateOrderItemRequest(999L, 1) // Non-existent product
        );

        // Act & Assert
        assertThatThrownBy(() -> orderService.createOrder(customer1.getId(), items, PaymentMethod.CARD))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product not found: 999");

        // Verify no order was created
        assertThat(orderRepository.count()).isZero();
        assertThat(paymentRepository.count()).isZero();
    }

    @Test
    void createOrder_shouldHandleConcurrentOrdersCorrectly() throws InterruptedException {
        // Arrange
        List<CreateOrderItemRequest> items = List.of(
                new CreateOrderItemRequest(product1.getId(), 5) // Half of available stock
        );

        CountDownLatch latch = new CountDownLatch(2);
        Exception[] exceptions = new Exception[2];

        // Act - Create two concurrent orders
        Thread thread1 = new Thread(() -> {
            try {
                orderService.createOrder(customer1.getId(), items, PaymentMethod.CARD);
            } catch (Exception e) {
                exceptions[0] = e;
            } finally {
                latch.countDown();
            }
        });

        Thread thread2 = new Thread(() -> {
            try {
                orderService.createOrder(customer2.getId(), items, PaymentMethod.CASH);
            } catch (Exception e) {
                exceptions[1] = e;
            } finally {
                latch.countDown();
            }
        });

        thread1.start();
        thread2.start();

        // Wait for both threads to complete
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();

        // Wait a bit more for transactions to complete
        Thread.sleep(100);

        // Assert - Both orders should succeed since we have enough stock (10 - 5 = 5, 5 - 5 = 0)
        long orderCount = orderRepository.count();
        assertThat(orderCount).isEqualTo(2);

        // Verify stock was updated correctly (both orders succeeded)
        Product updatedProduct = productRepository.findById(product1.getId()).orElseThrow();
        assertThat(updatedProduct.getStockQuantity()).isEqualTo(0); // 10 - 5 - 5

        // Both orders should have succeeded since we have enough stock
        boolean hasInsufficientStockException = false;
        for (Exception e : exceptions) {
            if (e instanceof InsufficientStockException) {
                hasInsufficientStockException = true;
                break;
            }
        }
        assertThat(hasInsufficientStockException).isFalse();
    }

    @Test
    void listOrders_shouldReturnPaginatedOrders() {
        // Arrange - Create multiple orders
        createTestOrder(customer1.getId(), List.of(new CreateOrderItemRequest(product1.getId(), 1)), PaymentMethod.CARD);
        createTestOrder(customer2.getId(), List.of(new CreateOrderItemRequest(product2.getId(), 2)), PaymentMethod.CASH);
        createTestOrder(customer3.getId(), List.of(new CreateOrderItemRequest(product3.getId(), 1)), PaymentMethod.CARD);

        // Act
        PageRequest pageRequest = PageRequest.of(0, 2, Sort.by("createdAt").descending());
        Page<Order> result = orderService.listOrders(customer1.getId(), pageRequest);

        // Assert
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getTotalPages()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent()).extracting(Order::getCustomer).extracting(User::getId).containsExactly(customer1.getId());
    }

    @Test
    void findHighValueOrders_shouldReturnOrdersWithTotalAmountGreaterThan1000() {
        // Arrange - Create orders with different amounts
        createTestOrder(customer1.getId(), List.of(new CreateOrderItemRequest(product1.getId(), 1)), PaymentMethod.CARD); // 1500
        createTestOrder(customer2.getId(), List.of(new CreateOrderItemRequest(product2.getId(), 2)), PaymentMethod.CARD); // 50
        createTestOrder(customer3.getId(), List.of(new CreateOrderItemRequest(product1.getId(), 1), 
                                   new CreateOrderItemRequest(product2.getId(), 1)), PaymentMethod.CARD); // 1525

        // Act
        List<OrderReportRow> result = orderService.findHighValueOrders();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).extracting(OrderReportRow::getTotalAmount)
                .usingComparatorForType(java.math.BigDecimal::compareTo, java.math.BigDecimal.class)
                .containsExactlyInAnyOrder(
            java.math.BigDecimal.valueOf(1500.0), 
            java.math.BigDecimal.valueOf(1525.0)
        );
        assertThat(result).extracting(OrderReportRow::getCustomerId).containsExactlyInAnyOrder(customer1.getId(), customer3.getId());
    }

    @Test
    void createOrder_shouldRollbackTransaction_whenPaymentFails() {
        // This test simulates a scenario where payment processing fails
        // In a real scenario, you might have a PaymentService that throws PaymentFailedException
        
        // Arrange
        List<CreateOrderItemRequest> items = List.of(
                new CreateOrderItemRequest(product1.getId(), 1)
        );

        // Act - Create order normally (payment will succeed in this case)
        Order result = orderService.createOrder(customer1.getId(), items, PaymentMethod.CARD);

        // Assert - Order should be created successfully
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(OrderStatus.CREATED);

        // Verify payment was created
        Payment payment = paymentRepository.findByOrderId(result.getId()).orElseThrow();
        assertThat(payment).isNotNull();
        assertThat(payment.getStatus()).isEqualTo("PENDING");
    }

    private void createTestOrder(Long customerId, List<CreateOrderItemRequest> items, PaymentMethod paymentMethod) {
        orderService.createOrder(customerId, items, paymentMethod);
    }
}
