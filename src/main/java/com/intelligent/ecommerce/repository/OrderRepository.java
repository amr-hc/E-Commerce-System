package com.intelligent.ecommerce.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.intelligent.ecommerce.entity.Order;
import com.intelligent.ecommerce.repository.projection.OrderReportRow;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"payment", "items", "items.product"})
    Page<Order> findAllByCustomerId(Long customerId, Pageable pageable);

    @Query(
        value = "SELECT * FROM orders o WHERE o.total_amount > 1000",
        nativeQuery = true
    )
    List<OrderReportRow> findHighValueOrders();
}
