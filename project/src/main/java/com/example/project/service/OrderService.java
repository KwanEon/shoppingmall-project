package com.example.project.service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;

import com.example.project.dto.KakaoPayReadyRequestDTO;
import com.example.project.dto.OrderDTO;
import com.example.project.dto.OrderItemDTO;
import com.example.project.dto.KakaoPayReadyResponseDTO;
import com.example.project.dto.KakaoPayApproveRequestDTO;
import com.example.project.dto.KakaoPayApproveResponseDTO;
import com.example.project.model.CartItem;
import com.example.project.model.Order;
import com.example.project.model.OrderItem;
import com.example.project.model.Product;
import com.example.project.model.Review;
import com.example.project.model.User;
import com.example.project.model.Order.OrderStatus;
import com.example.project.repository.OrderItemRepository;
import com.example.project.repository.OrderRepository;
import com.example.project.repository.ProductRepository;
import com.example.project.repository.ReviewRepository;
import com.example.project.repository.UserRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderService {
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final OrderItemService orderItemService;
    private final OrderItemRepository orderItemRepository;
    private final CartItemService cartItemService;
    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;

    @Value("${kakaopay.secret-key}")
    private String kakaoSecretKey;

    @Transactional(readOnly = true)
    public Order getOrderById(Long orderId) {   // 주문 상세 조회
        return orderRepository.findById(orderId).orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다"));
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> getOrderDTOsByUserId(Long userId) {   // 사용자별 주문 목록 전체 조회
        Map<Long, Long> myReviewMap = reviewRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(r -> r.getProduct().getId(), Review::getId));

        List<OrderDTO> orderDTOs = orderRepository.findByUserId(userId).stream()
                .map((Order order) -> OrderDTO.builder()
                        .id(order.getId())
                        .userId(order.getUser().getId())
                        .orderDate(order.getOrderDate().toString())
                        .status(order.getStatus().name())
                        .totalPrice(order.getTotalPrice())
                        .orderItems(order.getOrderItems().stream()
                            .map(orderItem -> OrderItemDTO.from(
                                orderItem,
                                myReviewMap.getOrDefault(orderItem.getProduct().getId(), null)
                            ))
                            .toList())
                        .build())
                .toList();
        return orderDTOs;
    }

    @Transactional(readOnly = true)
    public Page<OrderDTO> getOrdersByUserId(Long userId, Pageable pageable) {   // 사용자별 주문 목록 조회 (페이징)
        Map<Long, Long> myReviewMap = reviewRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(r -> r.getProduct().getId(), Review::getId));

        Page<Long> idPage = orderRepository.findOrderIdsByUserId(userId, pageable);
        List<Long> ids = idPage.getContent();

        if (ids.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, idPage.getTotalElements());
        }

        List<Order> orders = orderRepository.findOrdersWithItemsByIds(ids);

        Map<Long, Order> orderMap = orders.stream()
                .collect(Collectors.toMap(Order::getId, Function.identity()));

        List<OrderDTO> result = new ArrayList<>();
        for (Long oid : ids) {
            Order o = orderMap.get(oid);

            OrderDTO odto = OrderDTO.builder()
                    .id(o.getId())
                    .userId(o.getUser().getId())
                    .orderDate(o.getOrderDate().toString())
                    .status(o.getStatus().name())
                    .totalPrice(o.getTotalPrice())
                    .build();

            for (OrderItem oi : o.getOrderItems()) {
                OrderItemDTO it = OrderItemDTO.from(oi, myReviewMap.getOrDefault(oi.getProduct().getId(), null));
                odto.getOrderItems().add(it);
            }
            result.add(odto);
        }

        return new PageImpl<>(result, pageable, idPage.getTotalElements());
    }

    public Order createPendingOrderFromCart(Long userId) {      // 결제 전 주문 엔티티 생성(장바구니 기반)
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다"));
        List<CartItem> cartItems = cartItemService.getCartItemsByUserId(userId);

        if (cartItems.isEmpty()) {
            throw new IllegalStateException("장바구니가 비어 있습니다. 주문할 항목이 없습니다.");
        }

        Order order = Order.builder()
                .status(Order.OrderStatus.PENDING)
                .address(user.getAddress())
                .build();

        user.addOrder(order); // 양방향 연관관계 설정

        int totalPrice = 0;

        // 각 장바구니 항목을 주문 항목으로 변환
        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
            int quantity = cartItem.getQuantity();
            double itemPrice = product.getPrice() * quantity;

            if (product.getStock() < quantity) {
                throw new IllegalStateException("재고가 부족합니다. 현재 재고: " + product.getStock());
            }

            totalPrice += itemPrice;
            
            OrderItem item = orderItemService.createOrderItem(order, product, quantity);
            order.addOrderItem(item);   // 양방향 연관관계 설정
        }

        // 총 금액 설정
        order.setTotalPrice(totalPrice);
        orderRepository.save(order);

        return order;
    }

    public Order createPendingOrder(Long userId, Long productId, int quantity) {  // 결제 전 주문 엔티티 생성(단건 주문)
        if (quantity <= 0) {
            throw new IllegalArgumentException("수량은 0보다 커야 합니다.");
        }

        // 사용자 확인
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다"));

        // 상품 확인
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다"));

        // 재고 확인
        if (product.getStock() < quantity) {
            throw new IllegalStateException("재고가 부족합니다. 현재 재고: " + product.getStock());
        }

        // 총 금액 계산
        int totalPrice = product.getPrice() * quantity;

        // 주문 생성
        Order order = Order.builder()
            .status(OrderStatus.PENDING)
            .address(user.getAddress())
            .totalPrice(totalPrice)
            .build();

        user.addOrder(order); // 양방향 연관관계 설정

        // 주문 항목 생성
        OrderItem item = orderItemService.createOrderItem(order, product, quantity);
        order.addOrderItem(item);   // 양방향 연관관계 설정

        orderRepository.save(order);

        return order;
    }

    public void cancelOrder(Long orderId) {     // 주문 취소
        Order order = getOrderById(orderId);
        if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new IllegalStateException("이미 배송된 주문은 취소할 수 없습니다.");
        }
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        // 주문 항목의 재고 복구
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        for (OrderItem orderItem : orderItems) {
            Product product = orderItem.getProduct();
            product.setStock(product.getStock() + orderItem.getQuantity()); // 재고 복구
            productRepository.save(product);
        }
    }

    public KakaoPayReadyResponseDTO KakaoPayReady(Long userId, Order order, int quantity) {   // 카카오페이 결제 준비
        KakaoPayReadyRequestDTO kakaoPayRequest = KakaoPayReadyRequestDTO.builder()
                                .cid("TC0ONETIME")
                                .partner_order_id(order.getId().toString())
                                .partner_user_id(userId.toString())
                                .item_name(order.getOrderItems().get(0).getProduct().getName())
                                .quantity(quantity)
                                .total_amount(order.getTotalPrice())
                                .tax_free_amount(0)
                                .approval_url("http://localhost:3000/payment/success?orderId=" + order.getId())
                                .cancel_url("http://localhost:8080/payment/cancel?orderId=" + order.getId())
                                .fail_url("http://localhost:3000/payment/fail")
                                .build();

        HttpHeaders headers = new HttpHeaders();
        RestTemplate restTemplate = new RestTemplate();

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "SECRET_KEY " + kakaoSecretKey);

        HttpEntity<KakaoPayReadyRequestDTO> requestEntity = new HttpEntity<>(kakaoPayRequest, headers);

        try {
            // 카카오페이 결제 준비 API 호출
            ResponseEntity<KakaoPayReadyResponseDTO> response = restTemplate.postForEntity(
                    "https://open-api.kakaopay.com/online/v1/payment/ready",
                    requestEntity,
                    KakaoPayReadyResponseDTO.class
            );
            KakaoPayReadyResponseDTO responseDTO = response.getBody();
            if (response.getStatusCode().is2xxSuccessful() && responseDTO != null) {
                order.setTid(responseDTO.getTid());
                orderRepository.save(order);

                return responseDTO;
            } else {
                throw new IllegalStateException("결제 준비에 실패했습니다.");
            }
        } catch (HttpClientErrorException e) {      // 카카오페이 API 호출 실패 시
            throw new IllegalStateException("결제 준비 중 오류가 발생했습니다: " + e.getResponseBodyAsString());
        } catch (Exception e) {                     // 기타 예외 처리
            throw new IllegalStateException("결제 준비 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    public KakaoPayApproveResponseDTO KakaoPayApprove(Long orderId, String pgToken) {   // 카카오페이 결제 승인
        Order order = getOrderById(orderId);

        if (order.getStatus() == OrderStatus.PAID) {
            throw new IllegalStateException("이미 결제 완료된 주문입니다.");
        }

        KakaoPayApproveRequestDTO kakaoPayApproveRequest = KakaoPayApproveRequestDTO.builder()
                                        .cid("TC0ONETIME")
                                        .tid(order.getTid())
                                        .partner_order_id(order.getId().toString())
                                        .partner_user_id(order.getUser().getId().toString())
                                        .pg_token(pgToken)
                                        .build();

        HttpHeaders headers = new HttpHeaders();
        RestTemplate restTemplate = new RestTemplate();

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "SECRET_KEY " + kakaoSecretKey);

        HttpEntity<KakaoPayApproveRequestDTO> requestEntity = new HttpEntity<>(kakaoPayApproveRequest, headers);

        try {
            // 카카오페이 결제 승인 API 호출
            ResponseEntity<KakaoPayApproveResponseDTO> response = restTemplate.postForEntity(
                    "https://open-api.kakaopay.com/online/v1/payment/approve",
                    requestEntity,
                    KakaoPayApproveResponseDTO.class
            );
            KakaoPayApproveResponseDTO result = response.getBody();
            if (response.getStatusCode().is2xxSuccessful() && result != null) {
                order.setStatus(OrderStatus.PAID);  // 결제 완료로 상태 변경
                orderRepository.save(order);

                // 재고 차감
                Product product = productRepository.findByIdWithLock(order.getOrderItems().get(0).getProduct().getId())
                        .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다"));
                product.setStock(product.getStock() - order.getOrderItems().get(0).getQuantity());
                productRepository.save(product);
            }
            return result;
        } catch (HttpClientErrorException e) {      // 카카오페이 API 호출 실패 시
            throw new IllegalStateException("결제 승인 중 오류가 발생했습니다: " + e.getResponseBodyAsString());
        } catch (Exception e) {                     // 기타 예외 처리
            throw new IllegalStateException("결제 승인 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    public KakaoPayReadyResponseDTO KakaoPayReadyFromCart(Long userId, Order order) {   // 카카오페이 결제 준비(장바구니 기반)
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getId());
        String itemName = orderItems.get(0).getProduct().getName();
        if (orderItems.size() > 1) {
            itemName += " 외 " + (orderItems.size() - 1) + "건";
        }
        KakaoPayReadyRequestDTO kakaoPayRequest = KakaoPayReadyRequestDTO.builder()
                                .cid("TC0ONETIME")
                                .partner_order_id(order.getId().toString())
                                .partner_user_id(userId.toString())
                                .item_name(itemName)
                                .quantity(order.getOrderItems().stream().mapToInt(item -> item.getQuantity()).sum())
                                .total_amount(order.getTotalPrice())
                                .tax_free_amount(0)
                                .approval_url("http://localhost:3000/payment/success/cart?orderId=" + order.getId())
                                .cancel_url("http://localhost:8080/payment/cancel?orderId=" + order.getId())
                                .fail_url("http://localhost:3000/payment/fail")
                                .build();

        HttpHeaders headers = new HttpHeaders();
        RestTemplate restTemplate = new RestTemplate();

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "SECRET_KEY " + kakaoSecretKey);

        HttpEntity<KakaoPayReadyRequestDTO> requestEntity = new HttpEntity<>(kakaoPayRequest, headers);

        try {
            ResponseEntity<KakaoPayReadyResponseDTO> response = restTemplate.postForEntity(
                    "https://open-api.kakaopay.com/online/v1/payment/ready",
                    requestEntity,
                    KakaoPayReadyResponseDTO.class
            );
            KakaoPayReadyResponseDTO responseDTO = response.getBody();
            if (response.getStatusCode().is2xxSuccessful() && responseDTO != null) {
                order.setTid(responseDTO.getTid());
                orderRepository.save(order);

                return responseDTO;
            } else {
                throw new IllegalStateException("결제 준비에 실패했습니다.");
            }
        } catch (HttpClientErrorException e) {      // 카카오페이 API 호출 실패 시
            throw new IllegalStateException("결제 준비 중 오류가 발생했습니다: " + e.getResponseBodyAsString());
        } catch (Exception e) {                     // 기타 예외 처리
            throw new IllegalStateException("결제 준비 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    public KakaoPayApproveResponseDTO KakaoPayApproveFromCart(Long orderId, String pgToken) {   // 카카오페이 결제 승인(장바구니 기반)
        Order order = getOrderById(orderId);

        if (order.getStatus() == OrderStatus.PAID) {
            throw new IllegalStateException("이미 결제 완료된 주문입니다.");
        }

        KakaoPayApproveRequestDTO kakaoPayApproveRequest = KakaoPayApproveRequestDTO.builder()
                                        .cid("TC0ONETIME")
                                        .tid(order.getTid())
                                        .partner_order_id(order.getId().toString())
                                        .partner_user_id(order.getUser().getId().toString())
                                        .pg_token(pgToken)
                                        .build();

        HttpHeaders headers = new HttpHeaders();
        RestTemplate restTemplate = new RestTemplate();

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "SECRET_KEY " + kakaoSecretKey);

        HttpEntity<KakaoPayApproveRequestDTO> requestEntity = new HttpEntity<>(kakaoPayApproveRequest, headers);

        try {
            ResponseEntity<KakaoPayApproveResponseDTO> response = restTemplate.postForEntity(
                    "https://open-api.kakaopay.com/online/v1/payment/approve",
                    requestEntity,
                    KakaoPayApproveResponseDTO.class
            );
            KakaoPayApproveResponseDTO result = response.getBody();
            if (response.getStatusCode().is2xxSuccessful() && result != null) {
                order.setStatus(OrderStatus.PAID);  // 결제 완료로 상태 변경
                orderRepository.save(order);

                // 재고 차감
                for (OrderItem orderItem : order.getOrderItems()) {
                    Product product = productRepository.findByIdWithLock(orderItem.getProduct().getId())
                            .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다"));
                    product.setStock(product.getStock() - orderItem.getQuantity());
                    productRepository.save(product);
                }

                // 장바구니 비우기
                cartItemService.deleteAllCartItemsByUserId(order.getUser().getId());
            }
            return result;
        } catch (HttpClientErrorException e) {      // 카카오페이 API 호출 실패 시
            throw new IllegalStateException("결제 승인 중 오류가 발생했습니다: " + e.getResponseBodyAsString());
        } catch (Exception e) {                     // 기타 예외 처리
            throw new IllegalStateException("결제 승인 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    public void KakaoPayApproveCancel(Long orderId) {   // 카카오페이 결제 취소
        Order order = getOrderById(orderId);
        orderRepository.delete(order);
    }
}
