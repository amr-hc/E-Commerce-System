package com.intelligent.ecommerce.controller;

import com.intelligent.ecommerce.dto.product.CreateProductRequest;
import com.intelligent.ecommerce.entity.Product;
import com.intelligent.ecommerce.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductController {
    private final ProductService productService;

    @GetMapping("/search")
    public List<Product> searchByNameVector(
            @RequestParam("query") String query,
            @RequestParam(value = "limit", defaultValue = "10") int limit
    ) {
        return productService.searchByNameVector(query, limit);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product create(@RequestBody @Valid CreateProductRequest request) {
        return productService.create(request);
    }


}
