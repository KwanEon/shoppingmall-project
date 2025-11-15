package com.example.project.repository;

import org.springframework.stereotype.Repository;

import com.example.project.model.OrderItem;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrderId(Long orderId);
}
