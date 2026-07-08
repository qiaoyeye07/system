package com.fleamarket.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class HandleReportRequest {

    @NotBlank(message = "处理结果不能为空")
    private String action; // ACCEPTED, REJECTED

    @NotBlank(message = "处理备注不能为空")
    @Size(min = 1, max = 500, message = "处理备注最长 500 个字符")
    private String adminNote;
}
