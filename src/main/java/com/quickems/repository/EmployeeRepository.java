package com.quickems.repository;

import com.quickems.entity.Employee;
import com.quickems.enums.EmploymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmail(String email);
    Optional<Employee> findByEmployeeId(String employeeId);
    boolean existsByEmail(String email);
    long countByStatus(EmploymentStatus status);
    long countByDepartmentId(Long departmentId);

    // ── Derived queries instead of custom JPQL for simple filters ──────────
    Page<Employee> findByDepartmentIdAndStatus(Long departmentId, EmploymentStatus status, Pageable pageable);
    Page<Employee> findByDepartmentId(Long departmentId, Pageable pageable);
    Page<Employee> findByStatus(EmploymentStatus status, Pageable pageable);

    // ── Search by text fields ────────────────────────────────────────────────
    Page<Employee> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrEmployeeIdContainingIgnoreCase(
            String firstName, String lastName, String email, String employeeId, Pageable pageable);

    // ── Stats queries ────────────────────────────────────────────────────────
    @Query("SELECT e.department.name, COUNT(e) FROM Employee e WHERE e.department IS NOT NULL GROUP BY e.department.name")
    List<Object[]> countByDepartment();

    @Query("SELECT e.status, COUNT(e) FROM Employee e GROUP BY e.status")
    List<Object[]> countByStatusGrouped();

    @Query("SELECT COUNT(e) FROM Employee e WHERE e.hireDate >= :startDate AND e.hireDate <= :endDate")
    long countNewHires(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query(value = "SELECT MAX(CAST(SUBSTRING(employee_id, 5) AS INTEGER)) FROM employees WHERE employee_id LIKE 'EMP-%'",
           nativeQuery = true)
    Integer findMaxEmployeeIdNumber();
}
