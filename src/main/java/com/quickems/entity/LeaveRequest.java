package com.quickems.entity;

import com.quickems.enums.LeaveStatus;
import com.quickems.enums.LeaveType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeaveType leaveType;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeaveStatus status;

    private String reviewComment;

    @Column(updatable = false)
    private LocalDateTime appliedAt;

    private LocalDateTime reviewedAt;

    @PrePersist
    protected void onCreate() {
        appliedAt = LocalDateTime.now();
        if (status == null) status = LeaveStatus.PENDING;
    }
}
