package com.intelligent.ecommerce.repository.projection;

import java.math.BigDecimal;
import java.time.Instant;

public interface OrderReportRow {
    Long getId();
    Long getCustomerId();
    BigDecimal getTotalAmount();
    String getStatus();
    Instant getCreatedAt();
}
