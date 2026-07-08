package com.fleamarket.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DisputeRequest {

    @NotBlank(message = "裁定结果不能为空")
    private String action; // APPROVE_REFUND, MAINTAIN_STATUS

    @NotBlank(message = "裁定理由不能为空")
    @Size(min = 1, max = 500, message = "裁定理由最长 500 个字符")
    private String reason;
}
