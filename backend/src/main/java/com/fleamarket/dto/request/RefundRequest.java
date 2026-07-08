package com.fleamarket.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RefundRequest {

    @NotBlank(message = "退款原因不能为空")
    @Size(min = 1, max = 500, message = "退款原因最长 500 个字符")
    private String reason;
}
