package com.intelligent.ecommerce.validation.validator;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.intelligent.ecommerce.dto.order.request.CreateOrderItemRequest;
import com.intelligent.ecommerce.validation.annotation.UniqueProductIds;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class UniqueProductIdsValidator implements ConstraintValidator<UniqueProductIds, List<CreateOrderItemRequest>> {

    @Override
    public boolean isValid(List<CreateOrderItemRequest> items, ConstraintValidatorContext context) {
        if (items == null) {
            return true;
        }

        Set<Long> seen = new HashSet<>();
        for (CreateOrderItemRequest item : items) {
            if (item.getProductId() == null) continue;
            if (!seen.add(item.getProductId())) {
                return false;
            }
        }
        return true;
    }
}
