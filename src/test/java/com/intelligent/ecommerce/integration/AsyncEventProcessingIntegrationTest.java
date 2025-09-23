package com.intelligent.ecommerce.integration;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

import com.intelligent.ecommerce.dto.order.request.CreateOrderItemRequest;
import com.intelligent.ecommerce.entity.Order;
import com.intelligent.ecommerce.entity.Payment;
import com.intelligent.ecommerce.entity.Product;
import com.intelligent.ecommerce.entity.User;
import com.intelligent.ecommerce.enums.OrderStatus;
import com.intelligent.ecommerce.enums.PaymentMethod;
import com.intelligent.ecommerce.event.OrderCreatedEvent;
import com.intelligent.ecommerce.repository.OrderRepository;
import com.intelligent.ecommerce.repository.PaymentRepository;
import com.intelligent.ecommerce.repository.ProductRepository;
import com.intelligent.ecommerce.repository.UserRepository;
import com.intelligent.ecommerce.service.OrderService;

@SpringBootTest
@ActiveProfiles("test")
@RecordApplicationEvents
@Transactional
class AsyncEventProcessingIntegrationTest {

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

    private Product product;
    private User customer1;
    private User customer2;
    private User customer3;

    @BeforeEach
    void setUp() {
        // امسح بالترتيب الآمن (payments -> orders -> products -> users)
        paymentRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();

        // عمل يوزرز للتجارب
        customer1 = userRepository.save(
            User.builder()
                .email("c1@example.com")
                .password("x") // في التست مش فارقة
                .isVerified(true)
                .build()
        );
        customer2 = userRepository.save(
            User.builder()
                .email("c2@example.com")
                .password("x")
                .isVerified(true)
                .build()
        );
        customer3 = userRepository.save(
            User.builder()
                .email("c3@example.com")
                .password("x")
                .isVerified(true)
                .build()
        );

        // منتج تيست بسعر BigDecimal
        product = Product.builder()
                .name("Test Product")
                .price(new BigDecimal("100.00"))
                .stockQuantity(10)
                .build();

        productRepository.save(product);
    }

    @Test
    void createOrder_shouldPublishOrderCreatedEvent() {
        // Arrange
        Long customerId = customer1.getId();
        List<CreateOrderItemRequest> items = List.of(
                new CreateOrderItemRequest(product.getId(), 2)
        );

        // Act
        Order result = orderService.createOrder(customerId, items, PaymentMethod.CARD);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(result.getCustomer()).isNotNull();
        assertThat(result.getCustomer().getId()).isEqualTo(customerId);

        // Verify event was published
        assertThat(applicationEvents.stream(OrderCreatedEvent.class)).hasSize(1);
        OrderCreatedEvent event = applicationEvents.stream(OrderCreatedEvent.class).findFirst().orElseThrow();
        assertThat(event.getOrderId()).isEqualTo(result.getId());
    }

    @Test
    void createOrder_shouldPublishEventOnlyOnSuccessfulTransaction() {
        // Arrange
        Long customerId = customer1.getId();
        List<CreateOrderItemRequest> validItems = List.of(
                new CreateOrderItemRequest(product.getId(), 2)
        );
        List<CreateOrderItemRequest> invalidItems = List.of(
                new CreateOrderItemRequest(999L, 1) // Non-existent product
        );

        // Act - Create valid order first
        Order validOrder = orderService.createOrder(customerId, validItems, PaymentMethod.CARD);

        // Act - Try to create invalid order
        try {
            orderService.createOrder(customer2.getId(), invalidItems, PaymentMethod.CARD);
        } catch (Exception ignored) {}

        // Assert
        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(paymentRepository.count()).isEqualTo(1);

        // Verify only one event was published (for the successful order)
        assertThat(applicationEvents.stream(OrderCreatedEvent.class)).hasSize(1);
        OrderCreatedEvent event = applicationEvents.stream(OrderCreatedEvent.class).findFirst().orElseThrow();
        assertThat(event.getOrderId()).isEqualTo(validOrder.getId());
    }

    @Test
    void createMultipleOrders_shouldPublishMultipleEvents() {
        // Arrange
        List<CreateOrderItemRequest> items = List.of(
                new CreateOrderItemRequest(product.getId(), 1)
        );

        // Act
        Order order1 = orderService.createOrder(customer1.getId(), items, PaymentMethod.CARD);
        Order order2 = orderService.createOrder(customer2.getId(), items, PaymentMethod.CASH);
        Order order3 = orderService.createOrder(customer3.getId(), items, PaymentMethod.CARD);

        // Assert
        assertThat(orderRepository.count()).isEqualTo(3);
        assertThat(paymentRepository.count()).isEqualTo(3);

        // Verify three events were published
        assertThat(applicationEvents.stream(OrderCreatedEvent.class)).hasSize(3);

        List<OrderCreatedEvent> events = applicationEvents.stream(OrderCreatedEvent.class).toList();
        assertThat(events).extracting(OrderCreatedEvent::getOrderId)
                .containsExactlyInAnyOrder(order1.getId(), order2.getId(), order3.getId());
    }

    @Test
    void createOrder_shouldPublishEventWithCorrectOrderId() {
        // Arrange
        Long customerId = customer1.getId();
        List<CreateOrderItemRequest> items = List.of(
                new CreateOrderItemRequest(product.getId(), 3)
        );

        // Act
        Order result = orderService.createOrder(customerId, items, PaymentMethod.CARD);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();

        // Verify event contains correct order ID
        assertThat(applicationEvents.stream(OrderCreatedEvent.class)).hasSize(1);
        OrderCreatedEvent event = applicationEvents.stream(OrderCreatedEvent.class).findFirst().orElseThrow();
        assertThat(event.getOrderId()).isEqualTo(result.getId());
        assertThat(event.getOrderId()).isNotNull();
    }

    @Test
    void createOrder_shouldPublishEventAfterTransactionCommit() {
        // Arrange
        Long customerId = customer1.getId();
        List<CreateOrderItemRequest> items = List.of(
                new CreateOrderItemRequest(product.getId(), 1)
        );

        // Act
        Order result = orderService.createOrder(customerId, items, PaymentMethod.CARD);

        // Assert
        assertThat(result).isNotNull();

        // Verify order exists in database (transaction committed)
        Order savedOrder = orderRepository.findById(result.getId()).orElseThrow();
        assertThat(savedOrder.getCustomer()).isNotNull();
        assertThat(savedOrder.getCustomer().getId()).isEqualTo(customerId);

        // Verify payment exists in database (transaction committed)
        Payment savedPayment = paymentRepository.findByOrderId(result.getId()).orElseThrow();
        // كان 100.0 double — دلوقتي BigDecimal
        assertThat(savedPayment.getAmount()).isNotNull();
        assertThat(savedPayment.getAmount().compareTo(new BigDecimal("100.00"))).isZero();

        // Verify event was published (after transaction commit)
        assertThat(applicationEvents.stream(OrderCreatedEvent.class)).hasSize(1);
        OrderCreatedEvent event = applicationEvents.stream(OrderCreatedEvent.class).findFirst().orElseThrow();
        assertThat(event.getOrderId()).isEqualTo(result.getId());
    }

    @Test
    void createOrder_shouldNotPublishEvent_whenTransactionRollsBack() {
        // Arrange
        Long customerId = customer1.getId();
        List<CreateOrderItemRequest> invalidItems = List.of(
                new CreateOrderItemRequest(999L, 1) // Non-existent product
        );

        // Act & Assert
        try {
            orderService.createOrder(customerId, invalidItems, PaymentMethod.CARD);
        } catch (Exception ignored) {}

        // Verify no order was created (transaction rolled back)
        assertThat(orderRepository.count()).isZero();
        assertThat(paymentRepository.count()).isZero();

        // Verify no event was published (transaction rolled back)
        assertThat(applicationEvents.stream(OrderCreatedEvent.class)).isEmpty();
    }
}
