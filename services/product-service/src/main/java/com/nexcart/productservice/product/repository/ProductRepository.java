package com.nexcart.productservice.product.repository;

import com.nexcart.productservice.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    Optional<Product> findBySlug(String slug);

    List<Product> findByBrandIdAndIsActiveTrue(Long brandId);

    List<Product> findByCategoryIdAndIsActiveTrue(Long categoryId);

    List<Product> findByIsActiveTrue();

    Page<Product> findByIsActiveTrue(Pageable pageable);

    List<Product> findByIsFeaturedTrueAndIsActiveTrue();

    @Query("SELECT p FROM Product p WHERE p.isActive = true " +
           "AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Product> searchProducts(@Param("search") String search);

    @Query("SELECT p FROM Product p WHERE p.isActive = true " +
           "AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Product> searchProducts(@Param("search") String search, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.categoryId = :categoryId " +
           "AND p.price BETWEEN :minPrice AND :maxPrice " +
           "AND p.isActive = true")
    List<Product> findByCategoryAndPriceRange(@Param("categoryId") Long categoryId,
                                                @Param("minPrice") java.math.BigDecimal minPrice,
                                                @Param("maxPrice") java.math.BigDecimal maxPrice);
}
