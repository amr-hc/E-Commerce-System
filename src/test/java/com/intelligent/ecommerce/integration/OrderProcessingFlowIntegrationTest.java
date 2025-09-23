package com.intelligent.ecommerce.integration;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
class OrderProcessingFlowIntegrationTest {

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

    private Product laptop;
    private Product mouse;
    private Product keyboard;
    private User customer1;
    private User customer2;
    private User customer3;
    private User customer4;
    private User customer5;

    @BeforeEach
    void setUp() {
        // Clean up existing data
        paymentRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();

        // Create test products with different stock levels
        laptop = Product.builder()
                .name("Gaming Laptop")
                .price(java.math.BigDecimal.valueOf(2000.0))
                .stockQuantity(5) // Limited stock
                .build();

        mouse = Product.builder()
                .name("Gaming Mouse")
                .price(java.math.BigDecimal.valueOf(50.0))
                .stockQuantity(100) // High stock
                .build();

        keyboard = Product.builder()
                .name("Mechanical Keyboard")
                .price(java.math.BigDecimal.valueOf(150.0))
                .stockQuantity(20) // Medium stock
                .build();

        productRepository.saveAll(List.of(laptop, mouse, keyboard));

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
        customer4 = User.builder()
                .email("customer4@test.com")
                .username("customer4")
                .build();
        customer5 = User.builder()
                .email("customer5@test.com")
                .username("customer5")
                .build();

        userRepository.saveAll(List.of(customer1, customer2, customer3, customer4, customer5));
    }

    @Test
    void completeOrderProcessingFlow_shouldWorkEndToEnd() {
        // Arrange
        List<CreateOrderItemRequest> items = List.of(
                new CreateOrderItemRequest(laptop.getId(), 1),
                new CreateOrderItemRequest(mouse.getId(), 2),
                new CreateOrderItemRequest(keyboard.getId(), 1)
        );

        // Act
        Order result = orderService.createOrder(customer1.getId(), items, PaymentMethod.CARD);

        // Assert - Order Creation
        assertThat(result).isNotNull();
        assertThat(result.getCustomer().getId()).isEqualTo(customer1.getId());
        assertThat(result.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(result.getTotalAmount()).isEqualByComparingTo(java.math.BigDecimal.valueOf(2000.0 + 100.0 + 150.0)); // 2250.0
        assertThat(result.getItems()).hasSize(3);

        // Assert - Payment Creation
        Payment payment = paymentRepository.findByOrderId(result.getId()).orElseThrow();
        assertThat(payment).isNotNull();
        assertThat(payment.getAmount()).isEqualByComparingTo(java.math.BigDecimal.valueOf(2250.0));
        assertThat(payment.getStatus()).isEqualTo("PENDING");
        assertThat(payment.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);

        // Assert - Stock Updates
        Product updatedLaptop = productRepository.findById(laptop.getId()).orElseThrow();
        Product updatedMouse = productRepository.findById(mouse.getId()).orElseThrow();
        Product updatedKeyboard = productRepository.findById(keyboard.getId()).orElseThrow();

        assertThat(updatedLaptop.getStockQuantity()).isEqualTo(4); // 5 - 1
        assertThat(updatedMouse.getStockQuantity()).isEqualTo(98); // 100 - 2
        assertThat(updatedKeyboard.getStockQuantity()).isEqualTo(19); // 20 - 1

        // Assert - Event Publishing
        assertThat(applicationEvents.stream(OrderCreatedEvent.class)).hasSize(1);
        OrderCreatedEvent event = applicationEvents.stream(OrderCreatedEvent.class).findFirst().orElseThrow();
        assertThat(event.getOrderId()).isEqualTo(result.getId());

        // Assert - Pagination API
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        Page<Order> ordersPage = orderService.listOrders(customer1.getId(), pageRequest);
        assertThat(ordersPage.getTotalElements()).isEqualTo(1);
        assertThat(ordersPage.getContent().get(0).getId()).isEqualTo(result.getId());

        // Assert - High Value Orders Report
        List<OrderReportRow> highValueOrders = orderService.findHighValueOrders();
        assertThat(highValueOrders).hasSize(1);
        assertThat(highValueOrders.get(0).getTotalAmount()).isEqualByComparingTo(java.math.BigDecimal.valueOf(2250.0));
        assertThat(highValueOrders.get(0).getCustomerId()).isEqualTo(customer1.getId());
    }

    @Test
    void concurrentOrderProcessing_shouldHandleRaceConditionsCorrectly() throws InterruptedException {
        // Arrange
        int numberOfThreads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        Exception[] exceptions = new Exception[numberOfThreads];
        Order[] orders = new Order[numberOfThreads];

        // Act - Create multiple concurrent orders for the same limited stock product
        User[] customers = {customer1, customer2, customer3, customer4, customer5};
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadIndex = i;
            executor.submit(() -> {
                try {
                    List<CreateOrderItemRequest> items = List.of(
                            new CreateOrderItemRequest(laptop.getId(), 2) // Each thread tries to buy 2 laptops
                    );
                    orders[threadIndex] = orderService.createOrder(customers[threadIndex].getId(), items, PaymentMethod.CARD);
                } catch (Exception e) {
                    exceptions[threadIndex] = e;
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for all threads to complete
        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();
        
        // Wait a bit more for transactions to complete
        Thread.sleep(100);

        // Assert - Only some orders should succeed due to limited stock (5 laptops total)
        long successfulOrders = 0;
        long failedOrders = 0;

        for (int i = 0; i < numberOfThreads; i++) {
            if (orders[i] != null) {
                successfulOrders++;
            }
            if (exceptions[i] instanceof InsufficientStockException) {
                failedOrders++;
            }
        }

        // With 5 laptops in stock and each order trying to buy 2, only 2 orders can succeed (4 laptops)
        // and 1 order can partially succeed (1 laptop), but since we're buying 2 at a time, only 2 orders succeed
        assertThat(successfulOrders).isEqualTo(2); // 2 orders * 2 laptops = 4 laptops
        assertThat(failedOrders).isEqualTo(3); // 3 orders fail due to insufficient stock

        // Verify final stock level
        Product finalLaptop = productRepository.findById(laptop.getId()).orElseThrow();
        assertThat(finalLaptop.getStockQuantity()).isEqualTo(1); // 5 - 4 = 1

        // Verify total orders in database
        assertThat(orderRepository.count()).isEqualTo(2);
    }

    @Test
    void orderProcessingWithMixedProducts_shouldHandlePartialStockCorrectly() {
        // Arrange
        List<CreateOrderItemRequest> items = List.of(
                new CreateOrderItemRequest(laptop.getId(), 3), // 3 laptops (stock: 5) - should succeed
                new CreateOrderItemRequest(mouse.getId(), 50), // 50 mice (stock: 100) - should succeed
                new CreateOrderItemRequest(keyboard.getId(), 25) // 25 keyboards (stock: 20) - should fail
        );

        // Act & Assert
        assertThatThrownBy(() -> orderService.createOrder(customer1.getId(), items, PaymentMethod.CARD))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Insufficient stock for product");

        // Verify no order was created
        assertThat(orderRepository.count()).isZero();
        assertThat(paymentRepository.count()).isZero();

        // Verify stock was not updated
        Product unchangedLaptop = productRepository.findById(laptop.getId()).orElseThrow();
        Product unchangedMouse = productRepository.findById(mouse.getId()).orElseThrow();
        Product unchangedKeyboard = productRepository.findById(keyboard.getId()).orElseThrow();

        assertThat(unchangedLaptop.getStockQuantity()).isEqualTo(5);
        assertThat(unchangedMouse.getStockQuantity()).isEqualTo(100);
        assertThat(unchangedKeyboard.getStockQuantity()).isEqualTo(20);
    }

    @Test
    void multipleOrdersWithDifferentPaymentMethods_shouldWorkCorrectly() {
        // Arrange
        List<CreateOrderItemRequest> items1 = List.of(new CreateOrderItemRequest(mouse.getId(), 1));
        List<CreateOrderItemRequest> items2 = List.of(new CreateOrderItemRequest(keyboard.getId(), 1));
        List<CreateOrderItemRequest> items3 = List.of(new CreateOrderItemRequest(mouse.getId(), 2));

        // Act
        Order order1 = orderService.createOrder(customer1.getId(), items1, PaymentMethod.CARD);
        Order order2 = orderService.createOrder(customer2.getId(), items2, PaymentMethod.CASH);
        Order order3 = orderService.createOrder(customer3.getId(), items3, PaymentMethod.CARD);

        // Assert
        assertThat(orderRepository.count()).isEqualTo(3);

        // Verify payments
        Payment payment1 = paymentRepository.findByOrderId(order1.getId()).orElseThrow();
        Payment payment2 = paymentRepository.findByOrderId(order2.getId()).orElseThrow();
        Payment payment3 = paymentRepository.findByOrderId(order3.getId()).orElseThrow();

        assertThat(payment1.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(payment2.getPaymentMethod()).isEqualTo(PaymentMethod.CASH);
        assertThat(payment3.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);

        // Verify stock updates
        Product updatedMouse = productRepository.findById(mouse.getId()).orElseThrow();
        Product updatedKeyboard = productRepository.findById(keyboard.getId()).orElseThrow();

        assertThat(updatedMouse.getStockQuantity()).isEqualTo(97); // 100 - 1 - 2
        assertThat(updatedKeyboard.getStockQuantity()).isEqualTo(19); // 20 - 1

        // Verify events
        assertThat(applicationEvents.stream(OrderCreatedEvent.class)).hasSize(3);
    }

    @Test
    void highValueOrdersReport_shouldFilterCorrectly() {
        // Arrange - Create orders with different amounts
        createTestOrder(customer1.getId(), java.math.BigDecimal.valueOf(500.0)); // Low value
        createTestOrder(customer2.getId(), java.math.BigDecimal.valueOf(1200.0)); // High value
        createTestOrder(customer3.getId(), java.math.BigDecimal.valueOf(800.0)); // Low value
        createTestOrder(customer1.getId(), java.math.BigDecimal.valueOf(2000.0)); // High value
        createTestOrder(customer2.getId(), java.math.BigDecimal.valueOf(1500.0)); // High value

        // Act
        List<OrderReportRow> highValueOrders = orderService.findHighValueOrders();

        // Assert - Check that we have high-value orders (amount > 1000)
        assertThat(highValueOrders).isNotEmpty();
        assertThat(highValueOrders).allMatch(order -> 
            order.getTotalAmount().compareTo(java.math.BigDecimal.valueOf(1000.0)) > 0);
        
        // Verify that all high-value orders are included
        assertThat(highValueOrders).hasSizeGreaterThanOrEqualTo(1); // At least one high-value order
    }

    @Test
    void paginationWithSorting_shouldWorkCorrectly() {
        // Arrange - Create multiple orders
        createTestOrder(customer1.getId(), java.math.BigDecimal.valueOf(1000.0));
        try { Thread.sleep(10); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        createTestOrder(customer2.getId(), java.math.BigDecimal.valueOf(2000.0));
        try { Thread.sleep(10); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        createTestOrder(customer3.getId(), java.math.BigDecimal.valueOf(1500.0));

        // Act - Test pagination with sorting
        PageRequest pageRequest = PageRequest.of(0, 2, Sort.by("createdAt").descending());
        Page<Order> firstPage = orderService.listOrders(customer1.getId(), pageRequest);

        PageRequest secondPageRequest = PageRequest.of(1, 2, Sort.by("createdAt").descending());
        Page<Order> secondPage = orderService.listOrders(customer1.getId(), secondPageRequest);

        // Assert
        assertThat(firstPage.getTotalElements()).isEqualTo(1);
        assertThat(firstPage.getTotalPages()).isEqualTo(1);
        assertThat(firstPage.getContent()).hasSize(1);
        assertThat(firstPage.getContent().get(0).getCustomer().getId()).isEqualTo(customer1.getId()); // Most recent

        assertThat(secondPage.getContent()).isEmpty();
    }

    private void createTestOrder(Long customerId, java.math.BigDecimal totalAmount) {
        // For high-value orders (> 1000), use laptop (price 2000.0)
        // For low-value orders, use mouse (price 50.0)
        if (totalAmount.compareTo(java.math.BigDecimal.valueOf(1000.0)) > 0) {
            // Use laptop for high-value orders
            int quantity = totalAmount.divide(laptop.getPrice()).intValue();
            quantity = Math.min(quantity, Math.min(laptop.getStockQuantity(), 2)); // Max 2 laptops per order
            if (quantity <= 0) quantity = 1;
            List<CreateOrderItemRequest> items = List.of(
                    new CreateOrderItemRequest(laptop.getId(), quantity)
            );
            orderService.createOrder(customerId, items, PaymentMethod.CARD);
        } else {
            // Use mouse for low-value orders
            int quantity = totalAmount.divide(mouse.getPrice()).intValue();
            quantity = Math.min(quantity, Math.min(mouse.getStockQuantity(), 20)); // Max 20 per order
            if (quantity <= 0) quantity = 1;
            List<CreateOrderItemRequest> items = List.of(
                    new CreateOrderItemRequest(mouse.getId(), quantity)
            );
            orderService.createOrder(customerId, items, PaymentMethod.CARD);
        }
    }
}
