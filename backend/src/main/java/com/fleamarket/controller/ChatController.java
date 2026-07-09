package com.fleamarket.controller;

import com.fleamarket.dto.request.SendMessageRequest;
import com.fleamarket.dto.response.ApiResponse;
import com.fleamarket.dto.response.ContactResponse;
import com.fleamarket.dto.response.MessageResponse;
import com.fleamarket.security.SecurityUtils;
import com.fleamarket.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/chat/send")
    public ApiResponse<MessageResponse> sendMessage(@Valid @RequestBody SendMessageRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(chatService.sendMessage(
                userId,
                request.getReceiverId(),
                request.getProductId(),
                request.getContent()));
    }

    @GetMapping("/chat/conversation/{contactId}")
    public ApiResponse<Page<MessageResponse>> getConversation(
            @PathVariable Long contactId,
            @RequestParam(required = false) Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(chatService.getConversation(userId, contactId, productId, page, size));
    }

    @GetMapping("/chat/contacts")
    public ApiResponse<List<ContactResponse>> getContacts() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(chatService.getContacts(userId));
    }

    @GetMapping("/chat/unread-count")
    public ApiResponse<Long> getUnreadCount() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(chatService.getUnreadCount(userId));
    }

    @PatchMapping("/chat/conversation/{contactId}/read")
    public ApiResponse<Integer> markConversationAsRead(@PathVariable Long contactId,
                                                       @RequestParam(required = false) Long productId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(chatService.markConversationAsRead(userId, contactId, productId));
    }

    @DeleteMapping("/chat/conversation/{contactId}")
    public ApiResponse<Integer> deleteConversation(@PathVariable Long contactId,
                                                   @RequestParam(required = false) Long productId) {
        Long userId = SecurityUtils.getCurrentUserId();
        int deleted = chatService.deleteConversation(userId, contactId, productId);
        return ApiResponse.success("已删除 " + deleted + " 条对话记录", deleted);
    }
}
