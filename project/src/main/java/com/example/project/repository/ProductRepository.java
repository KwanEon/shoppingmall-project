package com.example.project.repository;

import org.springframework.stereotype.Repository;

import com.example.project.model.Product;
import com.example.project.model.Product.Category;
import com.example.project.dto.PopularProductDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithLock(@Param("id") Long id);
    Page<Product> findByNameContainingIgnoreCase(String keyword, Pageable pageable);
    Page<Product> findByCategory(Category category, Pageable pageable);
    Page<Product> findByCategoryAndNameContainingIgnoreCase(Category category, String name, Pageable pageable);
    @Query(value = "SELECT p.id, p.name, p.image_Url AS thumbnailUrl, p.price, COUNT(oi.id) AS sales30d " +
                   "FROM product p " +
                   "JOIN order_item oi ON p.id = oi.product_id " +
                   "JOIN orders o ON oi.order_id = o.id " +
                   "WHERE o.order_date >= DATE_SUB(NOW(), INTERVAL 30 DAY) " +
                   "GROUP BY p.id " +
                   "ORDER BY sales30d DESC " +
                   "LIMIT 3", nativeQuery = true)
    List<PopularProductDTO> findTop3ByOrderTimes();
}
