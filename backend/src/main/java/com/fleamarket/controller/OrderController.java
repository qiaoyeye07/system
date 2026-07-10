package com.fleamarket.controller;

import com.fleamarket.domain.enums.OrderType;
import com.fleamarket.dto.request.*;
import com.fleamarket.dto.response.ApiResponse;
import com.fleamarket.dto.response.OrderLogResponse;
import com.fleamarket.dto.response.OrderResponse;
import com.fleamarket.exception.BusinessException;
import com.fleamarket.exception.ErrorCode;
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
    public ApiResponse<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        OrderResponse order = orderService.createCashOrder(userId, request.getProductId());
        return ApiResponse.success("下单成功，请尽快完成付款", order);
    }

    @PostMapping("/orders/{id}/pay")
    public ApiResponse<OrderResponse> pay(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        OrderResponse order = orderService.pay(userId, id);
        return ApiResponse.success("付款成功，等待卖家发货", order);
    }

    @PostMapping("/orders/{id}/ship")
    public ApiResponse<OrderResponse> ship(@PathVariable Long id,
                                           @Valid @RequestBody ShipRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        OrderResponse order = orderService.ship(userId, id, request.getLogisticsInfo());
        return ApiResponse.success("发货成功，等待买家收货", order);
    }

    @PostMapping("/orders/{id}/receive")
    public ApiResponse<OrderResponse> receive(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        OrderResponse order = orderService.receive(userId, id);
        return ApiResponse.success("收货成功，交易即将完成", order);
    }

    @PostMapping("/orders/{id}/complete")
    public ApiResponse<OrderResponse> complete(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        OrderResponse order = orderService.completeOrder(userId, id);
        return ApiResponse.success("交易已完成", order);
    }

    @PostMapping("/orders/{id}/cancel")
    public ApiResponse<OrderResponse> cancelByBuyer(@PathVariable Long id,
                                                     @Valid @RequestBody CancelOrderRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        OrderResponse order = orderService.cancelByBuyer(userId, id, request.getReason());
        return ApiResponse.success("取消操作已提交", order);
    }

    @PostMapping("/orders/{id}/seller-cancel")
    public ApiResponse<OrderResponse> sellerCancel(@PathVariable Long id,
                                                    @Valid @RequestBody CancelOrderRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        OrderResponse order = orderService.sellerCancel(userId, id, request.getReason());
        return ApiResponse.success("订单已取消", order);
    }

    @PostMapping("/orders/{id}/agree-cancel")
    public ApiResponse<OrderResponse> sellerAgreeCancel(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        OrderResponse order = orderService.sellerAgreeCancel(userId, id);
        return ApiResponse.success("已同意取消，订单已取消", order);
    }

    @PostMapping("/orders/{id}/reject-cancel")
    public ApiResponse<OrderResponse> rejectOrWithdrawCancel(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        OrderResponse order = orderService.rejectOrWithdrawCancel(userId, id);
        return ApiResponse.success("操作成功", order);
    }

    @PostMapping("/orders/{id}/escalate-cancel")
    public ApiResponse<OrderResponse> escalateCancel(@PathVariable Long id,
                                                      @Valid @RequestBody RefundRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        OrderResponse order = orderService.escalateCancelToDispute(userId, id, request.getReason());
        return ApiResponse.success("已申请管理员介入", order);
    }

    @PostMapping("/orders/{id}/refund")
    public ApiResponse<OrderResponse> requestRefund(@PathVariable Long id,
                                                     @Valid @RequestBody RefundRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        OrderResponse order = orderService.requestRefund(userId, id, request.getReason());
        return ApiResponse.success("退款申请已提交", order);
    }

    @PostMapping("/orders/{id}/refund/agree")
    public ApiResponse<OrderResponse> sellerAgreeRefund(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        OrderResponse order = orderService.sellerAgreeRefund(userId, id);
        return ApiResponse.success("已同意退款，订单已取消", order);
    }

    @PostMapping("/orders/{id}/refund/reject")
    public ApiResponse<OrderResponse> sellerRejectRefund(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        OrderResponse order = orderService.sellerRejectRefund(userId, id);
        return ApiResponse.success("已拒绝退款，等待买家处理", order);
    }

    @PostMapping("/orders/{id}/escalate")
    public ApiResponse<OrderResponse> escalateToDispute(@PathVariable Long id,
                                                         @Valid @RequestBody RefundRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        OrderResponse order = orderService.escalateToDispute(userId, id, request.getReason());
        return ApiResponse.success("已申请管理员介入", order);
    }

    @PostMapping("/orders/{id}/cancel-refund")
    public ApiResponse<OrderResponse> cancelRefund(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        OrderResponse order = orderService.cancelRefund(userId, id);
        return ApiResponse.success("已取消退款申请", order);
    }

    @DeleteMapping("/orders/{id}")
    public ApiResponse<Void> deleteOrder(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        orderService.deleteOrder(userId, id);
        return ApiResponse.success("订单已删除", null);
    }

    // ==================== 以物易物 ====================

    @PostMapping("/swap")
    public ApiResponse<OrderResponse> createSwap(@Valid @RequestBody CreateSwapRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        OrderResponse order = orderService.createSwapOrder(userId, request.getProductId(), request);
        return ApiResponse.success("交换提议已发起，等待卖家确认", order);
    }

    @PostMapping("/swap/{id}/agree")
    public ApiResponse<OrderResponse> agreeSwap(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        OrderResponse order = orderService.agreeSwap(userId, id);
        return ApiResponse.success("已同意交换，双方商品已下架，请尽快发货", order);
    }

    @PostMapping("/swap/{id}/reject")
    public ApiResponse<OrderResponse> rejectSwap(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        OrderResponse order = orderService.rejectSwap(userId, id);
        return ApiResponse.success("已拒绝交换提议", order);
    }

    @PostMapping("/swap/{id}/withdraw")
    public ApiResponse<OrderResponse> withdrawSwap(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        OrderResponse order = orderService.withdrawSwap(userId, id);
        return ApiResponse.success("已撤回交换提议", order);
    }

    @PostMapping("/swap/{id}/cancel")
    public ApiResponse<OrderResponse> cancelSwap(@PathVariable Long id,
                                                  @Valid @RequestBody CancelOrderRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        orderService.cancelSwapOrder(userId, id, request.getReason());
        return ApiResponse.success("交换已取消", orderService.getOrderDetail(userId, id));
    }

    @PostMapping("/swap/{id}/ship")
    public ApiResponse<OrderResponse> swapShip(@PathVariable Long id,
                                                @Valid @RequestBody ShipRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        OrderResponse order = orderService.swapShip(userId, id, request.getLogisticsInfo());
        return ApiResponse.success("发货成功", order);
    }

    @PostMapping("/swap/{id}/receive")
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

    @GetMapping({"/my/orders", "/orders"})
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

    // ==================== 交换查询 ====================

    @GetMapping("/swap")
    public ApiResponse<Page<OrderResponse>> listMySwaps(
            @RequestParam(defaultValue = "buy") String role,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Long userId = SecurityUtils.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);

        Page<OrderResponse> orders;
        if ("sell".equals(role)) {
            orders = orderService.listSellOrders(userId, OrderType.SWAP, status, pageable);
        } else {
            orders = orderService.listBuyOrders(userId, OrderType.SWAP, status, pageable);
        }
        return ApiResponse.success(orders);
    }

    @GetMapping("/swap/{id}")
    public ApiResponse<OrderResponse> getSwapDetail(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(orderService.getOrderDetail(userId, id));
    }

    @DeleteMapping("/swap/{id}")
    public ApiResponse<Void> deleteSwap(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        orderService.deleteOrder(userId, id);
        return ApiResponse.success("交换订单已删除", null);
    }
}
