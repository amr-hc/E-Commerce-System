package com.intelligent.ecommerce.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.intelligent.ecommerce.dto.product.CreateProductRequest;
import com.intelligent.ecommerce.entity.Product;
import com.intelligent.ecommerce.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final VectorService vectorService;
    private final JdbcTemplate jdbcTemplate;

    public List<Product> searchByNameVector(String query, int limit) {
        var vec = vectorService.embed(query);
        var pgVector = toPgVectorLiteral(vec);
        return productRepository.searchByNameVector(pgVector, limit);
    }

    private static String toPgVectorLiteral(List<Double> embedding) {
        return "[" + embedding.stream()
                .map(d -> Double.toString(d))
                .collect(Collectors.joining(","))
                + "]";
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public Product create(CreateProductRequest request) {

        // 1️⃣ create product normally
        Product product = Product.builder()
                .name(request.getName())
                .stockQuantity(request.getStockQuantity())
                .price(request.getPrice())
                .build();

        product = productRepository.save(product);

        // 2️⃣ generate embedding from product name
        List<Double> embedding = vectorService.embed(product.getName());

        // 3️⃣ convert to pgvector literal
        String pgVector = toPgVectorLiteral(embedding);

        // 4️⃣ update vector column using native SQL
        jdbcTemplate.update(
                "UPDATE products SET name_embedding = CAST(? AS vector) WHERE id = ?",
                pgVector,
                product.getId()
        );

        return product;
    }

    @Cacheable(value = "products", cacheManager = "caffeineCacheManager")
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Cacheable(value = "products", key = "#id", cacheManager = "longTtlCacheManager")
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
    }

}
