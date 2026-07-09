package com.fleamarket.service;

import com.fleamarket.domain.Message;
import com.fleamarket.domain.Product;
import com.fleamarket.domain.User;
import com.fleamarket.dto.response.ContactResponse;
import com.fleamarket.dto.response.MessageResponse;
import com.fleamarket.exception.BusinessException;
import com.fleamarket.exception.ErrorCode;
import com.fleamarket.repository.MessageRepository;
import com.fleamarket.repository.ProductRepository;
import com.fleamarket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public MessageResponse sendMessage(Long senderId, Long receiverId, Long productId, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "消息内容不能为空");
        }

        if (senderId.equals(receiverId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不能给自己发消息");
        }

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "接收方不存在"));

        if (!receiver.getEnabled()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "接收方账号不可用");
        }

        Product product = null;
        if (productId != null) {
            product = productRepository.findById(productId).orElse(null);
        }

        Message message = Message.builder()
                .sender(sender)
                .receiver(receiver)
                .product(product)
                .content(content.trim())
                .isRead(false)
                .build();

        message = messageRepository.save(message);
        MessageResponse response = toResponse(message);
        messagingTemplate.convertAndSend("/queue/chat/" + receiverId, response);
        return response;
    }

    @Transactional
    public Page<MessageResponse> getConversation(Long userId, Long contactId, Long productId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        // 标记对方发给我的消息为已读
        Page<Message> messages = messageRepository.findConversation(userId, contactId, productId, pageable);
        // 批量标记已读
        if (!messages.isEmpty()) {
            List<Message> unreadMessages = messages.getContent().stream()
                    .filter(m -> m.getReceiver().getId().equals(userId) && !m.getIsRead())
                    .collect(Collectors.toList());
            if (!unreadMessages.isEmpty()) {
                unreadMessages.forEach(m -> m.setIsRead(true));
                messageRepository.saveAll(unreadMessages);
            }
        }

        return messages.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<ContactResponse> getContacts(Long userId) {
        List<Message> lastMessages = messageRepository.findLastMessagesByUser(userId);

        Map<String, ContactResponse> contactMap = new LinkedHashMap<>();

        for (Message msg : lastMessages) {
            Long contactId = msg.getSender().getId().equals(userId)
                    ? msg.getReceiver().getId() : msg.getSender().getId();
            String contactName = msg.getSender().getId().equals(userId)
                    ? msg.getReceiver().getUsername() : msg.getSender().getUsername();

            String key = contactId + "_" + (msg.getProduct() != null ? msg.getProduct().getId() : 0);

            if (!contactMap.containsKey(key)) {
                long unreadCount = countUnread(userId, contactId, msg.getProduct() != null ? msg.getProduct().getId() : null);

                boolean isMine = msg.getSender().getId().equals(userId);
                contactMap.put(key, ContactResponse.builder()
                        .contactId(contactId)
                        .contactName(contactName)
                        .productId(msg.getProduct() != null ? msg.getProduct().getId() : null)
                        .productTitle(msg.getProduct() != null ? msg.getProduct().getTitle() : null)
                        .lastMessage(msg.getContent().length() > 50
                                ? msg.getContent().substring(0, 50) + "..." : msg.getContent())
                        .lastMessageTime(msg.getCreatedAt())
                        .lastMessageIsRead(msg.getIsRead())
                        .lastMessageIsMine(isMine)
                        .unreadCount(unreadCount)
                        .build());
            }
        }

        return new ArrayList<>(contactMap.values());
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return messageRepository.countByReceiverIdAndIsReadFalse(userId);
    }

    @Transactional
    public int markConversationAsRead(Long userId, Long contactId, Long productId) {
        return messageRepository.markConversationAsRead(userId, contactId, productId);
    }

    @Transactional
    public int deleteConversation(Long userId, Long contactId, Long productId) {
        return messageRepository.deleteConversationMessages(userId, contactId, productId);
    }

    private long countUnread(Long userId, Long contactId, Long productId) {
        return messageRepository.countUnreadByContact(userId, contactId, productId);
    }

    private MessageResponse toResponse(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getUsername())
                .receiverId(message.getReceiver().getId())
                .receiverName(message.getReceiver().getUsername())
                .productId(message.getProduct() != null ? message.getProduct().getId() : null)
                .productTitle(message.getProduct() != null ? message.getProduct().getTitle() : null)
                .content(message.getContent())
                .isRead(message.getIsRead())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
