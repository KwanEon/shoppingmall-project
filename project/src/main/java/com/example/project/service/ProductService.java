package com.example.project.service;

import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.example.project.dto.PopularProductDTO;
import com.example.project.dto.ProductDTO;
import com.example.project.dto.ProductListDTO;
import com.example.project.dto.ProductDetailDTO;
import com.example.project.dto.ReviewResponseDTO;
import com.example.project.model.Product;
import com.example.project.model.Product.Category;
import com.example.project.model.Review;
import com.example.project.repository.CartItemRepository;
import com.example.project.repository.ProductRepository;
import com.example.project.repository.ReviewRepository;

import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.io.IOException;
import java.nio.file.*;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {
    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;
    private final CartItemRepository cartItemRepository;
    
    @Value("${app.upload.dir}")
    private String uploadDir;

    @Transactional(readOnly = true)
    public Product getProductById(Long id) {    // 상품 조회
        return productRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public Page<ProductListDTO> findAllProducts(Category category, String keyword, Pageable pageable) {    // 모든 상품 조회(페이징)
        return productRepository.findAllProducts(category, keyword, pageable);
    }
    
    @Transactional(readOnly = true)
    public ProductDetailDTO getProductDetail(Long id, Pageable pageable) {   // 상품 상세 조회
        Product product = productRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
        Page<Long> reviewIdsPage = reviewRepository.findReviewIdsByProductId(id, pageable);     // 리뷰 ID 페이징 조회

        List<ReviewResponseDTO> reviewsDTO = new ArrayList<>();
        if (!reviewIdsPage.isEmpty()) {
            List<Review> reviews = reviewRepository.findReviewsByIds(reviewIdsPage.getContent());   // 리뷰 ID로 리뷰 조회(Fetch Join)

            reviewsDTO = reviews.stream()
                    .map(ReviewResponseDTO::from)   // Review -> ReviewResponseDTO 변환
                    .collect(Collectors.toList());  // List<ReviewResponseDTO> 생성
        }

        Page<ReviewResponseDTO> reviewsPage = new PageImpl<>(reviewsDTO, pageable, reviewIdsPage.getTotalElements());   // 리뷰 DTO 페이징 생성

        return ProductDetailDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .imageUrl(product.getImageUrl())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .category(product.getCategory())
                .reviews(reviewsPage)
                .build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void addProduct(ProductDTO productDTO, MultipartFile image) throws IOException {   // 상품 추가
        Path imagesDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(imagesDir);

        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            String original = StringUtils.cleanPath(image.getOriginalFilename());
            String ext = "";
            int dot = original.lastIndexOf('.');
            if (dot >= 0) ext = original.substring(dot);

            String filename = UUID.randomUUID().toString() + ext;
            Path destination = imagesDir.resolve(filename);

            image.transferTo(destination.toFile());
            imageUrl = "/images/" + filename;
        }

        Product product = Product.builder()
                .name(productDTO.getName())
                .imageUrl(imageUrl)
                .description(productDTO.getDescription())
                .price(productDTO.getPrice())
                .stock(productDTO.getStock())
                .category(productDTO.getCategory())
                .build();
        productRepository.save(product);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void updateProduct(Long id, ProductDTO productDTO, MultipartFile image) throws IOException {  // 상품 수정
        Product product = getProductById(id);
        product.setName(productDTO.getName());
        product.setDescription(productDTO.getDescription());
        product.setPrice(productDTO.getPrice());
        product.setStock(productDTO.getStock());
        product.setCategory(productDTO.getCategory());

        Path imagesDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(imagesDir);

        if (image != null && !image.isEmpty()) {
            // 기존 이미지 삭제
            if (product.getImageUrl() != null) {
                Path oldImage = Paths.get(product.getImageUrl().substring(1));
                Files.deleteIfExists(oldImage);
            }

            String original = StringUtils.cleanPath(image.getOriginalFilename());
            String ext = "";
            int dot = original.lastIndexOf('.');
            if (dot >= 0) ext = original.substring(dot);

            String filename = UUID.randomUUID().toString() + ext;
            Path destination = imagesDir.resolve(filename);
            image.transferTo(destination.toFile());
            product.setImageUrl("/images/" + filename);
        }
        productRepository.save(product);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void deleteProduct(Long id) throws IOException {    // 상품 삭제
        Product product = getProductById(id);
        cartItemRepository.deleteByProductId(id);

        // 이미지 삭제
        if (product.getImageUrl() != null) {
            Path imagePath = Paths.get(product.getImageUrl().substring(1));
            Files.deleteIfExists(imagePath);
        }

        productRepository.delete(product);
    }

    @Transactional(readOnly = true)
    public List<PopularProductDTO> getTop3PopularProducts() {  // 가장 많이 팔린 top3 상품 조회
        List<PopularProductDTO> popularProducts = productRepository.findTop3ByOrderTimes();
        return popularProducts;
    }
}
