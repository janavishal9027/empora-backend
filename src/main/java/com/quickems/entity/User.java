package com.quickems.entity;

import com.quickems.enums.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

@Entity
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
    private String fullName;

    @Email
    @NotBlank
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private boolean enabled = true;

    /** Raw (plain) temporary password — stored only until employee sets their own password */
    private String temporaryPassword;

    /** When the temporary password expires (1 hour after generation) */
    private LocalDateTime tempPasswordExpiry;

    /** True once the employee has set their own permanent password */
    @Column(nullable = false)
    private boolean passwordSet = false;

    @Column(updatable = false)
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
