package com.intelligent.ecommerce.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class OrderService {
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public Page<Order> listOrders(Long customerId, Pageable pageable) {
        return orderRepository.findAllByCustomerId(customerId, pageable);
    }

    public List<OrderReportRow> findHighValueOrders() {
        return orderRepository.findHighValueOrders();
    }

    @Transactional
    public Order createOrder(Long customerId, List<CreateOrderItemRequest> rawItems, PaymentMethod paymentMethod) {

        List<Long> productIds = rawItems.stream()
            .map(CreateOrderItemRequest::getProductId)
            .sorted()
            .toList();

        List<Product> products = productRepository.findAllForUpdateByIdIn(productIds);

        Map<Long, Product> productMap = products.stream()
            .collect(java.util.stream.Collectors.toMap(Product::getId, p -> p));

        BigDecimal totalAmount = BigDecimal.ZERO;

        User customer = userRepository.getReferenceById(customerId);

        Order order = Order.builder()
            .customer(customer)
            .status(OrderStatus.CREATED)
            .build();

        List<OrderItem> items = new ArrayList<>();

        for (CreateOrderItemRequest reqItem : rawItems) {
            Product product = productMap.get(reqItem.getProductId());
            if (product == null) {
                throw new IllegalArgumentException("Product not found: " + reqItem.getProductId());
            }
            if (product.getStockQuantity() < reqItem.getQuantity()) {
                throw new InsufficientStockException("Insufficient stock for product " + product.getId());
            }

            product.setStockQuantity(product.getStockQuantity() - reqItem.getQuantity());

            BigDecimal linePrice = product.getPrice().multiply(BigDecimal.valueOf(reqItem.getQuantity()));
            totalAmount = totalAmount.add(linePrice);

            OrderItem oi = OrderItem.builder()
                .product(product)
                .quantity(reqItem.getQuantity())
                .price(linePrice)
                .order(order)
                .build();

            items.add(oi);
        }

        order.setTotalAmount(totalAmount);
        order.setItems(items);

        order = orderRepository.save(order);

        Payment payment = Payment.builder()
            .order(order)
            .amount(totalAmount)
            .status("PENDING")
            .paymentMethod(paymentMethod)
            .build();

        paymentRepository.save(payment);

        eventPublisher.publishEvent(new OrderCreatedEvent(order.getId()));

        return order;
    }



}

