package com.quickems.controller;

import com.quickems.entity.Users;
import com.quickems.enums.Role;
import com.quickems.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One-time setup endpoint — resets admin/HR user passwords.
 * Call GET /api/setup/reset-users?adminPwd=X&hrPwd=Y to reset credentials.
 */
@RestController
@RequestMapping("/api/setup")
@RequiredArgsConstructor
public class SetupController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/reset-users")
    public ResponseEntity<Map<String, Object>> resetUsers(
            @RequestParam(defaultValue = "Empora@Admin1") String adminPwd,
            @RequestParam(defaultValue = "Empora@Hr2024") String hrPwd) {

        Map<String, Object> result = new LinkedHashMap<>();
        resetOrCreate("Admin User", "admin@empora.com", adminPwd, Role.ROLE_ADMIN, result);
        resetOrCreate("HR Manager", "hr@empora.com",    hrPwd,    Role.ROLE_HR,    result);
        result.put("status", "done");
        result.put("message", "Users reset. Login with the passwords you provided.");
        return ResponseEntity.ok(result);
    }

    private void resetOrCreate(String fullName, String email, String rawPassword,
                                Role role, Map<String, Object> result) {
        userRepository.findByEmail(email).ifPresentOrElse(
            user -> {
                user.setPassword(passwordEncoder.encode(rawPassword));
                user.setFullName(fullName);
                user.setRole(role);
                user.setEnabled(true);
                user.setPasswordSet(true);
                user.setTemporaryPassword(null);
                user.setTempPasswordExpiry(null);
                userRepository.save(user);
                result.put(email, "reset");
            },
            () -> {
                Users u = Users.builder()
                        .fullName(fullName)
                        .email(email)
                        .password(passwordEncoder.encode(rawPassword))
                        .role(role)
                        .enabled(true)
                        .passwordSet(true)
                        .build();
                userRepository.save(u);
                result.put(email, "created");
            }
        );
    }
}
