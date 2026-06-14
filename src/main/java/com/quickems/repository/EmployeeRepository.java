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

    @Query("SELECT e FROM Employee e WHERE " +
           "(:search IS NULL OR LOWER(e.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(e.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(e.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(e.employeeId) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:departmentId IS NULL OR e.department.id = :departmentId) AND " +
           "(:status IS NULL OR e.status = :status)")
    Page<Employee> searchEmployees(@Param("search") String search,
                                    @Param("departmentId") Long departmentId,
                                    @Param("status") EmploymentStatus status,
                                    Pageable pageable);

    @Query("SELECT e.department.name, COUNT(e) FROM Employee e GROUP BY e.department.name")
    List<Object[]> countByDepartment();

    @Query("SELECT e.status, COUNT(e) FROM Employee e GROUP BY e.status")
    List<Object[]> countByStatusGrouped();

    @Query("SELECT COUNT(e) FROM Employee e WHERE e.hireDate >= :startDate AND e.hireDate <= :endDate")
    long countNewHires(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT MAX(CAST(SUBSTRING(e.employeeId, 5) AS int)) FROM Employee e WHERE e.employeeId LIKE 'EMP-%'")
    Integer findMaxEmployeeIdNumber();
}
