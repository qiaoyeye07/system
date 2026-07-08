package com.fleamarket.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateRatingRequest {

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分范围为 1-5 星")
    @Max(value = 5, message = "评分范围为 1-5 星")
    private Integer score;
}
