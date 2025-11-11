package com.example.project.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PopularProductDTO {
    private Long id;
    private String name;
    private String imageUrl;
    private String category;
    private int price;
    private Long sales30d;
    private Double averageRating;
    private Long reviewCount;
}
