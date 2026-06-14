package com.quickems.dto;

import com.quickems.enums.EmploymentStatus;
import com.quickems.enums.Gender;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String employeeId;
    private Gender gender;
    private LocalDate dateOfBirth;
    private String address;
    private String jobTitle;
    private BigDecimal salary;
    private LocalDate hireDate;
    private EmploymentStatus status;
    private String profileImageUrl;
    private Long departmentId;
    private String departmentName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Returned only on first creation so admin can share it with the employee */
    private String temporaryPassword;

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
