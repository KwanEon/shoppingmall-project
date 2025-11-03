package com.example.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.example.project.model.Product.Category;

import jakarta.validation.constraints.NotNull;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Min;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {
    private Long id;

    @NotBlank(message = "상품 이름은 필수 입력 항목입니다.")
    @Size(min = 1, max = 100)
    private String name;

    @Size(min = 1, max = 255, message = "이미지 URL은 1자 이상 255자 이하이어야 합니다.")
    private String imageUrl;

    private String description;

    @NotNull(message = "상품 가격은 필수 입력 항목입니다.")
    @Min(value = 0, message = "상품 가격은 0 이상이어야 합니다.")
    private int price;

    @NotNull(message = "재고 수량은 필수 입력 항목입니다.")
    @Min(value = 0, message = "재고 수량은 0 이상이어야 합니다.")
    private int stock;

    @NotNull(message = "카테고리는 필수 입력 항목입니다.")
    @Enumerated(EnumType.STRING)
    private Category category;
}
