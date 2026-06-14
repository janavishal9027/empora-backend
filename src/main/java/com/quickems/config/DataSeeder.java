package com.quickems.config;

import com.quickems.entity.User;
import com.quickems.enums.Role;
import com.quickems.repository.DepartmentRepository;
import com.quickems.repository.EmployeeRepository;
import com.quickems.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository     userRepository;
    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder    passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("Ensuring default admin/HR accounts exist...");

        // Only create admin and HR users — no sample data
        upsertUser("Admin User", "admin@empora.com", "Empora@Admin1", Role.ROLE_ADMIN);
        upsertUser("HR Manager", "hr@empora.com",    "Empora@Hr2024", Role.ROLE_HR);

        log.info("Default accounts ready.");
        log.info("Admin → admin@empora.com / Empora@Admin1");
        log.info("HR    → hr@empora.com    / Empora@Hr2024");
    }

    private void upsertUser(String fullName, String email, String rawPassword, Role role) {
        userRepository.findByEmail(email).ifPresentOrElse(
            existing -> {
                existing.setPassword(passwordEncoder.encode(rawPassword));
                existing.setFullName(fullName);
                existing.setRole(role);
                existing.setEnabled(true);
                existing.setPasswordSet(true);
                existing.setTemporaryPassword(null);
                existing.setTempPasswordExpiry(null);
                userRepository.save(existing);
                log.info("Updated user: {}", email);
            },
            () -> {
                User user = User.builder()
                        .fullName(fullName)
                        .email(email)
                        .password(passwordEncoder.encode(rawPassword))
                        .role(role)
                        .enabled(true)
                        .passwordSet(true)
                        .build();
                userRepository.save(user);
                log.info("Created user: {}", email);
            }
        );
    }
}
