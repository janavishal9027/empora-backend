package com.quickems.service;

import com.quickems.dto.EmployeeDto;
import com.quickems.dto.EmployeeRequest;
import com.quickems.entity.Department;
import com.quickems.entity.Employee;
import com.quickems.entity.Users;
import com.quickems.enums.EmploymentStatus;
import com.quickems.enums.Role;
import com.quickems.exception.DuplicateResourceException;
import com.quickems.exception.ResourceNotFoundException;
import com.quickems.repository.DepartmentRepository;
import com.quickems.repository.EmployeeRepository;
import com.quickems.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private static final int TEMP_PASSWORD_HOURS = 1;

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Page<EmployeeDto> getAllEmployees(String search, Long departmentId, EmploymentStatus status,
                                             int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        // Use derived queries to avoid LOWER(bytea) issue with Hibernate 6 + PostgreSQL
        if (search != null && !search.isBlank()) {
            // When searching by text, get all text matches then filter in memory for dept/status
            // (acceptable for typical employee counts < 10k)
            Page<Employee> results = employeeRepository
                    .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrEmployeeIdContainingIgnoreCase(
                            search, search, search, search, pageable);
            return results.map(this::toDto);
        }

        // No search text — use department/status filters directly
        if (departmentId != null && status != null) {
            return employeeRepository.findByDepartmentIdAndStatus(departmentId, status, pageable).map(this::toDto);
        }
        if (departmentId != null) {
            return employeeRepository.findByDepartmentId(departmentId, pageable).map(this::toDto);
        }
        if (status != null) {
            return employeeRepository.findByStatus(status, pageable).map(this::toDto);
        }
        return employeeRepository.findAll(pageable).map(this::toDto);
    }

    public EmployeeDto getEmployeeById(Long id) {
        return toDto(findById(id));
    }

    @Transactional
    public EmployeeDto createEmployee(EmployeeRequest request) {
        if (employeeRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Employee with email '" + request.getEmail() + "' already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("A user account with email '" + request.getEmail() + "' already exists");
        }

        // Create employee record
        Employee employee = Employee.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .employeeId(generateEmployeeId())
                .gender(request.getGender())
                .dateOfBirth(request.getDateOfBirth())
                .address(request.getAddress())
                .jobTitle(request.getJobTitle())
                .salary(request.getSalary())
                .hireDate(request.getHireDate() != null ? request.getHireDate() : LocalDate.now())
                .status(request.getStatus() != null ? request.getStatus() : EmploymentStatus.ACTIVE)
                .profileImageUrl(request.getProfileImageUrl())
                .build();

        if (request.getDepartmentId() != null) {
            Department dept = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
            employee.setDepartment(dept);
        }
        employeeRepository.save(employee);

        // Auto-generate a secure temporary password
        String tempPwd = generateSecurePassword();

        // Create user account with the auto-generated temporary password
        Users user = Users.builder()
                .fullName(request.getFirstName() + " " + request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(tempPwd))
                .role(Role.ROLE_EMPLOYEE)
                .enabled(true)
                .temporaryPassword(tempPwd)
                .tempPasswordExpiry(LocalDateTime.now().plusHours(TEMP_PASSWORD_HOURS))
                .passwordSet(false)
                .build();
        userRepository.save(user);

        // Return DTO with the temporary password so admin can share it
        EmployeeDto dto = toDto(employee);
        dto.setTemporaryPassword(tempPwd);
        return dto;
    }

    @Transactional
    public EmployeeDto updateEmployee(Long id, EmployeeRequest request) {
        Employee employee = findById(id);

        if (!employee.getEmail().equals(request.getEmail()) && employeeRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Employee with email '" + request.getEmail() + "' already exists");
        }

        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setEmail(request.getEmail());
        employee.setPhone(request.getPhone());
        employee.setGender(request.getGender());
        employee.setDateOfBirth(request.getDateOfBirth());
        employee.setAddress(request.getAddress());
        employee.setJobTitle(request.getJobTitle());
        employee.setSalary(request.getSalary());
        employee.setHireDate(request.getHireDate());
        employee.setProfileImageUrl(request.getProfileImageUrl());

        if (request.getStatus() != null) {
            employee.setStatus(request.getStatus());
        }
        if (request.getDepartmentId() != null) {
            Department dept = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
            employee.setDepartment(dept);
        } else {
            employee.setDepartment(null);
        }
        return toDto(employeeRepository.save(employee));
    }

    @Transactional
    public void deleteEmployee(Long id) {
        employeeRepository.delete(findById(id));
    }

    @Transactional
    public EmployeeDto updateStatus(Long id, EmploymentStatus status) {
        Employee employee = findById(id);
        employee.setStatus(status);
        return toDto(employeeRepository.save(employee));
    }

    /**
     * Admin generates a new temporary password for an employee whose temp password has expired.
     * Returns the plain-text temp password so admin can share it with the employee.
     */
    @Transactional
    public String generateTemporaryPassword(Long employeeId, String newTempPassword) {
        Employee employee = findById(employeeId);
        Users user = userRepository.findByEmail(employee.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("No user account found for employee"));

        if (user.isPasswordSet()) {
            throw new IllegalStateException("This employee has already set a permanent password. Temp password not needed.");
        }
        if (user.isTempPasswordActive()) {
            throw new IllegalStateException("A temporary password is still active for this employee. Wait for it to expire first.");
        }

        user.setTemporaryPassword(newTempPassword);
        user.setTempPasswordExpiry(LocalDateTime.now().plusHours(TEMP_PASSWORD_HOURS));
        user.setPassword(passwordEncoder.encode(newTempPassword));
        userRepository.save(user);

        return newTempPassword;
    }

    private Employee findById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
    }

    private String generateEmployeeId() {
        Integer maxNum = employeeRepository.findMaxEmployeeIdNumber();
        int next = (maxNum == null ? 0 : maxNum) + 1;
        return String.format("EMP-%04d", next);
    }

    /**
     * Generates a secure random password with:
     * - 2 uppercase letters
     * - 2 lowercase letters
     * - 2 digits
     * - 2 special characters
     * Format: Emp-XXXX1234!@  (12 chars total, easy to share)
     */
    private String generateSecurePassword() {
        String upper   = "ABCDEFGHJKLMNPQRSTUVWXYZ";
        String lower   = "abcdefghjkmnpqrstuvwxyz";
        String digits  = "23456789";
        String special = "!@#$%";
        String all     = upper + lower + digits + special;

        SecureRandom rng = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        // Guarantee at least one of each category
        sb.append(upper.charAt(rng.nextInt(upper.length())));
        sb.append(upper.charAt(rng.nextInt(upper.length())));
        sb.append(lower.charAt(rng.nextInt(lower.length())));
        sb.append(lower.charAt(rng.nextInt(lower.length())));
        sb.append(digits.charAt(rng.nextInt(digits.length())));
        sb.append(digits.charAt(rng.nextInt(digits.length())));
        sb.append(special.charAt(rng.nextInt(special.length())));
        sb.append(special.charAt(rng.nextInt(special.length())));
        // Fill remaining 4 chars from full set
        for (int i = 0; i < 4; i++) {
            sb.append(all.charAt(rng.nextInt(all.length())));
        }
        // Shuffle so the pattern isn't predictable
        char[] chars = sb.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            char tmp = chars[i]; chars[i] = chars[j]; chars[j] = tmp;
        }
        return new String(chars);
    }

    private EmployeeDto toDto(Employee e) {
        return EmployeeDto.builder()
                .id(e.getId())
                .firstName(e.getFirstName())
                .lastName(e.getLastName())
                .email(e.getEmail())
                .phone(e.getPhone())
                .employeeId(e.getEmployeeId())
                .gender(e.getGender())
                .dateOfBirth(e.getDateOfBirth())
                .address(e.getAddress())
                .jobTitle(e.getJobTitle())
                .salary(e.getSalary())
                .hireDate(e.getHireDate())
                .status(e.getStatus())
                .profileImageUrl(e.getProfileImageUrl())
                .departmentId(e.getDepartment() != null ? e.getDepartment().getId() : null)
                .departmentName(e.getDepartment() != null ? e.getDepartment().getName() : null)
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
