package com.intelligent.ecommerce.validation.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.intelligent.ecommerce.validation.validator.UniqueProductIdsValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Constraint(validatedBy = UniqueProductIdsValidator.class)
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface UniqueProductIds {
    String message() default "Duplicate productId found in items";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
