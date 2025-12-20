package com.intelligent.ecommerce.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.intelligent.ecommerce.entity.Product;

import jakarta.persistence.LockModeType;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdForUpdate(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id in :ids order by p.id asc")
    List<Product> findAllForUpdateByIdIn(@Param("ids") List<Long> ids);

    @Query(
            value = """
        SELECT *
        FROM products
        WHERE name_embedding IS NOT NULL
        ORDER BY name_embedding <=> CAST(:queryVector AS vector)
        LIMIT :limit
        """,
            nativeQuery = true
    )
    List<Product> searchByNameVector(@Param("queryVector") String queryVector,
                                     @Param("limit") int limit);


}
