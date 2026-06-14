package com.quickems.service;

import com.quickems.dto.DashboardStats;
import com.quickems.enums.EmploymentStatus;
import com.quickems.enums.LeaveStatus;
import com.quickems.repository.DepartmentRepository;
import com.quickems.repository.EmployeeRepository;
import com.quickems.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final LeaveRequestRepository leaveRequestRepository;

    public DashboardStats getStats() {
        long totalEmployees = employeeRepository.count();
        long activeEmployees = employeeRepository.countByStatus(EmploymentStatus.ACTIVE);
        long totalDepartments = departmentRepository.count();
        long pendingLeave = leaveRequestRepository.countByStatus(LeaveStatus.PENDING);

        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate endOfMonth = LocalDate.now();
        long newHires = employeeRepository.countNewHires(startOfMonth, endOfMonth);

        // By department
        List<Object[]> byDept = employeeRepository.countByDepartment();
        Map<String, Long> empByDept = new LinkedHashMap<>();
        for (Object[] row : byDept) {
            String deptName = row[0] != null ? (String) row[0] : "Unassigned";
            empByDept.put(deptName, (Long) row[1]);
        }

        // By status
        List<Object[]> byStatus = employeeRepository.countByStatusGrouped();
        Map<String, Long> empByStatus = new LinkedHashMap<>();
        for (Object[] row : byStatus) {
            empByStatus.put(row[0].toString(), (Long) row[1]);
        }

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
