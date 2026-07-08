package com.fleamarket.controller;

import com.fleamarket.domain.enums.ReportTargetType;
import com.fleamarket.dto.request.AppealRequest;
import com.fleamarket.dto.request.CreateReportRequest;
import com.fleamarket.dto.response.ApiResponse;
import com.fleamarket.dto.response.ReportResponse;
import com.fleamarket.security.SecurityUtils;
import com.fleamarket.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ReportController {

    private final ReportService reportService;

    @PostMapping("/reports")
    public ApiResponse<ReportResponse> create(@Valid @RequestBody CreateReportRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        ReportTargetType targetType = parseTargetType(request.getTargetType());
        ReportResponse report = reportService.createReport(
                userId, targetType, request.getTargetId(), request.getReason(), request.getDescription());
        return ApiResponse.success("举报已提交，请等待处理结果", report);
    }

    @GetMapping("/my/reports")
    public ApiResponse<Page<ReportResponse>> listMyReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Long userId = SecurityUtils.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(reportService.listByReporter(userId, pageable));
    }

    @PostMapping("/reports/{id}/appeal")
    public ApiResponse<ReportResponse> appeal(@PathVariable Long id,
                                                @Valid @RequestBody AppealRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        ReportResponse report = reportService.appeal(id, userId, request.getAppealReason());
        return ApiResponse.success("申诉已提交", report);
    }

    private ReportTargetType parseTargetType(String type) {
        try {
            return ReportTargetType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new com.fleamarket.exception.BusinessException(
                    com.fleamarket.exception.ErrorCode.BAD_REQUEST,
                    "无效的举报对象类型，可选: PRODUCT, USER, MESSAGE");
        }
    }
}
