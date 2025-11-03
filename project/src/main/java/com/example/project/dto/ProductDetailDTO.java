package com.example.project.dto;

import com.example.project.model.Product.Category;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;

@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailDTO {
    private Long id;
    private String name;
    private String imageUrl;
    private String description;
    private int price;
    private int stock;
    private Category category;

    private Page<ReviewResponseDTO> reviews;
}
