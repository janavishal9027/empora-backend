package com.quickems.service;

import com.quickems.dto.LeaveRequestCreate;
import com.quickems.dto.LeaveRequestDto;
import com.quickems.entity.Employee;
import com.quickems.entity.LeaveRequest;
import com.quickems.enums.LeaveStatus;
import com.quickems.exception.ResourceNotFoundException;
import com.quickems.repository.EmployeeRepository;
import com.quickems.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class LeaveService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;

    @Transactional(readOnly = true)
    public Page<LeaveRequestDto> getAllLeaveRequests(LeaveStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("appliedAt").descending());
        if (status != null) {
            return leaveRequestRepository.findByStatus(status, pageable).map(this::toDto);
        }
        return leaveRequestRepository.findAll(pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<LeaveRequestDto> getLeaveRequestsByEmployee(Long employeeId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("appliedAt").descending());
        return leaveRequestRepository.findByEmployeeId(employeeId, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public LeaveRequestDto getLeaveRequestById(Long id) {
        return toDto(findById(id));
    }

    @Transactional
    public LeaveRequestDto createLeaveRequest(LeaveRequestCreate request) {
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        LeaveRequest leave = LeaveRequest.builder()
                .employee(employee)
                .leaveType(request.getLeaveType())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .reason(request.getReason())
                .status(LeaveStatus.PENDING)
                .build();

        return toDto(leaveRequestRepository.save(leave));
    }

    @Transactional
    public LeaveRequestDto reviewLeaveRequest(Long id, LeaveStatus status, String comment) {
        LeaveRequest leave = findById(id);
        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new IllegalStateException("Only pending requests can be reviewed");
        }
        leave.setStatus(status);
        leave.setReviewComment(comment);
        leave.setReviewedAt(LocalDateTime.now());
        leaveRequestRepository.save(leave);

        // If approved and the leave period includes today, set employee status to ON_LEAVE
        if (status == LeaveStatus.APPROVED) {
            LocalDate today = LocalDate.now();
            if (!today.isBefore(leave.getStartDate()) && !today.isAfter(leave.getEndDate())) {
                Employee emp = leave.getEmployee();
                emp.setStatus(com.quickems.enums.EmploymentStatus.ON_LEAVE);
                employeeRepository.save(emp);
            }
        }

        return toDto(leave);
    }

    @Transactional
    public void cancelLeaveRequest(Long id) {
        LeaveRequest leave = findById(id);
        if (leave.getStatus() == LeaveStatus.APPROVED || leave.getStatus() == LeaveStatus.REJECTED) {
            throw new IllegalStateException("Cannot cancel an already reviewed request");
        }
        leave.setStatus(LeaveStatus.CANCELLED);
        leaveRequestRepository.save(leave);
    }

    private LeaveRequest findById(Long id) {
        return leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with id: " + id));
    }

    private LeaveRequestDto toDto(LeaveRequest l) {
        long totalDays = ChronoUnit.DAYS.between(l.getStartDate(), l.getEndDate()) + 1;
        Employee emp = l.getEmployee();
        return LeaveRequestDto.builder()
                .id(l.getId())
                .employeeId(emp.getId())
                .employeeName(emp.getFirstName() + " " + emp.getLastName())
                .employeeCode(emp.getEmployeeId())
                .departmentName(emp.getDepartment() != null ? emp.getDepartment().getName() : null)
                .leaveType(l.getLeaveType())
                .startDate(l.getStartDate())
                .endDate(l.getEndDate())
                .totalDays(totalDays)
                .reason(l.getReason())
                .status(l.getStatus())
                .reviewComment(l.getReviewComment())
                .appliedAt(l.getAppliedAt())
                .reviewedAt(l.getReviewedAt())
                .build();
    }
}
