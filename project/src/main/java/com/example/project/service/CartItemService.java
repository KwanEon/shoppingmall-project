package com.example.project.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import com.example.project.dto.CartItemDTO;
import com.example.project.model.CartItem;
import com.example.project.model.Product;
import com.example.project.model.User;
import com.example.project.repository.CartItemRepository;
import com.example.project.repository.ProductRepository;
import com.example.project.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CartItemService {
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public void addCartItem(Long userId, Long productId, int quantity) {    // 장바구니 추가
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));
        Product product = productRepository.findById(productId).orElseThrow(() -> new IllegalArgumentException("해당 상품을 찾을 수 없습니다."));
        CartItem cartItem = cartItemRepository.findByUserIdAndProductId(userId, productId);

        int current = (cartItem == null) ? 0 : cartItem.getQuantity();
        if (current + quantity > 10) {
            throw new IllegalArgumentException("장바구니 수량 제한을 초과합니다. 최대 10개까지 가능합니다.");
        }

        if (cartItem != null) {
            cartItem.setQuantity(cartItem.getQuantity() + quantity);
        }
        else {
            cartItem = CartItem.builder()
                .product(product)
                .user(user)
                .quantity(quantity)
                .build();
        }
        cartItemRepository.save(cartItem);
    }

    @Transactional(readOnly = true)
    public List<CartItem> getCartItemsByUserId(Long userId) {   // 장바구니 조회
        return cartItemRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<CartItemDTO> getCartItemDTOsByUserId(Long userId) {  // 장바구니 DTO 반환
        List<CartItemDTO> cartItems = cartItemRepository.findDTOByUserId(userId);
        return cartItems;
    }

    public void updateCartItem(Long id, int change) {   // 장바구니 수량 변경
        CartItem cartItem = findById(id);
        int newQuantity = cartItem.getQuantity() + change;
        if (newQuantity < 1) {
            throw new IllegalArgumentException("수량은 1 이상이어야 합니다.");
        } else if (newQuantity > 10) {
            throw new IllegalArgumentException("수량은 10 이하여야 합니다.");
        }
        cartItem.setQuantity(newQuantity);
        cartItemRepository.save(cartItem);
    }

    public void deleteAllCartItemsByUserId(Long userid) {   // 장바구니 비우기
        cartItemRepository.deleteByUserId(userid);
    }

    public void deleteCartItemByProductId(Long userId, Long productId) {    // 장바구니에서 특정 상품 삭제
        CartItem cartItem = cartItemRepository.findByUserIdAndProductId(userId, productId);
        if (cartItem == null) {
            throw new IllegalArgumentException("장바구니에 해당 상품이 없습니다.");
        }
        cartItemRepository.delete(cartItem);
    }

    public CartItem findById(Long id) {  // 장바구니 아이템 찾기
        return cartItemRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("장바구니가 비어있습니다."));
    }
}
