package com.intelligent.ecommerce.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.intelligent.ecommerce.enums.OrderStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(
    name = "orders",
    indexes = {
        @Index(name = "idx_orders_total_amount", columnList = "totalAmount"),
        @Index(name = "idx_orders_created_at", columnList = "createdAt")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode(exclude = {"payment", "items"})
@ToString(exclude = {"payment", "items"})
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @NotNull(message = "Total amount is required")
    @DecimalMin(value = "0.00", inclusive = false, message = "Total amount must be greater than zero")
    @Digits(integer = 12, fraction = 2, message = "Total amount must have up to 12 digits and 2 decimals")
    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private OrderStatus status;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Valid
    private Payment payment;

    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    @Valid
    @Size(min = 1, message = "Order must contain at least one item")
    private List<OrderItem> items = new ArrayList<>();
}
