package com.quickems.controller;

import com.quickems.dto.EmployeeDto;
import com.quickems.dto.EmployeeRequest;
import com.quickems.enums.EmploymentStatus;
import com.quickems.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    public ResponseEntity<Page<EmployeeDto>> getAllEmployees(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) EmploymentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "firstName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(employeeService.getAllEmployees(search, departmentId, status, page, size, sortBy, sortDir));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeDto> getEmployeeById(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getEmployeeById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_HR')")
    public ResponseEntity<EmployeeDto> createEmployee(@Valid @RequestBody EmployeeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeService.createEmployee(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_HR')")
    public ResponseEntity<EmployeeDto> updateEmployee(@PathVariable Long id,
                                                       @Valid @RequestBody EmployeeRequest request) {
        return ResponseEntity.ok(employeeService.updateEmployee(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_HR')")
    public ResponseEntity<EmployeeDto> updateStatus(@PathVariable Long id,
                                                     @RequestParam EmploymentStatus status) {
        return ResponseEntity.ok(employeeService.updateStatus(id, status));
    }

    /**
     * Admin generates a new temporary password for an employee whose previous one expired.
     * Backend auto-generates a secure password and returns it.
     */
    @PostMapping("/{id}/generate-temp-password")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<java.util.Map<String, String>> generateTempPassword(@PathVariable Long id) {
        String result = employeeService.generateTemporaryPassword(id);
        return ResponseEntity.ok(java.util.Map.of(
                "message", "Temporary password generated. Valid for 1 hour.",
                "temporaryPassword", result
        ));
    }
}
