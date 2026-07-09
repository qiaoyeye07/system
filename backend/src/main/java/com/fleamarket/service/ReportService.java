package com.fleamarket.service;

import com.fleamarket.domain.Report;
import com.fleamarket.domain.User;
import com.fleamarket.domain.enums.ReportStatus;
import com.fleamarket.domain.enums.ReportTargetType;
import com.fleamarket.dto.response.ReportResponse;
import com.fleamarket.exception.BusinessException;
import com.fleamarket.exception.ErrorCode;
import com.fleamarket.repository.MessageRepository;
import com.fleamarket.repository.ProductRepository;
import com.fleamarket.repository.ReportRepository;
import com.fleamarket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final MessageRepository messageRepository;

    @Transactional
    public ReportResponse createReport(Long reporterId, ReportTargetType targetType,
                                        Long targetId, String reason, String description) {
        if (targetType == ReportTargetType.USER && reporterId.equals(targetId)) {
            throw new BusinessException(ErrorCode.CANNOT_REPORT_SELF);
        }

        List<Report> existingReports = reportRepository
                .findByReporterIdAndTargetTypeAndTargetIdAndStatusIn(
                        reporterId, targetType, targetId,
                        List.of(ReportStatus.PENDING));

        if (!existingReports.isEmpty()) {
            throw new BusinessException(ErrorCode.DUPLICATE_REPORT);
        }

        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        // 校验合法原因
        List<String> validReasons = List.of("FAKE_DESC", "PROHIBITED", "FRAUD", "HARASS", "OTHER");
        if (!validReasons.contains(reason)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "无效的举报原因");
        }

        Report report = Report.builder()
                .reporter(reporter)
                .targetType(targetType)
                .targetId(targetId)
                .reason(reason)
                .description(description)
                .status(ReportStatus.PENDING)
                .build();

        report = reportRepository.save(report);
        return toResponse(report);
    }

    public ReportResponse getById(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REPORT_NOT_FOUND));
        return toResponse(report);
    }

    public Page<ReportResponse> listByReporter(Long reporterId, Pageable pageable) {
        return reportRepository.findByReporterId(reporterId, pageable).map(this::toResponse);
    }

    public Page<ReportResponse> listByStatus(String status, Pageable pageable) {
        if ("ALL".equalsIgnoreCase(status)) {
            return reportRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::toResponse);
        }
        ReportStatus reportStatus = ReportStatus.valueOf(status.toUpperCase());
        return reportRepository.findByStatusOrderByCreatedAtDesc(reportStatus, pageable).map(this::toResponse);
    }

    @Transactional
    public ReportResponse handleReport(Long reportId, String action, String adminNote) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REPORT_NOT_FOUND));

        if (report.getStatus() != ReportStatus.PENDING) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该举报已被处理");
        }

        ReportStatus newStatus;
        if ("ACCEPTED".equals(action)) {
            newStatus = ReportStatus.ACCEPTED;
        } else if ("REJECTED".equals(action)) {
            newStatus = ReportStatus.REJECTED;
        } else {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "无效的处理结果，可选 ACCEPTED 或 REJECTED");
        }

        report.setStatus(newStatus);
        report.setAdminNote(adminNote);
        reportRepository.save(report);

        return toResponse(report);
    }

    @Transactional
    public ReportResponse appeal(Long reportId, Long userId, String appealReason) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REPORT_NOT_FOUND));

        if (report.getStatus() != ReportStatus.ACCEPTED && report.getStatus() != ReportStatus.REJECTED) {
            throw new BusinessException(ErrorCode.CANNOT_APPEAL);
        }
        if (!report.getReporter().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (report.getAppealReason() != null) {
            throw new BusinessException(ErrorCode.ALREADY_APPEALED);
        }

        report.setPreAppealStatus(report.getStatus());
        report.setStatus(ReportStatus.APPEALING);
        report.setAppealReason(appealReason);
        reportRepository.save(report);

        return toResponse(report);
    }

    @Transactional
    public ReportResponse handleAppeal(Long reportId, String appealResult) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REPORT_NOT_FOUND));

        if (report.getStatus() != ReportStatus.APPEALING) {
            throw new BusinessException(ErrorCode.CANNOT_APPEAL);
        }

        if ("UPHELD".equals(appealResult)) {
            // 维持原判：恢复到申诉前的状态，清除申诉信息，允许再次申诉
            report.setAppealResult("UPHELD");
            report.setStatus(report.getPreAppealStatus() != null
                    ? report.getPreAppealStatus() : ReportStatus.ACCEPTED);
            report.setAppealReason(null);
            report.setPreAppealStatus(null);
        } else if ("OVERTURNED".equals(appealResult)) {
            // 改判：反转原处理结果
            report.setAppealResult("OVERTURNED");
            boolean wasAccepted = report.getPreAppealStatus() == ReportStatus.ACCEPTED;
            report.setStatus(wasAccepted ? ReportStatus.REJECTED : ReportStatus.ACCEPTED);
            report.setAdminNote(null); // 清除原处理备注
        } else {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "无效的申诉结果，可选 UPHELD 或 OVERTURNED");
        }

        reportRepository.save(report);
        return toResponse(report);
    }

    public ReportResponse toResponse(Report report) {
        String targetSummary = resolveTargetSummary(report.getTargetType(), report.getTargetId());
        return ReportResponse.builder()
                .id(report.getId())
                .reporterId(report.getReporter().getId())
                .reporterName(report.getReporter().getUsername())
                .targetType(report.getTargetType().name())
                .targetId(report.getTargetId())
                .targetSummary(targetSummary)
                .reason(report.getReason())
                .description(report.getDescription())
                .status(report.getStatus().name())
                .adminNote(report.getAdminNote())
                .appealReason(report.getAppealReason())
                .appealResult(report.getAppealResult())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
    }

    private String resolveTargetSummary(ReportTargetType targetType, Long targetId) {
        try {
            if (targetType == ReportTargetType.USER) {
                return userRepository.findById(targetId)
                        .map(u -> "用户：" + u.getUsername())
                        .orElse("用户 #" + targetId + "（已删除）");
            } else if (targetType == ReportTargetType.PRODUCT) {
                return productRepository.findById(targetId)
                        .map(p -> "商品：「" + p.getTitle() + "」")
                        .orElse("商品 #" + targetId + "（已下架或删除）");
            } else {
                return messageRepository.findById(targetId)
                        .map(m -> "消息：「" + (m.getContent().length() > 50 ? m.getContent().substring(0, 50) + "…" : m.getContent()) + "」")
                        .orElse("消息 #" + targetId + "（已删除）");
            }
        } catch (Exception e) {
            return targetType.name() + " #" + targetId;
        }
    }
}
