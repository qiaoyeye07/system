package com.fleamarket.service;

import com.fleamarket.domain.Report;
import com.fleamarket.domain.User;
import com.fleamarket.domain.enums.ReportStatus;
import com.fleamarket.domain.enums.ReportTargetType;
import com.fleamarket.dto.response.ReportResponse;
import com.fleamarket.exception.BusinessException;
import com.fleamarket.exception.ErrorCode;
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

    @Transactional
    public ReportResponse createReport(Long reporterId, ReportTargetType targetType,
                                        Long targetId, String reason, String description) {
        if (targetType == ReportTargetType.USER && reporterId.equals(targetId)) {
            throw new BusinessException(ErrorCode.CANNOT_REPORT_SELF);
        }

        List<Report> existingReports = reportRepository
                .findByReporterIdAndTargetTypeAndTargetIdAndStatusIn(
                        reporterId, targetType, targetId,
                        List.of(ReportStatus.PENDING, ReportStatus.PROCESSING));

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
        ReportStatus reportStatus = ReportStatus.valueOf(status.toUpperCase());
        return reportRepository.findByStatus(reportStatus, pageable).map(this::toResponse);
    }

    @Transactional
    public ReportResponse handleReport(Long reportId, String action, String adminNote) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REPORT_NOT_FOUND));

        if (report.getStatus() != ReportStatus.PENDING && report.getStatus() != ReportStatus.PROCESSING) {
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
            // 维持原判，把状态改回受理/驳回
            report.setAppealResult("UPHELD");
        } else if ("OVERTURNED".equals(appealResult)) {
            // 改判
            report.setAppealResult("OVERTURNED");
            report.setStatus(report.getStatus() == ReportStatus.APPEALING
                    ? (report.getAppealReason() != null ? ReportStatus.REJECTED : ReportStatus.ACCEPTED)
                    : report.getStatus());
            // 反转处理结果
            if (report.getAppealReason() != null) {
                report.setStatus(ReportStatus.REJECTED);
            }
        } else {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "无效的申诉结果，可选 UPHELD 或 OVERTURNED");
        }

        reportRepository.save(report);
        return toResponse(report);
    }

    public ReportResponse toResponse(Report report) {
        return ReportResponse.builder()
                .id(report.getId())
                .reporterId(report.getReporter().getId())
                .reporterName(report.getReporter().getUsername())
                .targetType(report.getTargetType().name())
                .targetId(report.getTargetId())
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
}
