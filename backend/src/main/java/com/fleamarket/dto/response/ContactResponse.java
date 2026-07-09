package com.fleamarket.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactResponse {

    private Long contactId;
    private String contactName;
    private Long productId;
    private String productTitle;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private Boolean lastMessageIsRead;
    private Boolean lastMessageIsMine;
    private Long unreadCount;
}
