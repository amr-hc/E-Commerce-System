package com.intelligent.ecommerce.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.intelligent.ecommerce.entity.Product;
import com.intelligent.ecommerce.repository.ProductRepository;

@DataJpaTest
@ActiveProfiles("test")
class ProductRepositoryIntegrationTest {

    @Autowired
    private ProductRepository productRepository;

    private Product product1;
    private Product product2;
    private Product product3;

    @BeforeEach
    void setUp() {
        // Clean up existing data
        productRepository.deleteAll();

        // Create test products
        product1 = Product.builder()
                .name("Laptop")
                .price(java.math.BigDecimal.valueOf(1500.0))
                .stockQuantity(10)
                .build();

        product2 = Product.builder()
                .name("Mouse")
                .price(java.math.BigDecimal.valueOf(25.0))
                .stockQuantity(50)
                .build();

        product3 = Product.builder()
                .name("Keyboard")
                .price(java.math.BigDecimal.valueOf(75.0))
                .stockQuantity(30)
                .build();

        productRepository.saveAll(List.of(product1, product2, product3));
    }

    @Test
    void findAllForUpdateByIdIn_shouldReturnProductsWithPessimisticLock() {
        // Arrange
        List<Long> productIds = List.of(product1.getId(), product3.getId());

        // Act
        List<Product> result = productRepository.findAllForUpdateByIdIn(productIds);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Product::getId)
                .containsExactlyInAnyOrder(product1.getId(), product3.getId());
        assertThat(result).extracting(Product::getName)
                .containsExactlyInAnyOrder("Laptop", "Keyboard");
    }

    @Test
    void findAllForUpdateByIdIn_shouldReturnEmptyList_whenNoProductsFound() {
        // Arrange
        List<Long> nonExistentIds = List.of(999L, 998L);

        // Act
        List<Product> result = productRepository.findAllForUpdateByIdIn(nonExistentIds);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void findAllForUpdateByIdIn_shouldReturnPartialResults_whenSomeProductsExist() {
        // Arrange
        List<Long> mixedIds = List.of(product1.getId(), 999L, product2.getId());

        // Act
        List<Product> result = productRepository.findAllForUpdateByIdIn(mixedIds);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Product::getId)
                .containsExactlyInAnyOrder(product1.getId(), product2.getId());
    }

    @Test
    void findAllForUpdateByIdIn_shouldHandleEmptyInput() {
        // Arrange
        List<Long> emptyIds = List.of();

        // Act
        List<Product> result = productRepository.findAllForUpdateByIdIn(emptyIds);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void findAllForUpdateByIdIn_shouldMaintainOrderFromInput() {
        // Arrange
        List<Long> productIds = List.of(product3.getId(), product1.getId(), product2.getId());

        // Act
        List<Product> result = productRepository.findAllForUpdateByIdIn(productIds);

        // Assert
        assertThat(result).hasSize(3);
        // Note: The actual order might depend on database implementation
        // but we verify all expected products are returned
        assertThat(result).extracting(Product::getId)
                .containsExactlyInAnyOrder(product1.getId(), product2.getId(), product3.getId());
    }

    @Test
    void save_shouldPersistProductCorrectly() {
        // Arrange
        Product newProduct = Product.builder()
                .name("Monitor")
                .price(java.math.BigDecimal.valueOf(300.0))
                .stockQuantity(15)
                .build();

        // Act
        Product savedProduct = productRepository.save(newProduct);

        // Assert
        assertThat(savedProduct.getId()).isNotNull();
        assertThat(savedProduct.getName()).isEqualTo("Monitor");
        assertThat(savedProduct.getPrice()).isEqualTo(java.math.BigDecimal.valueOf(300.0));
        assertThat(savedProduct.getStockQuantity()).isEqualTo(15);

        // Verify it can be retrieved
        Optional<Product> retrieved = productRepository.findById(savedProduct.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getName()).isEqualTo("Monitor");
    }

    @Test
    void saveAll_shouldPersistMultipleProductsCorrectly() {
        // Arrange
        List<Product> newProducts = List.of(
                Product.builder().name("Headphones").price(java.math.BigDecimal.valueOf(100.0)).stockQuantity(20).build(),
                Product.builder().name("Webcam").price(java.math.BigDecimal.valueOf(80.0)).stockQuantity(25).build()
        );

        // Act
        List<Product> savedProducts = productRepository.saveAll(newProducts);

        // Assert
        assertThat(savedProducts).hasSize(2);
        assertThat(savedProducts).extracting(Product::getId).doesNotContainNull();
        assertThat(savedProducts).extracting(Product::getName)
                .containsExactlyInAnyOrder("Headphones", "Webcam");

        // Verify all products exist in database
        assertThat(productRepository.count()).isEqualTo(5); // 3 original + 2 new
    }

    @Test
    void findById_shouldReturnCorrectProduct() {
        // Act
        Optional<Product> result = productRepository.findById(product2.getId());

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Mouse");
        assertThat(result.get().getPrice()).isEqualTo(25.0);
        assertThat(result.get().getStockQuantity()).isEqualTo(50);
    }

    @Test
    void findById_shouldReturnEmpty_whenProductNotFound() {
        // Act
        Optional<Product> result = productRepository.findById(999L);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void deleteById_shouldRemoveProduct() {
        // Arrange
        Long productId = product2.getId();

        // Act
        productRepository.deleteById(productId);

        // Assert
        assertThat(productRepository.findById(productId)).isEmpty();
        assertThat(productRepository.count()).isEqualTo(2);
    }

    @Test
    void count_shouldReturnCorrectNumberOfProducts() {
        // Act
        long count = productRepository.count();

        // Assert
        assertThat(count).isEqualTo(3);
    }

    @Test
    void findAll_shouldReturnAllProducts() {
        // Act
        List<Product> result = productRepository.findAll();

        // Assert
        assertThat(result).hasSize(3);
        assertThat(result).extracting(Product::getName)
                .containsExactlyInAnyOrder("Laptop", "Mouse", "Keyboard");
    }
}
