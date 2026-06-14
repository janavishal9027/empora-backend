package com.quickems.dto;

import com.quickems.enums.LeaveStatus;
import com.quickems.enums.LeaveType;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveRequestDto {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private String employeeCode;
    private String departmentName;
    private LeaveType leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private long totalDays;
    private String reason;
    private LeaveStatus status;
    private String reviewComment;
    private LocalDateTime appliedAt;
    private LocalDateTime reviewedAt;
}
