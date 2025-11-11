package com.example.project.dto;

import com.example.project.model.Product.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductListDTO {
    private Long id;
    private String name;
    private String imageUrl;
    private Category category;
    private int price;
    private int stock;
    private Double averageRating;
    private Long reviewCount;

    public ProductListDTO(Long id, String name, String imageUrl, int price, int stock, Category category, Number averageRating, Long reviewCount) {
        this.id = id;
        this.name = name;
        this.imageUrl = imageUrl;
        this.price = price;
        this.stock = stock;
        this.category = category;
        this.averageRating = averageRating == null ? 0.0 : averageRating.doubleValue();
        this.reviewCount = reviewCount;
    }
}
