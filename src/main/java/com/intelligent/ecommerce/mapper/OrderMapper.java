package com.intelligent.ecommerce.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import com.intelligent.ecommerce.dto.order.response.OrderItemResponse;
import com.intelligent.ecommerce.dto.order.response.OrderReportResponse;
import com.intelligent.ecommerce.dto.order.response.OrderResponse;
import com.intelligent.ecommerce.dto.order.response.ProductResponse;
import com.intelligent.ecommerce.entity.Order;
import com.intelligent.ecommerce.entity.OrderItem;
import com.intelligent.ecommerce.entity.Product;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrderMapper {

    OrderResponse toDto(Order order);

    List<OrderResponse> toDto(List<Order> orders);
    
    List<OrderReportResponse> toDtoReport(List<Order> orders);

    OrderItemResponse toDto(OrderItem item);

    ProductResponse toDto(Product product);

}
