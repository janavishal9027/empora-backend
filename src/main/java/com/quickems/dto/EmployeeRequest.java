package com.quickems.dto;

import com.quickems.enums.EmploymentStatus;
import com.quickems.enums.Gender;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @Email(message = "Valid email is required")
    @NotBlank(message = "Email is required")
    private String email;

    private String phone;

    private Gender gender;

    private LocalDate dateOfBirth;

    private String address;

    @NotBlank(message = "Job title is required")
    private String jobTitle;

    @DecimalMin(value = "0.0", message = "Salary must be positive")
    private BigDecimal salary;

    private LocalDate hireDate;

    private EmploymentStatus status;

    private String profileImageUrl;

    private Long departmentId;
}
