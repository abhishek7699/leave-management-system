package com.lms.employee.infrastructure.persistence.repository;

import com.lms.employee.infrastructure.persistence.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Employee> findAllByDepartment(String department);
}
