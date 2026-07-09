package com.fleamarket.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PublishProductRequest {

    @NotBlank(message = "标题不能为空")
    @Size(min = 1, max = 100, message = "标题长度为 1-100 个字符")
    private String title;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须为正数")
    @DecimalMax(value = "999999.99", message = "价格最大为 999999.99")
    private BigDecimal price;

    @NotBlank(message = "描述不能为空")
    @Size(min = 1, max = 4000, message = "描述长度为 1-4000 个字符")
    private String description;

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    private String tags;

    @NotBlank(message = "成色不能为空")
    private String condition; // NEW, LIKE_NEW, USED

    @NotBlank(message = "交易方式不能为空")
    private String tradeType; // PICKUP, EXPRESS, BOTH

    @NotBlank(message = "交易模式不能为空")
    private String tradeMode; // SELL, SWAP, BOTH

    @NotBlank(message = "所在地不能为空")
    @Size(min = 1, max = 50, message = "所在地长度为 1-50 个字符")
    private String location;

    private String images;
}
