package com.fleamarket.controller;

import com.fleamarket.dto.request.SendMessageRequest;
import com.fleamarket.dto.response.ApiResponse;
import com.fleamarket.dto.response.ContactResponse;
import com.fleamarket.dto.response.MessageResponse;
import com.fleamarket.security.SecurityUtils;
import com.fleamarket.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ChatController {

    private final ChatService chatService;

    @Value("${app.upload.path:./uploads}")
    private String uploadPath;

    @PostMapping("/chat/upload-image")
    public ApiResponse<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty() || file.getSize() > 5 * 1024 * 1024) {
            return ApiResponse.error(com.fleamarket.exception.ErrorCode.BAD_REQUEST, "图片大小不能超过 5MB");
        }
        String ext = ".jpg";
        String original = file.getOriginalFilename();
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf(".")).toLowerCase();
        }
        String name = UUID.randomUUID().toString().substring(0, 8) + ext;
        Path dir = Paths.get(uploadPath, "chat").toAbsolutePath().normalize();
        File dirFile = dir.toFile();
        if (!dirFile.exists()) dirFile.mkdirs();
        file.transferTo(new File(dirFile, name));
        return ApiResponse.success(Map.of("url", "/uploads/chat/" + name));
    }

    @PostMapping("/chat/send")
    public ApiResponse<MessageResponse> sendMessage(@Valid @RequestBody SendMessageRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(chatService.sendMessage(
                userId,
                request.getReceiverId(),
                request.getProductId(),
                request.getContent(),
                request.getMessageType(),
                request.getAttachmentUrl()));
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
