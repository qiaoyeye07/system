package com.fleamarket.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderLogResponse {

    private Long id;
    private Long operatorId;
    private String operatorName;
    private String actionType;
    private String oldStatus;
    private String newStatus;
    private String detail;
    private LocalDateTime createdAt;
}
