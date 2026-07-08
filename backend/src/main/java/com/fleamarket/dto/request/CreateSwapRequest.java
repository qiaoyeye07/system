package com.fleamarket.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateSwapRequest {

    @NotNull(message = "请选择交换商品")
    private Long swapProductId;

    @Size(max = 500, message = "意向说明最长为 500 个字符")
    private String swapNote;
}
