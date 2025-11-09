package com.example.project.repository;

import org.springframework.stereotype.Repository;
import com.example.project.model.Review;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    @Query("SELECT r.id FROM Review r WHERE r.product.id = :productId ORDER BY r.reviewDate DESC")
    Page<Long> findReviewIdsByProductId(@Param("productId") Long productId, Pageable pageable);

    @Query("SELECT r FROM Review r JOIN FETCH r.user WHERE r.id IN :reviewIds")
    List<Review> findReviewsByIds(@Param("reviewIds") List<Long> reviewIds);

    boolean existsByProductIdAndUserId(Long productId, Long userId);

    Optional<Review> findByUserIdAndProductId(Long userId, Long productId);
    
    List<Review> findByUserId(Long userId);
}
