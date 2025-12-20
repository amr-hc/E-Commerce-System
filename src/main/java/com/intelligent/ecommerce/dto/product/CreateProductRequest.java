package com.intelligent.ecommerce.dto.product;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateProductRequest {

    @NotBlank
    @Size(max = 255)
    private String name;

    @NotNull
    @Min(0)
    private Integer stockQuantity;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal price;
}
