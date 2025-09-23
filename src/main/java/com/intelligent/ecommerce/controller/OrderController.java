package com.intelligent.ecommerce.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.intelligent.ecommerce.dto.common.ApiResponse;
import com.intelligent.ecommerce.dto.common.PaginatedResponse;
import com.intelligent.ecommerce.dto.order.request.CreateOrderRequest;
import com.intelligent.ecommerce.dto.order.response.OrderReportResponse;
import com.intelligent.ecommerce.dto.order.response.OrderResponse;
import com.intelligent.ecommerce.entity.Order;
import com.intelligent.ecommerce.mapper.OrderMapper;
import com.intelligent.ecommerce.repository.projection.OrderReportRow;
import com.intelligent.ecommerce.service.OrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderMapper orderMapper;

    @GetMapping
    public ResponseEntity<ApiResponse<PaginatedResponse<OrderResponse>>> listOrders(
        @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<Order> page = orderService.listOrders(pageable);
        Page<OrderResponse> dtoPage = page.map(orderMapper::toDto);
        PaginatedResponse<OrderResponse> paginated = PaginatedResponse.fromPage(dtoPage);
        return ResponseEntity.ok(ApiResponse.success(paginated));
    }

    @GetMapping("/high-value")
    public ResponseEntity<ApiResponse<List<OrderReportRow>>> highValueOrders() {
        List<OrderReportRow> orders  = orderService.findHighValueOrders();
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(@Valid @RequestBody CreateOrderRequest createOrderRequest) {

        Order created = orderService.createOrder(createOrderRequest.getCustomerId(), createOrderRequest.getItems(), createOrderRequest.getPaymentMethod());

        return ResponseEntity.ok(ApiResponse.success(orderMapper.toDto(created)));
    }

}
