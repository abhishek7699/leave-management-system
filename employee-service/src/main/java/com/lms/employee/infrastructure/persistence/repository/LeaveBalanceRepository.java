package com.lms.employee.infrastructure.persistence.repository;

import com.lms.employee.infrastructure.persistence.entity.LeaveBalance;
import com.lms.employee.infrastructure.persistence.entity.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {

    Optional<LeaveBalance> findByEmployeeIdAndLeaveType(Long employeeId, LeaveType leaveType);

    List<LeaveBalance> findAllByEmployeeId(Long employeeId);
}
