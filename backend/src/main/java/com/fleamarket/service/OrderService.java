package com.fleamarket.service;

import com.fleamarket.domain.*;
import com.fleamarket.domain.enums.*;
import com.fleamarket.dto.request.CreateSwapRequest;
import com.fleamarket.dto.response.OrderLogResponse;
import com.fleamarket.dto.response.OrderResponse;
import com.fleamarket.exception.BusinessException;
import com.fleamarket.exception.ErrorCode;
import com.fleamarket.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderLogRepository orderLogRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    // ==================== 现金订单 ====================

    @Transactional
    public OrderResponse createCashOrder(Long buyerId, Long productId) {
        Product product = productRepository.findById(productId)
                .filter(p -> p.getStatus() == ProductStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_ACTIVE));

        if (product.getSeller().getId().equals(buyerId)) {
            throw new BusinessException(ErrorCode.CANNOT_BUY_OWN_PRODUCT);
        }
        if (product.getTradeMode() == TradeMode.SWAP) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该商品仅支持交换");
        }

        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        // 商品标记为已售
        product.setStatus(ProductStatus.SOLD);
        productRepository.save(product);

        String orderNo = generateOrderNo("ORD");
        String status = "PENDING_PAY";

        Order order = Order.builder()
                .orderNo(orderNo)
                .orderType(OrderType.CASH)
                .product(product)
                .buyer(buyer)
                .seller(product.getSeller())
                .status(status)
                .amount(product.getPrice())
                .build();

        order = orderRepository.save(order);
        createLog(order, buyer, ActionType.CREATE, null, status, "买家下单");

        return toResponse(order);
    }

    @Transactional
    public OrderResponse pay(Long buyerId, Long orderId) {
        Order order = getOrder(orderId, buyerId, true);
        validateStatus(order, "PENDING_PAY", "付款");

        order.setStatus("PAID");
        orderRepository.save(order);
        createLog(order, order.getBuyer(), ActionType.PAY, "PENDING_PAY", "PAID", "买家模拟付款");

        return toResponse(order);
    }

    @Transactional
    public OrderResponse ship(Long sellerId, Long orderId, String logisticsInfo) {
        Order order = getOrder(orderId, sellerId, false);
        validateStatus(order, "PAID", "发货");

        order.setStatus("SHIPPED");
        order.setLogisticsInfo(logisticsInfo);
        orderRepository.save(order);
        createLog(order, order.getSeller(), ActionType.SHIP, "PAID", "SHIPPED", "卖家发货：" + logisticsInfo);

        return toResponse(order);
    }

    @Transactional
    public OrderResponse receive(Long buyerId, Long orderId) {
        Order order = getOrder(orderId, buyerId, true);
        validateStatus(order, "SHIPPED", "收货");

        order.setStatus("RECEIVED");
        orderRepository.save(order);
        createLog(order, order.getBuyer(), ActionType.RECEIVE, "SHIPPED", "RECEIVED", "买家确认收货");

        return toResponse(order);
    }

    @Transactional
    public OrderResponse completeOrder(Long buyerId, Long orderId) {
        Order order = getOrder(orderId, buyerId, true);
        validateStatus(order, "RECEIVED", "确认完成");

        order.setStatus("COMPLETED");
        orderRepository.save(order);
        createLog(order, order.getBuyer(), ActionType.RECEIVE, "RECEIVED", "COMPLETED",
                "买家确认完成，交易结束");

        return toResponse(order);
    }

    @Transactional
    public OrderResponse cancelByBuyer(Long buyerId, Long orderId, String reason) {
        Order order = getOrder(orderId, buyerId, true);

        if ("PENDING_PAY".equals(order.getStatus())) {
            return cancelOrder(order, order.getBuyer(), reason);
        }
        throw new BusinessException(ErrorCode.CANNOT_CANCEL_SHIPPED);
    }

    @Transactional
    public OrderResponse sellerCancel(Long sellerId, Long orderId, String reason) {
        Order order = getOrder(orderId, sellerId, false);
        validateStatus(order, "PAID", "取消");

        return cancelOrder(order, order.getSeller(), reason);
    }

    @Transactional
    public OrderResponse sellerAgreeCancel(Long sellerId, Long orderId) {
        Order order = getOrder(orderId, sellerId, false);
        if (!"PAID".equals(order.getStatus()) || order.getCancelReason() == null) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION, "当前没有待处理的取消申请");
        }

        return cancelOrder(order, order.getSeller(), order.getCancelReason());
    }

    @Transactional
    public OrderResponse rejectOrWithdrawCancel(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        boolean isBuyer = order.getBuyer().getId().equals(userId);
        boolean isSeller = order.getSeller().getId().equals(userId);
        if (!isBuyer && !isSeller) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        if (!"PAID".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION, "当前状态不支持该操作");
        }
        if (order.getCancelReason() == null) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION, "当前没有待处理的取消申请");
        }

        if (isBuyer) {
            // 买家撤销取消申请：清除原因
            order.setCancelReason(null);
            orderRepository.save(order);
            createLog(order, order.getBuyer(), ActionType.CANCEL, "PAID", "PAID", "买家撤销取消申请");
        } else {
            // 卖家拒绝取消：保留 cancelReason，买家可选择申诉
            orderRepository.save(order);
            createLog(order, order.getSeller(), ActionType.REJECT_REFUND, "PAID", "PAID", "卖家拒绝取消申请");
        }

        return toResponse(order);
    }

    @Transactional
    public OrderResponse escalateCancelToDispute(Long userId, Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        boolean isBuyer = order.getBuyer().getId().equals(userId);
        boolean isSeller = order.getSeller().getId().equals(userId);
        if (!isBuyer && !isSeller) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (!"PAID".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION, "当前状态不支持申请管理员介入");
        }

        String who = isBuyer ? "买家" : "卖家";
        order.setStatus("DISPUTE");
        order.setCancelReason(reason);
        orderRepository.save(order);
        createLog(order, isBuyer ? order.getBuyer() : order.getSeller(),
                ActionType.REQUEST_REFUND, "PAID", "DISPUTE",
                who + "就取消申请管理员介入：" + reason);

        return toResponse(order);
    }

    @Transactional
    public OrderResponse requestRefund(Long buyerId, Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getBuyer().getId().equals(buyerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        String status = order.getStatus();
        if (!"RECEIVED".equals(status) && !"SHIPPED".equals(status) && !"PAID".equals(status)) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION, "当前状态不支持申请退款");
        }

        order.setRefundReason(reason);
        orderRepository.save(order);
        createLog(order, order.getBuyer(), ActionType.REQUEST_REFUND, status, status,
                "买家申请退款：" + reason);

        return toResponse(order);
    }

    @Transactional
    public OrderResponse sellerAgreeRefund(Long sellerId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getSeller().getId().equals(sellerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (order.getRefundReason() == null) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION, "当前没有待处理的退款申请");
        }

        return cancelOrder(order, order.getSeller(), order.getRefundReason());
    }

    @Transactional
    public OrderResponse sellerRejectRefund(Long sellerId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getSeller().getId().equals(sellerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (order.getRefundReason() == null) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION, "当前没有待处理的退款申请");
        }

        orderRepository.save(order);
        createLog(order, order.getSeller(), ActionType.REJECT_REFUND, order.getStatus(), order.getStatus(),
                "卖家拒绝退款");

        return toResponse(order);
    }

    @Transactional
    public OrderResponse escalateToDispute(Long userId, Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        boolean isBuyer = order.getBuyer().getId().equals(userId);
        boolean isSeller = order.getSeller().getId().equals(userId);
        if (!isBuyer && !isSeller) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        String oldStatus = order.getStatus();
        String who = isBuyer ? "买家" : "卖家";
        order.setStatus("DISPUTE");
        order.setRefundReason(reason);
        orderRepository.save(order);
        createLog(order, isBuyer ? order.getBuyer() : order.getSeller(),
                ActionType.REQUEST_REFUND, oldStatus, "DISPUTE",
                who + "申请管理员介入：" + reason);

        return toResponse(order);
    }

    @Transactional
    public OrderResponse cancelRefund(Long buyerId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getBuyer().getId().equals(buyerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (order.getRefundReason() == null) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION, "当前没有进行中的退款申请");
        }

        String oldStatus = order.getStatus();
        order.setRefundReason(null);
        orderRepository.save(order);
        createLog(order, order.getBuyer(), ActionType.CANCEL, oldStatus, oldStatus,
                "买家取消退款申请");

        return toResponse(order);
    }

    @Transactional
    public void deleteOrder(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getBuyer().getId().equals(userId) && !order.getSeller().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 只允许删除已终止的订单
        String status = order.getStatus();
        if (!"CANCELLED".equals(status) && !"COMPLETED".equals(status) && !"REJECTED".equals(status)) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION, "只能删除已完成、已取消或已拒绝的订单");
        }

        orderLogRepository.deleteAll(orderLogRepository.findByOrderIdOrderByCreatedAtDesc(orderId));
        orderRepository.delete(order);
    }

    @Transactional
    public OrderResponse adminJudge(Long orderId, String action, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (!"DISPUTE".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION, "该订单不在纠纷状态");
        }

        if ("APPROVE_REFUND".equals(action)) {
            return cancelOrder(order, getSystemUser(), "管理员裁定：" + reason);
        } else {
            // 驳回纠纷：取消类纠纷回到 PAID，退款类纠纷回到 RECEIVED
            String returnStatus = order.getCancelReason() != null ? "PAID" : "RECEIVED";
            order.setStatus(returnStatus);
            order.setRefundReason(null);
            order.setCancelReason(null);
            orderRepository.save(order);
            createLog(order, getSystemUser(), ActionType.ADMIN_JUDGE, "DISPUTE", returnStatus,
                    "管理员裁定维持原状：" + reason);
            return toResponse(order);
        }
    }

    private User getSystemUser() {
        return userRepository.findById(1L).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "系统用户不存在"));
    }

    // ==================== 以物易物 ====================

    @Transactional
    public OrderResponse createSwapOrder(Long buyerId, Long productId, CreateSwapRequest request) {
        // 目标商品校验
        Product targetProduct = productRepository.findById(productId)
                .filter(p -> p.getStatus() == ProductStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_ACTIVE));

        if (targetProduct.getSeller().getId().equals(buyerId)) {
            throw new BusinessException(ErrorCode.CANNOT_BUY_OWN_PRODUCT);
        }
        if (targetProduct.getTradeMode() == TradeMode.SELL) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该商品仅支持出售，不可交换");
        }

        // 交换物校验
        Product swapProduct = productRepository.findById(request.getSwapProductId())
                .filter(p -> p.getStatus() == ProductStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_ACTIVE));

        if (!swapProduct.getSeller().getId().equals(buyerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "交换物品必须是您自己的在售商品");
        }

        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        String orderNo = generateOrderNo("SWP");

        Order order = Order.builder()
                .orderNo(orderNo)
                .orderType(OrderType.SWAP)
                .product(targetProduct)
                .swapProduct(swapProduct)
                .buyer(buyer)
                .seller(targetProduct.getSeller())
                .status("PENDING_CONFIRM")
                .amount(java.math.BigDecimal.ZERO)
                .swapNote(request.getSwapNote())
                .build();

        order = orderRepository.save(order);
        createLog(order, buyer, ActionType.CREATE, null, "PENDING_CONFIRM",
                "买家发起交换提议" + (request.getSwapNote() != null ? "：" + request.getSwapNote() : ""));

        return toResponse(order);
    }

    @Transactional
    public OrderResponse agreeSwap(Long sellerId, Long orderId) {
        Order order = getSwapOrder(orderId, sellerId, false);
        validateStatus(order, "PENDING_CONFIRM", "确认交换");

        // 双方商品下架
        Product targetProduct = order.getProduct();
        targetProduct.setStatus(ProductStatus.OFF);
        productRepository.save(targetProduct);

        Product swapProduct = order.getSwapProduct();
        if (swapProduct != null) {
            swapProduct.setStatus(ProductStatus.OFF);
            productRepository.save(swapProduct);
        }

        order.setStatus("CONFIRMED");
        orderRepository.save(order);
        createLog(order, order.getSeller(), ActionType.AGREE_SWAP, "PENDING_CONFIRM", "CONFIRMED",
                "卖家同意交换，双方商品已下架");

        return toResponse(order);
    }

    @Transactional
    public OrderResponse rejectSwap(Long sellerId, Long orderId) {
        Order order = getSwapOrder(orderId, sellerId, false);
        validateStatus(order, "PENDING_CONFIRM", "拒绝交换");

        order.setStatus("REJECTED");
        orderRepository.save(order);
        createLog(order, order.getSeller(), ActionType.REJECT_SWAP, "PENDING_CONFIRM", "REJECTED",
                "卖家拒绝交换");

        return toResponse(order);
    }

    @Transactional
    public OrderResponse withdrawSwap(Long buyerId, Long orderId) {
        Order order = getSwapOrder(orderId, buyerId, true);
        validateStatus(order, "PENDING_CONFIRM", "撤回交换");

        order.setStatus("REJECTED");
        orderRepository.save(order);
        createLog(order, order.getBuyer(), ActionType.REJECT_SWAP, "PENDING_CONFIRM", "REJECTED",
                "买家撤回交换提议");

        return toResponse(order);
    }

    @Transactional
    public void cancelSwapOrder(Long userId, Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getBuyer().getId().equals(userId) && !order.getSeller().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        String oldStatus = order.getStatus();
        order.setStatus("CANCELLED");
        orderRepository.save(order);

        // 恢复双方商品为在售
        if (order.getProduct() != null && order.getProduct().getStatus() == ProductStatus.OFF) {
            order.getProduct().setStatus(ProductStatus.ACTIVE);
            productRepository.save(order.getProduct());
        }
        if (order.getSwapProduct() != null && order.getSwapProduct().getStatus() == ProductStatus.OFF) {
            order.getSwapProduct().setStatus(ProductStatus.ACTIVE);
            productRepository.save(order.getSwapProduct());
        }

        createLog(order, null, ActionType.CANCEL, oldStatus, "CANCELLED",
                "交换取消：" + (reason != null ? reason : ""));
    }

    @Transactional
    public OrderResponse swapShip(Long userId, Long orderId, String logisticsInfo) {
        Order order = getSwapOrder(orderId, userId, null);
        validateStatus(order, "CONFIRMED", "发货");

        boolean isBuyer = order.getBuyer().getId().equals(userId);
        String detail = (isBuyer ? "买家(交换方)" : "卖家(交换方)") + "发货：" + logisticsInfo;

        // 检查双方是否都已发货
        List<OrderLog> shipLogs = orderLogRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
        boolean otherShipped = shipLogs.stream()
                .anyMatch(log -> log.getActionType() == ActionType.SHIP);

        if (otherShipped) {
            order.setStatus("BOTH_SHIPPED");
        }

        orderRepository.save(order);
        String newStatus = order.getStatus();
        createLog(order, isBuyer ? order.getBuyer() : order.getSeller(),
                ActionType.SHIP, "CONFIRMED", newStatus, detail);

        return toResponse(order);
    }

    @Transactional
    public OrderResponse swapReceive(Long userId, Long orderId) {
        Order order = getSwapOrder(orderId, userId, null);
        validateStatus(order, "BOTH_SHIPPED", "收货");

        boolean isBuyer = order.getBuyer().getId().equals(userId);
        String detail = (isBuyer ? "买家" : "卖家") + "确认收货";

        // 检查对方是否也已收货
        List<OrderLog> logs = orderLogRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
        boolean otherReceived = logs.stream()
                .anyMatch(log -> log.getActionType() == ActionType.RECEIVE);

        if (otherReceived) {
            order.setStatus("COMPLETED");
            detail += "，交换完成";
        }

        orderRepository.save(order);
        String newStatus = order.getStatus();
        createLog(order, isBuyer ? order.getBuyer() : order.getSeller(),
                ActionType.RECEIVE, "BOTH_SHIPPED", newStatus, detail);

        return toResponse(order);
    }

    // ==================== 通用查询 ====================

    public OrderResponse getOrderDetail(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getBuyer().getId().equals(userId) && !order.getSeller().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return toResponse(order);
    }

    public Page<OrderResponse> listBuyOrders(Long buyerId, OrderType orderType, String status, Pageable pageable) {
        if (status != null && !status.isEmpty()) {
            return orderRepository.findByBuyerIdAndOrderTypeAndStatus(buyerId, orderType, status, pageable)
                    .map(o -> toResponseWithCounterparty(o, false));
        }
        return orderRepository.findByBuyerIdAndOrderType(buyerId, orderType, pageable)
                .map(o -> toResponseWithCounterparty(o, false));
    }

    public Page<OrderResponse> listSellOrders(Long sellerId, OrderType orderType, String status, Pageable pageable) {
        if (status != null && !status.isEmpty()) {
            return orderRepository.findBySellerIdAndOrderTypeAndStatus(sellerId, orderType, status, pageable)
                    .map(o -> toResponseWithCounterparty(o, true));
        }
        return orderRepository.findBySellerIdAndOrderType(sellerId, orderType, pageable)
                .map(o -> toResponseWithCounterparty(o, true));
    }

    public List<OrderLogResponse> getOrderLogs(Long orderId) {
        return orderLogRepository.findByOrderIdOrderByCreatedAtDesc(orderId).stream()
                .map(log -> OrderLogResponse.builder()
                        .id(log.getId())
                        .operatorId(log.getOperator() != null ? log.getOperator().getId() : null)
                        .operatorName(log.getOperator() != null ? log.getOperator().getUsername() : "系统")
                        .actionType(log.getActionType().name())
                        .oldStatus(log.getOldStatus())
                        .newStatus(log.getNewStatus())
                        .detail(log.getDetail())
                        .createdAt(log.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    // ==================== 辅助方法 ====================

    private OrderResponse cancelOrder(Order order, User operator, String reason) {
        if ("COMPLETED".equals(order.getStatus()) || "CANCELLED".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_ENDED);
        }

        String oldStatus = order.getStatus();
        order.setStatus("CANCELLED");
        order.setCancelReason(reason);
        orderRepository.save(order);

        // 恢复商品为在售（现金订单）
        if (order.getOrderType() == OrderType.CASH && order.getProduct() != null) {
            Product product = order.getProduct();
            product.setStatus(ProductStatus.ACTIVE);
            productRepository.save(product);
        }

        createLog(order, operator, ActionType.CANCEL, oldStatus, "CANCELLED",
                "订单取消：" + (reason != null ? reason : ""));

        return toResponse(order);
    }

    private Order getOrder(Long orderId, Long userId, Boolean isBuyer) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getOrderType() != OrderType.CASH) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (isBuyer != null) {
            if (isBuyer && !order.getBuyer().getId().equals(userId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
            if (!isBuyer && !order.getSeller().getId().equals(userId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
        }
        return order;
    }

    private Order getSwapOrder(Long orderId, Long userId, Boolean isBuyer) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getOrderType() != OrderType.SWAP) {
            throw new BusinessException(ErrorCode.SWAP_ORDER_NOT_FOUND);
        }
        if (isBuyer != null) {
            if (isBuyer && !order.getBuyer().getId().equals(userId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
            if (!isBuyer && !order.getSeller().getId().equals(userId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
        } else {
            // 双方都可操作
            if (!order.getBuyer().getId().equals(userId) && !order.getSeller().getId().equals(userId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
        }
        return order;
    }

    private void validateStatus(Order order, String expectedStatus, String action) {
        if (!expectedStatus.equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "当前状态不支持" + action + "操作");
        }
    }

    private void createLog(Order order, User operator, ActionType actionType,
                           String oldStatus, String newStatus, String detail) {
        OrderLog log = OrderLog.builder()
                .order(order)
                .operator(operator)
                .actionType(actionType)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .detail(detail)
                .build();
        orderLogRepository.save(log);
    }

    private String generateOrderNo(String prefix) {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String seq = UUID.randomUUID().toString().replace("-", "").substring(0, 5).toUpperCase();
        return prefix + "-" + date + "-" + seq;
    }

    // ==================== 管理员查询 ====================

    public Page<OrderResponse> listOrdersByTypeAndStatus(OrderType orderType, String status, Pageable pageable) {
        if (status != null && !status.isEmpty()) {
            return orderRepository.findByOrderTypeAndStatus(orderType, status, pageable)
                    .map(this::toResponse);
        }
        // 如果没指定状态，需要查全部。用 findAll 效率不够好，这里用分页查每个状态再合并
        return orderRepository.findAll(pageable).map(this::toResponse);
    }

    public Page<OrderResponse> listDisputes(Pageable pageable) {
        return orderRepository.findByOrderTypeAndStatus(OrderType.CASH, "DISPUTE", pageable)
                .map(this::toResponse);
    }

    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }

    public OrderResponse toResponse(Order order) {
        String productImage = order.getProduct() != null && order.getProduct().getImages() != null
                ? order.getProduct().getImages().split(",")[0] : null;

        String swapProductTitle = null;
        String swapProductImage = null;
        Long swapProductId = null;
        if (order.getSwapProduct() != null) {
            swapProductId = order.getSwapProduct().getId();
            swapProductTitle = order.getSwapProduct().getTitle();
            swapProductImage = order.getSwapProduct().getImages() != null
                    ? order.getSwapProduct().getImages().split(",")[0] : null;
        }

        return OrderResponse.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .orderType(order.getOrderType().name())
                .productId(order.getProduct().getId())
                .productTitle(order.getProduct().getTitle())
                .productImage(productImage)
                .productPrice(order.getProduct().getPrice())
                .swapProductId(swapProductId)
                .swapProductTitle(swapProductTitle)
                .swapProductImage(swapProductImage)
                .buyerId(order.getBuyer().getId())
                .buyerName(order.getBuyer().getUsername())
                .sellerId(order.getSeller().getId())
                .sellerName(order.getSeller().getUsername())
                .status(order.getStatus())
                .amount(order.getAmount())
                .logisticsInfo(order.getLogisticsInfo())
                .cancelReason(order.getCancelReason())
                .refundReason(order.getRefundReason())
                .swapNote(order.getSwapNote())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private OrderResponse toResponseWithCounterparty(Order order, boolean isSellerView) {
        OrderResponse response = toResponse(order);
        response.setCounterpartyName(isSellerView
                ? order.getBuyer().getUsername()
                : order.getSeller().getUsername());
        return response;
    }
}
