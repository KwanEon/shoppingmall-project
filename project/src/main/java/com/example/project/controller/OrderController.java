package com.example.project.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import com.example.project.dto.CartItemDTO;
import com.example.project.dto.OrderDTO;
import com.example.project.dto.KakaoPayReadyResponseDTO;
import com.example.project.dto.KakaoPayApproveResponseDTO;
import org.springframework.web.bind.annotation.*;
import com.example.project.security.CustomUserDetails;
import com.example.project.service.CartItemService;
import com.example.project.service.OrderService;
import com.example.project.model.Order;
import java.util.List;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;
    private final CartItemService cartItemService;

    @GetMapping("/cartitem")    // 장바구니 아이템 목록 조회
    public ResponseEntity<?> showCartItems(@AuthenticationPrincipal CustomUserDetails principal) {
        List<CartItemDTO> cartItems = cartItemService.getCartItemDTOsByUserId(principal.getUserId());
        if (cartItems.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());  // 장바구니가 비어있을 경우 빈 리스트 반환
        }
        return ResponseEntity.ok(cartItems);
    }

    @PostMapping("/cartitem/{productId}")   // 장바구니에 아이템 추가
    public ResponseEntity<?> addCartItem(@AuthenticationPrincipal CustomUserDetails principal,
                                         @PathVariable("productId") Long productId,
                                         @RequestParam("quantity") int quantity) {
        if (principal == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        if (quantity < 1) {
            return ResponseEntity.badRequest().body("주문 수량은 최소 1개부터 가능합니다.");
        } else if (quantity > 10) {
            return ResponseEntity.badRequest().body("주문 수량은 최대 10개까지만 가능합니다.");
        }

        try {
            cartItemService.addCartItem(principal.getUserId(), productId, quantity);
            return ResponseEntity.status(HttpStatus.CREATED).body("상품이 장바구니에 추가되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/cartitem/{cartId}") // 장바구니 아이템 수량 변경
    public ResponseEntity<?> updateCartItemQuantity(@AuthenticationPrincipal CustomUserDetails principal,
                                                    @PathVariable("cartId") Long cartId,
                                                    @RequestParam("operation") String operation) {
        if (cartItemService.findById(cartId).getUser().getId() != principal.getUserId()) {
            return ResponseEntity.status(403).body("권한이 없습니다. 해당 장바구니 상품을 수정할 수 없습니다.");
        }
        if (!operation.equals("increase") && !operation.equals("decrease")) {   // 수량 변경 작업이 'increase' 또는 'decrease'가 아닐 경우
            return ResponseEntity.badRequest().body("잘못된 요청입니다. 'increase' 또는 'decrease'만 허용됩니다.");
        }
        int change = operation.equals("increase") ? 1 : -1; // 수량 변경 값 설정
        cartItemService.updateCartItem(cartId, change);
        return ResponseEntity.ok("장바구니 상품 수량이 업데이트되었습니다.");
    }

    @DeleteMapping("/cartitem")   // 장바구니 전체 삭제
    public ResponseEntity<?> deleteCartItem(@AuthenticationPrincipal CustomUserDetails principal) {
        cartItemService.deleteAllCartItemsByUserId(principal.getUserId());
        return ResponseEntity.ok("장바구니가 비워졌습니다.");
    }

    @DeleteMapping("/cartitem/{productId}") // 장바구니에서 특정 아이템 삭제
    public ResponseEntity<?> deleteCartItemByProductId(@AuthenticationPrincipal CustomUserDetails principal,
                                                       @PathVariable("productId") Long productId) {
        cartItemService.deleteCartItemByProductId(principal.getUserId(), productId);
        return ResponseEntity.ok("상품이 장바구니에서 삭제되었습니다.");
    }

    @PostMapping("/order/cartitem") // 장바구니 아이템으로 주문 생성 및 카카오페이 결제 준비
    public ResponseEntity<?> placeCartOrder(@AuthenticationPrincipal CustomUserDetails principal) {
        try {
            Order order = orderService.createPendingOrderFromCart(principal.getUserId());
            KakaoPayReadyResponseDTO responseDTO = orderService.KakaoPayReadyFromCart(principal.getUserId(), order);

            Map<String, Object> result = new HashMap<>();
            result.put("kakaoPayResponse", responseDTO);
            result.put("orderId", order.getId());       // 결제 상태 확인을 위한 주문 ID 추가

            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    @PostMapping("/payment/success/cart")  // 카카오페이 결제 승인 및 완료(장바구니 기반)
    public ResponseEntity<?> KakaoPaySuccessFromCart(@RequestParam("orderId") Long orderId,
                                                     @RequestParam("pg_token") String pgToken) {                                        
        try {
            KakaoPayApproveResponseDTO responseDTO = orderService.KakaoPayApproveFromCart(orderId, pgToken);
            return ResponseEntity.ok(responseDTO);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    @PostMapping("/order/{productId}")  // 개별 상품 주문 생성 및 카카오페이 결제 준비
    public ResponseEntity<?> placeItemOrder(@AuthenticationPrincipal CustomUserDetails principal,
                                            @PathVariable("productId") Long productId,
                                            @RequestParam("quantity") int quantity) {
        try {
            if (quantity < 1) {
                return ResponseEntity.badRequest().body("주문 수량은 최소 1개부터 가능합니다.");
            } else if (quantity > 10) {
                return ResponseEntity.badRequest().body("주문 수량은 최대 10개까지만 가능합니다.");
            }
            Order order = orderService.createPendingOrder(principal.getUserId(), productId, quantity);
            KakaoPayReadyResponseDTO responseDTO = orderService.KakaoPayReady(principal.getUserId(), order, quantity);

            Map<String, Object> result = new HashMap<>();
            result.put("kakaoPayResponse", responseDTO);
            result.put("orderId", order.getId());       // 결제 상태 확인을 위한 주문 ID 추가

            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @PostMapping("/payment/success")  // 카카오페이 결제 승인 및 완료
    public ResponseEntity<?> KakaoPaySuccess(@RequestParam("orderId") Long orderId,
                                             @RequestParam("pg_token") String pgToken) {
        try {
            KakaoPayApproveResponseDTO responseDTO = orderService.KakaoPayApprove(orderId, pgToken);
            return ResponseEntity.ok(responseDTO);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @PostMapping("/payment/cancel")   // 카카오페이 결제 취소
    public ResponseEntity<?> KakaoPayCancel(@RequestParam("orderId") Long orderId) {
        orderService.KakaoPayApproveCancel(orderId);
        return ResponseEntity.ok("주문이 취소되었습니다.");
    }

    @GetMapping("/auth/orders/all")     // 현재 로그인한 유저의 주문 목록 전체 조회
    public ResponseEntity<?> getCurrentUserAllOrders(@AuthenticationPrincipal CustomUserDetails principal) {
        List<OrderDTO> orders = orderService.getOrderDTOsByUserId(principal.getUserId());
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/auth/orders")   // 현재 로그인한 유저의 주문 목록 조회 (페이징)
    public ResponseEntity<?> getCurrentUserOrders(@AuthenticationPrincipal CustomUserDetails principal,
                                                  @RequestParam(value = "page", defaultValue = "0") int pageable) {
        PageRequest pageRequest = PageRequest.of(pageable, 5);
        Page<OrderDTO> orders = orderService.getOrdersByUserId(principal.getUserId(), pageRequest);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/order/status/{orderId}") // 주문 상태 조회
    public ResponseEntity<?> getOrderStatus(@AuthenticationPrincipal CustomUserDetails principal,
                                            @PathVariable("orderId") Long orderId) {
        Order order = orderService.getOrderById(orderId);
        if (order == null || !order.getUser().getId().equals(principal.getUserId())) {
            return ResponseEntity.status(403).body("접근 권한이 없습니다.");
        }

        Map<String, Object> statusInfo = new HashMap<>();
        statusInfo.put("status", order.getStatus().name());  // 예: "PAID", "PENDING" 등

        return ResponseEntity.ok(statusInfo);
    }
}
