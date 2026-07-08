package com.fleamarket.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ShipRequest {

    @NotBlank(message = "物流信息不能为空")
    @Size(min = 1, max = 500, message = "物流信息最长 500 个字符")
    private String logisticsInfo;
}
