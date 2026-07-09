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
public class OrderResponse {

    private Long id;
    private String orderNo;
    private String orderType;
    private Long productId;
    private String productTitle;
    private String productImage;
    private BigDecimal productPrice;
    private Long swapProductId;
    private String swapProductTitle;
    private String swapProductImage;
    private Long buyerId;
    private String buyerName;
    private Long sellerId;
    private String sellerName;
    private String counterpartyName;
    private String status;
    private BigDecimal amount;
    private String logisticsInfo;
    private String cancelReason;
    private String refundReason;
    private String swapNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
