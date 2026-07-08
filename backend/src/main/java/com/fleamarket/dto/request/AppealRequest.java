package com.fleamarket.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AppealRequest {

    @NotBlank(message = "申诉理由不能为空")
    @Size(min = 1, max = 500, message = "申诉理由最长 500 个字符")
    private String appealReason;
}
