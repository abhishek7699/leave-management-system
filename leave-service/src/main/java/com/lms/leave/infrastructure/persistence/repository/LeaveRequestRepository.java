package com.lms.leave.infrastructure.persistence.repository;

import com.lms.leave.infrastructure.persistence.entity.LeaveRequest;
import com.lms.leave.infrastructure.persistence.entity.LeaveStatus;
import com.lms.leave.infrastructure.persistence.entity.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    List<LeaveRequest> findAllByEmployeeId(Long employeeId);

    List<LeaveRequest> findAllByManagerIdAndStatus(Long managerId, LeaveStatus status);

    @Query("""
            SELECT lr FROM LeaveRequest lr
            WHERE lr.employeeId = :employeeId
              AND lr.leaveType  = :leaveType
              AND lr.status IN ('PENDING', 'APPROVED')
              AND lr.startDate <= :endDate
              AND lr.endDate   >= :startDate
            """)
    List<LeaveRequest> findOverlapping(
            @Param("employeeId") Long employeeId,
            @Param("leaveType") LeaveType leaveType,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
