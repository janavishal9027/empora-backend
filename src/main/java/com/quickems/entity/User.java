package com.quickems.entity;

import com.quickems.enums.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Email
    @NotBlank
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @NotBlank
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "temporary_password", length = 255)
    private String temporaryPassword;

    @Column(name = "temp_password_expiry")
    private LocalDateTime tempPasswordExpiry;

    @Column(name = "password_set", nullable = false)
    private boolean passwordSet = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public boolean isTempPasswordActive() {
        return temporaryPassword != null
                && tempPasswordExpiry != null
                && LocalDateTime.now().isBefore(tempPasswordExpiry);
    }

    public boolean isTempPasswordExpired() {
        return temporaryPassword != null
                && tempPasswordExpiry != null
                && LocalDateTime.now().isAfter(tempPasswordExpiry);
    }
}
