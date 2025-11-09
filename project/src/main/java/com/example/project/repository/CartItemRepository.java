package com.example.project.repository;

import org.springframework.stereotype.Repository;

import com.example.project.model.CartItem;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByUserId(Long userId);

    CartItem findByUserIdAndProductId(Long userId, Long productId);
    
    void deleteByUserId(Long userId);
}
