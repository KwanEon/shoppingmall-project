package com.example.project.repository;

import org.springframework.stereotype.Repository;
import com.example.project.model.CartItem;
import com.example.project.dto.CartItemDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByUserId(Long userId);

    @Query(value= "SELECT new com.example.project.dto.CartItemDTO(" +
                  "c.id, p.id, p.name, p.imageUrl, p.price, c.quantity) " +
                  "FROM CartItem c " +
                  "JOIN c.product p " +
                  "WHERE c.user.id = :userId")
    List<CartItemDTO> findDTOByUserId(@Param("userId") Long userId);

    CartItem findByUserIdAndProductId(Long userId, Long productId);
    
    void deleteByUserId(Long userId);

    void deleteByProductId(Long productid);
}
