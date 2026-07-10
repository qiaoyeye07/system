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

    private static final String EVENT_MESSAGE_NEW = "MESSAGE_NEW";
    private static final String EVENT_MESSAGE_READ = "MESSAGE_READ";

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public MessageResponse sendMessage(Long senderId, Long receiverId, Long productId, String content,
                                       String messageType, String attachmentUrl) {
        String type = (messageType != null && !messageType.isEmpty()) ? messageType : "TEXT";
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
                .messageType(type)
                .attachmentUrl(attachmentUrl)
                .isRead(false)
                .build();

        message = messageRepository.save(message);
        MessageResponse response = toResponse(message);
        messagingTemplate.convertAndSend("/queue/chat/" + receiverId, Map.of(
                "type", EVENT_MESSAGE_NEW,
                "data", response
        ));
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
                notifyReadReceipt(userId, contactId, productId, unreadMessages);
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
        List<Message> unreadMessages = messageRepository.findUnreadConversationMessages(userId, contactId, productId);
        if (unreadMessages.isEmpty()) {
            return 0;
        }
        unreadMessages.forEach(m -> m.setIsRead(true));
        messageRepository.saveAll(unreadMessages);
        notifyReadReceipt(userId, contactId, productId, unreadMessages);
        return unreadMessages.size();
    }

    @Transactional
    public int deleteConversation(Long userId, Long contactId, Long productId) {
        return messageRepository.deleteConversationMessages(userId, contactId, productId);
    }

    private long countUnread(Long userId, Long contactId, Long productId) {
        return messageRepository.countUnreadByContact(userId, contactId, productId);
    }

    private void notifyReadReceipt(Long readerId, Long senderId, Long productId, List<Message> readMessages) {
        List<Long> messageIds = readMessages.stream()
                .map(Message::getId)
                .collect(Collectors.toList());
        messagingTemplate.convertAndSend("/queue/chat/" + senderId, Map.of(
                "type", EVENT_MESSAGE_READ,
                "data", Map.of(
                        "readerId", readerId,
                        "senderId", senderId,
                        "productId", productId == null ? 0 : productId,
                        "messageIds", messageIds,
                        "readCount", messageIds.size()
                )
        ));
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
                .messageType(message.getMessageType())
                .attachmentUrl(message.getAttachmentUrl())
                .isRead(message.getIsRead())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
