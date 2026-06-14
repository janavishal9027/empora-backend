package com.quickems.dto;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStats {
    private long totalEmployees;
    private long activeEmployees;
    private long totalDepartments;
    private long pendingLeaveRequests;
    private long newHiresThisMonth;
    private Map<String, Long> employeesByDepartment;
    private Map<String, Long> employeesByStatus;
}
