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
public class ReportResponse {

    private Long id;
    private Long reporterId;
    private String reporterName;
    private String targetType;
    private Long targetId;
    private String targetSummary;
    private String reason;
    private String description;
    private String status;
    private String adminNote;
    private String appealReason;
    private String appealResult;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
