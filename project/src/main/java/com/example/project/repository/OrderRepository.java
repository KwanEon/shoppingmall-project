package com.example.project.repository;

import org.springframework.stereotype.Repository;
import com.example.project.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    @EntityGraph(attributePaths = {"orderItems", "orderItems.product"})
    List<Order> findByUserId(Long userId);

    @Query("SELECT o.id FROM Order o WHERE o.user.id = :userId AND o.status = 'PAID' ORDER BY o.orderDate DESC")
    Page<Long> findOrderIdsByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT o FROM Order o " +
           "JOIN FETCH o.orderItems oi " +
           "JOIN FETCH oi.product p " +
           "WHERE o.id IN :ids")
    List<Order> findOrdersWithItemsByIds(@Param("ids") List<Long> ids);
}
