package com.fleamarket.controller;

import com.fleamarket.domain.enums.OrderType;
import com.fleamarket.dto.request.*;
import com.fleamarket.dto.response.ApiResponse;
import com.fleamarket.dto.response.OrderLogResponse;
import com.fleamarket.dto.response.OrderResponse;
import com.fleamarket.security.SecurityUtils;
import com.fleamarket.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class OrderController {

    private final OrderService orderService;

    // ==================== 现金订单 ====================

    @PostMapping("/orders")
    public ApiResponse<OrderResponse> create(@RequestParam Long productId) {
        Long userId = SecurityUtils.getCurrentUserId();
        OrderResponse order = orderService.createCashOrder(userId, productId);
        return ApiResponse.success("下单成功，请尽快完成付款", order);
    }

    @PutMapping("/orders/{id}/pay")
    public ApiResponse<OrderResponse> pay(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        OrderResponse order = orderService.pay(userId, id);
        return ApiResponse.success("付款成功，等待卖家发货", order);
    }

    @PutMapping("/orders/{id}/ship")
    public ApiResponse<OrderResponse> ship(@PathVariable Long id,
                                            @Valid @RequestBody ShipRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        OrderResponse order = orderService.ship(userId, id, request.getLogisticsInfo());
        return ApiResponse.success("发货成功，等待买家收货", order);
    }

    @PutMapping("/orders/{id}/receive")
    public ApiResponse<OrderResponse> receive(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        OrderResponse order = orderService.receive(userId, id);
        return ApiResponse.success("收货成功，交易即将完成", order);
    }

    @PutMapping("/orders/{id}/cancel")
    public ApiResponse<OrderResponse> cancelByBuyer(@PathVariable Long id,
                                                      @Valid @RequestBody CancelOrderRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        OrderResponse order = orderService.cancelByBuyer(userId, id, request.getReason());
        return ApiResponse.success("取消操作已提交", order);
    }

    @PutMapping("/orders/{id}/seller-cancel")
    public ApiResponse<OrderResponse> sellerCancel(@PathVariable Long id,
                                                     @Valid @RequestBody CancelOrderRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        OrderResponse order = orderService.sellerCancel(userId, id, request.getReason());
        return ApiResponse.success("订单已取消", order);
    }

    @PutMapping("/orders/{id}/agree-cancel")
    public ApiResponse<OrderResponse> sellerAgreeCancel(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        OrderResponse order = orderService.sellerAgreeCancel(userId, id);
        return ApiResponse.success("已同意取消，订单已取消", order);
    }

    @PutMapping("/orders/{id}/refund")
    public ApiResponse<OrderResponse> requestRefund(@PathVariable Long id,
                                                      @Valid @RequestBody RefundRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        OrderResponse order = orderService.requestRefund(userId, id, request.getReason());
        return ApiResponse.success("退款申请已提交", order);
    }

    @PutMapping("/orders/{id}/agree-refund")
    public ApiResponse<OrderResponse> sellerAgreeRefund(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        OrderResponse order = orderService.sellerAgreeRefund(userId, id);
        return ApiResponse.success("已同意退款，订单已取消", order);
    }

    @PutMapping("/orders/{id}/reject-refund")
    public ApiResponse<OrderResponse> sellerRejectRefund(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        OrderResponse order = orderService.sellerRejectRefund(userId, id);
        return ApiResponse.success("已拒绝退款，等待管理员介入", order);
    }

    // ==================== 以物易物 ====================

    @PostMapping("/swap-orders")
    public ApiResponse<OrderResponse> createSwap(@RequestParam Long productId,
                                                   @Valid @RequestBody CreateSwapRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        OrderResponse order = orderService.createSwapOrder(userId, productId, request);
        return ApiResponse.success("交换提议已发起，等待卖家确认", order);
    }

    @PutMapping("/swap-orders/{id}/agree")
    public ApiResponse<OrderResponse> agreeSwap(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        OrderResponse order = orderService.agreeSwap(userId, id);
        return ApiResponse.success("已同意交换，双方商品已下架，请尽快发货", order);
    }

    @PutMapping("/swap-orders/{id}/reject")
    public ApiResponse<OrderResponse> rejectSwap(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        OrderResponse order = orderService.rejectSwap(userId, id);
        return ApiResponse.success("已拒绝交换提议", order);
    }

    @PutMapping("/swap-orders/{id}/ship")
    public ApiResponse<OrderResponse> swapShip(@PathVariable Long id,
                                                 @Valid @RequestBody ShipRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        OrderResponse order = orderService.swapShip(userId, id, request.getLogisticsInfo());
        return ApiResponse.success("发货成功", order);
    }

    @PutMapping("/swap-orders/{id}/receive")
    public ApiResponse<OrderResponse> swapReceive(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        OrderResponse order = orderService.swapReceive(userId, id);
        return ApiResponse.success("收货成功", order);
    }

    // ==================== 订单查询 ====================

    @GetMapping("/orders/{id}")
    public ApiResponse<OrderResponse> getDetail(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(orderService.getOrderDetail(userId, id));
    }

    @GetMapping("/orders/{id}/logs")
    public ApiResponse<List<OrderLogResponse>> getLogs(@PathVariable Long id) {
        return ApiResponse.success(orderService.getOrderLogs(id));
    }

    @GetMapping("/my/orders")
    public ApiResponse<Page<OrderResponse>> listMyOrders(
            @RequestParam(defaultValue = "CASH") String orderType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "buy") String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Long userId = SecurityUtils.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);
        OrderType type = OrderType.valueOf(orderType.toUpperCase());

        Page<OrderResponse> orders;
        if ("sell".equals(role)) {
            orders = orderService.listSellOrders(userId, type, status, pageable);
        } else {
            orders = orderService.listBuyOrders(userId, type, status, pageable);
        }
        return ApiResponse.success(orders);
    }
}
