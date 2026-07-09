package com.fleamarket.controller;

import com.fleamarket.domain.enums.OrderType;
import com.fleamarket.dto.request.DisputeRequest;
import com.fleamarket.dto.request.HandleReportRequest;
import com.fleamarket.dto.response.ApiResponse;
import com.fleamarket.dto.response.OrderResponse;
import com.fleamarket.dto.response.ReportResponse;
import com.fleamarket.dto.response.UserResponse;
import com.fleamarket.service.AuthService;
import com.fleamarket.service.OrderService;
import com.fleamarket.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {

    private final AuthService authService;
    private final OrderService orderService;
    private final ReportService reportService;

    // ==================== 用户管理 ====================

    @GetMapping("/users")
    public ApiResponse<Page<UserResponse>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(authService.listUsers(pageable));
    }

    @PutMapping("/users/{id}/toggle-enabled")
    public ApiResponse<UserResponse> toggleUserEnabled(@PathVariable Long id,
                                                         @RequestParam boolean enabled) {
        UserResponse user = authService.toggleUserEnabled(id, enabled);
        String msg = enabled ? "用户已启用" : "用户已禁用";
        return ApiResponse.success(msg, user);
    }

    // ==================== 订单管理 ====================

    @GetMapping("/orders")
    public ApiResponse<Page<OrderResponse>> listAllOrders(
            @RequestParam(defaultValue = "CASH") String orderType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        OrderType type = OrderType.valueOf(orderType.toUpperCase());
        return ApiResponse.success(orderService.listOrdersByTypeAndStatus(type, status, pageable));
    }

    // ==================== 纠纷处理 ====================

    @GetMapping("/disputes")
    public ApiResponse<Page<OrderResponse>> listDisputes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(orderService.listDisputes(pageable));
    }

    @PutMapping("/disputes/{orderId}/judge")
    public ApiResponse<OrderResponse> judgeDispute(@PathVariable Long orderId,
                                                     @Valid @RequestBody DisputeRequest request) {
        OrderResponse result = orderService.adminJudge(orderId, request.getAction(), request.getReason());
        String msg = "APPROVE_REFUND".equals(request.getAction()) ? "已裁定同意退款" : "已裁定维持原状";
        return ApiResponse.success(msg, result);
    }

    @GetMapping("/orders/{orderId}")
    public ApiResponse<OrderResponse> getOrderDetail(@PathVariable Long orderId) {
        return ApiResponse.success(orderService.toResponse(
                orderService.getOrderById(orderId)));
    }

    @GetMapping("/disputes/{orderId}")
    public ApiResponse<OrderResponse> getDisputeDetail(@PathVariable Long orderId) {
        return ApiResponse.success(orderService.toResponse(
                orderService.getOrderById(orderId)));
    }

    // ==================== 举报管理 ====================

    @GetMapping("/reports")
    public ApiResponse<Page<ReportResponse>> listReports(
            @RequestParam(defaultValue = "PENDING") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(reportService.listByStatus(status, pageable));
    }

    @GetMapping("/reports/{id}")
    public ApiResponse<ReportResponse> getReport(@PathVariable Long id) {
        return ApiResponse.success(reportService.getById(id));
    }

    @PutMapping("/reports/{id}/handle")
    public ApiResponse<ReportResponse> handleReport(@PathVariable Long id,
                                                      @Valid @RequestBody HandleReportRequest request) {
        ReportResponse report = reportService.handleReport(id, request.getAction(), request.getAdminNote());
        String msg = "ACCEPTED".equals(request.getAction()) ? "举报已受理" : "举报已驳回";
        return ApiResponse.success(msg, report);
    }

    @PutMapping("/reports/{id}/handle-appeal")
    public ApiResponse<ReportResponse> handleAppeal(@PathVariable Long id,
                                                      @RequestParam String appealResult) {
        ReportResponse report = reportService.handleAppeal(id, appealResult);
        String msg = "UPHELD".equals(appealResult) ? "申诉已驳回，维持原判" : "申诉成立，已改判";
        return ApiResponse.success(msg, report);
    }
}
