package com.quickems.dto.auth;

import com.quickems.enums.Role;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String token;
    private String email;
    private String fullName;
    private Role role;
    /** True when the employee logged in with a temporary password and must set a new one */
    private boolean requiresPasswordChange;
    /** Remaining seconds on the temp password (shown in UI countdown) */
    private Long tempPasswordExpiresInSeconds;
}
