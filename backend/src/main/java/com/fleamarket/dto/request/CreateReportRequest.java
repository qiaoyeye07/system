package com.fleamarket.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateReportRequest {

    @NotBlank(message = "举报对象类型不能为空")
    private String targetType; // PRODUCT, USER, MESSAGE

    @NotNull(message = "举报对象 ID 不能为空")
    private Long targetId;

    @NotBlank(message = "举报原因不能为空")
    private String reason; // FAKE_DESC, PROHIBITED, FRAUD, HARASS, OTHER

    @Size(max = 500, message = "补充描述最长 500 个字符")
    private String description;
}
