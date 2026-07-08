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
public class UserResponse {

    private Long id;
    private String username;
    private String role;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private Double avgScore;
    private Long ratingCount;
    private Long activeProductCount;
}
