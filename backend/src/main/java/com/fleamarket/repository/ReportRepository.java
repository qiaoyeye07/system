package com.fleamarket.repository;

import com.fleamarket.domain.Report;
import com.fleamarket.domain.enums.ReportStatus;
import com.fleamarket.domain.enums.ReportTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    Page<Report> findByReporterId(Long reporterId, Pageable pageable);

    Page<Report> findByStatus(ReportStatus status, Pageable pageable);

    List<Report> findByReporterIdAndTargetTypeAndTargetIdAndStatusIn(
            Long reporterId, ReportTargetType targetType, Long targetId, List<ReportStatus> statuses);

    Page<Report> findByTargetTypeAndTargetId(ReportTargetType targetType, Long targetId, Pageable pageable);
}
