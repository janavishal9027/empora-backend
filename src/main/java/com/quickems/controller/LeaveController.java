package com.quickems.controller;

import com.quickems.dto.LeaveRequestCreate;
import com.quickems.dto.LeaveRequestDto;
import com.quickems.enums.LeaveStatus;
import com.quickems.service.LeaveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/leave-requests")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;

    @GetMapping
    public ResponseEntity<Page<LeaveRequestDto>> getAllLeaveRequests(
            @RequestParam(required = false) LeaveStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(leaveService.getAllLeaveRequests(status, page, size));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<Page<LeaveRequestDto>> getLeaveRequestsByEmployee(
            @PathVariable Long employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(leaveService.getLeaveRequestsByEmployee(employeeId, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeaveRequestDto> getLeaveRequestById(@PathVariable Long id) {
        return ResponseEntity.ok(leaveService.getLeaveRequestById(id));
    }

    @PostMapping
    public ResponseEntity<LeaveRequestDto> createLeaveRequest(@Valid @RequestBody LeaveRequestCreate request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(leaveService.createLeaveRequest(request));
    }

    @PatchMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_HR')")
    public ResponseEntity<LeaveRequestDto> reviewLeaveRequest(
            @PathVariable Long id,
            @RequestParam LeaveStatus status,
            @RequestParam(required = false) String comment) {
        return ResponseEntity.ok(leaveService.reviewLeaveRequest(id, status, comment));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelLeaveRequest(@PathVariable Long id) {
        leaveService.cancelLeaveRequest(id);
        return ResponseEntity.noContent().build();
    }
}
