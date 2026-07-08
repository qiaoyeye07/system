package com.fleamarket.controller;

import com.fleamarket.dto.response.ApiResponse;
import com.fleamarket.dto.response.ContactResponse;
import com.fleamarket.dto.response.MessageResponse;
import com.fleamarket.security.SecurityUtils;
import com.fleamarket.service.ChatService;
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
    public ApiResponse<MessageResponse> sendMessage(
            @RequestParam Long receiverId,
            @RequestParam(required = false) Long productId,
            @RequestParam String content) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(chatService.sendMessage(userId, receiverId, productId, content));
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
}
