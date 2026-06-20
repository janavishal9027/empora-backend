package com.quickems.service;

import com.quickems.dto.DashboardStats;
import com.quickems.enums.EmploymentStatus;
import com.quickems.enums.LeaveStatus;
import com.quickems.repository.DepartmentRepository;
import com.quickems.repository.EmployeeRepository;
import com.quickems.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final LeaveRequestRepository leaveRequestRepository;

    @Transactional(readOnly = true)
    public DashboardStats getStats() {
        long totalEmployees  = employeeRepository.count();
        long activeEmployees = employeeRepository.countByStatus(EmploymentStatus.ACTIVE);
        long onLeave         = employeeRepository.countByStatus(EmploymentStatus.ON_LEAVE);
        long totalDepartments = departmentRepository.count();
        long pendingLeave    = leaveRequestRepository.countByStatus(LeaveStatus.PENDING);

        // New hires this month — employees created in the current month
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate today        = LocalDate.now();
        // Use createdAt (actual creation time) to determine new hires
        long newHires = employeeRepository.countByCreatedAtBetween(
                startOfMonth.atStartOfDay(), today.plusDays(1).atStartOfDay());

        // By department
        List<Object[]> byDept = employeeRepository.countByDepartment();
        Map<String, Long> empByDept = new LinkedHashMap<>();
        for (Object[] row : byDept) {
            String deptName = row[0] != null ? (String) row[0] : "Unassigned";
            empByDept.put(deptName, (Long) row[1]);
        }

        // By status — ensure ON_LEAVE is always present in the map
        List<Object[]> byStatus = employeeRepository.countByStatusGrouped();
        Map<String, Long> empByStatus = new LinkedHashMap<>();
        for (Object[] row : byStatus) {
            empByStatus.put(row[0].toString(), (Long) row[1]);
        }
        // Ensure ON_LEAVE key exists even if 0
        empByStatus.putIfAbsent("ON_LEAVE", 0L);
        empByStatus.putIfAbsent("ACTIVE", 0L);

        return DashboardStats.builder()
                .totalEmployees(totalEmployees)
                .activeEmployees(activeEmployees)
                .totalDepartments(totalDepartments)
                .pendingLeaveRequests(pendingLeave)
                .newHiresThisMonth(newHires)
                .employeesByDepartment(empByDept)
                .employeesByStatus(empByStatus)
                .build();
    }
}
