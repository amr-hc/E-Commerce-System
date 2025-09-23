package com.intelligent.ecommerce.repository.projection;
public interface OrderReportRow {
    Long getId();
    Long getCustomerId();
    Double getTotalAmount();
    String getStatus();
    java.sql.Timestamp getCreatedAt();
}
