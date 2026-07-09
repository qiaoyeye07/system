package com.fleamarket.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SendMessageRequest {

    @NotNull(message = "接收方不能为空")
    private Long receiverId;

    private Long productId;

    @NotBlank(message = "消息内容不能为空")
    @Size(min = 1, max = 2000, message = "消息内容最长 2000 个字符")
    private String content;

    private String messageType;
    private String attachmentUrl;
}
