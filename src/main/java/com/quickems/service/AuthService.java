package com.quickems.service;

import com.quickems.dto.auth.AuthResponse;
import com.quickems.dto.auth.LoginRequest;
import com.quickems.dto.auth.SetPasswordRequest;
import com.quickems.entity.User;
import com.quickems.repository.UserRepository;
import com.quickems.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        // ── Case 1: Employee whose password is not yet set — authenticate via temp password ──
        if (!user.isPasswordSet()) {
            if (user.getTemporaryPassword() == null) {
                throw new BadCredentialsException("No temporary password set. Contact your admin.");
            }
            if (user.isTempPasswordExpired()) {
                throw new BadCredentialsException("Temporary password has expired. Contact your admin to generate a new one.");
            }
            if (!request.getPassword().equals(user.getTemporaryPassword())) {
                throw new BadCredentialsException("Invalid email or password");
            }

            // Generate JWT using Spring Security context
            UserDetails userDetails = buildUserDetails(user);
            String token = jwtUtils.generateToken(userDetails);

            long secondsLeft = ChronoUnit.SECONDS.between(LocalDateTime.now(), user.getTempPasswordExpiry());

            return AuthResponse.builder()
                    .token(token)
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .role(user.getRole())
                    .requiresPasswordChange(true)
                    .tempPasswordExpiresInSeconds(secondsLeft)
                    .build();
        }

        // ── Case 2: Normal login with permanent password ──
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtUtils.generateToken(userDetails);

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .requiresPasswordChange(false)
                .build();
    }

    /**
     * Called by an employee using their temporary password to set a permanent password.
     * Validates the temp password is still active, new passwords match, then saves BCrypt hash.
     */
    @Transactional
    public AuthResponse setPassword(String email, SetPasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        if (user.isPasswordSet()) {
            throw new IllegalStateException("Password has already been set for this account.");
        }
        if (user.getTemporaryPassword() == null || user.isTempPasswordExpired()) {
            throw new IllegalStateException("Temporary password has expired. Contact your admin for a new one.");
        }
        if (!request.getTemporaryPassword().equals(user.getTemporaryPassword())) {
            throw new BadCredentialsException("Temporary password is incorrect.");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New password and confirm password do not match.");
        }

        // Set permanent password, clear temp password fields
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordSet(true);
        user.setTemporaryPassword(null);
        user.setTempPasswordExpiry(null);
        userRepository.save(user);

        // Return a fresh JWT so the employee is immediately logged in with their new password
        UserDetails userDetails = buildUserDetails(user);
        String token = jwtUtils.generateToken(userDetails);

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .requiresPasswordChange(false)
                .build();
    }

    /** Build Spring Security UserDetails from our User entity */
    private UserDetails buildUserDetails(User user) {
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(user.getRole().name())
                .disabled(!user.isEnabled())
                .build();
    }
}
