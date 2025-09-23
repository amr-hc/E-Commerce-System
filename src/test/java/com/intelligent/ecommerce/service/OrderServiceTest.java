package com.intelligent.ecommerce.service;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.intelligent.ecommerce.dto.order.request.CreateOrderItemRequest;
import com.intelligent.ecommerce.entity.Order;
import com.intelligent.ecommerce.entity.Payment;
import com.intelligent.ecommerce.entity.Product;
import com.intelligent.ecommerce.enums.OrderStatus;
import com.intelligent.ecommerce.enums.PaymentMethod;
import com.intelligent.ecommerce.event.OrderCreatedEvent;
import com.intelligent.ecommerce.exception.InsufficientStockException;
import com.intelligent.ecommerce.repository.OrderRepository;
import com.intelligent.ecommerce.repository.PaymentRepository;
import com.intelligent.ecommerce.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private OrderService orderService;

    private CreateOrderItemRequest createItemRequest(Long productId, int qty) {
        return new CreateOrderItemRequest(productId, qty);
    }

    @Test
    void createOrder_shouldCreateOrderSuccessfully() {
        // Arrange
        Long customerId = 1L;
        List<CreateOrderItemRequest> items = List.of(createItemRequest(101L, 2));

        Product product = Product.builder()
            .id(101L)
            .price(50.0)
            .stockQuantity(10)
            .build();

        when(productRepository.findAllForUpdateByIdIn(List.of(101L)))
            .thenReturn(List.of(product));

        when(orderRepository.save(any(Order.class)))
            .thenAnswer(invocation -> {
                Order savedOrder = invocation.getArgument(0);
                savedOrder.setId(500L);
                return savedOrder;
            });

        // Act
        Order result = orderService.createOrder(customerId, items, PaymentMethod.CARD);

        // Assert
        assertThat(result.getCustomerId()).isEqualTo(customerId);
        assertThat(result.getTotalAmount()).isEqualTo(100.0);
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.CREATED);

        verify(productRepository).findAllForUpdateByIdIn(List.of(101L));
        verify(orderRepository).save(any(Order.class));
        verify(paymentRepository).save(any(Payment.class));
        verify(eventPublisher).publishEvent(any(OrderCreatedEvent.class));
    }

    @Test
    void createOrder_shouldThrowIfProductNotFound() {
        // Arrange
        List<CreateOrderItemRequest> items = List.of(createItemRequest(999L, 1));
        when(productRepository.findAllForUpdateByIdIn(List.of(999L)))
            .thenReturn(List.of());

        // Act & Assert
        assertThatThrownBy(() ->
            orderService.createOrder(1L, items, PaymentMethod.CARD)
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Product not found");
    }

    @Test
    void createOrder_shouldThrowIfStockInsufficient() {
        List<CreateOrderItemRequest> items = List.of(createItemRequest(101L, 5));

        Product product = Product.builder()
            .id(101L)
            .price(20.0)
            .stockQuantity(3)
            .build();

        when(productRepository.findAllForUpdateByIdIn(List.of(101L)))
            .thenReturn(List.of(product));

        assertThatThrownBy(() ->
            orderService.createOrder(1L, items, PaymentMethod.CASH)
        ).isInstanceOf(InsufficientStockException.class)
         .hasMessageContaining("Insufficient stock");
    }
}
