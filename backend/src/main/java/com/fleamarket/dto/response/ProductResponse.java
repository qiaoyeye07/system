package com.fleamarket.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private Long id;
    private Long sellerId;
    private String sellerName;
    private Double sellerRating;
    private String title;
    private BigDecimal price;
    private String description;
    private Long categoryId;
    private String categoryName;
    private String tags;
    private String condition;
    private String tradeType;
    private String tradeMode;
    private String location;
    private String images;
    private String status;
    private LocalDateTime createdAt;
}
