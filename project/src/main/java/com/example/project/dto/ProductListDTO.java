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
    private Category category;
    private int price;
    private int stock;
}
