package com.quickems.controller;

import com.quickems.dto.auth.AuthResponse;
import com.quickems.dto.auth.LoginRequest;
import com.quickems.dto.auth.SetPasswordRequest;
import com.quickems.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Employee calls this (while logged in with their JWT from temp-password login)
     * to set their permanent password.
     */
    @PostMapping("/set-password")
    public ResponseEntity<AuthResponse> setPassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SetPasswordRequest request) {
        return ResponseEntity.ok(authService.setPassword(userDetails.getUsername(), request));
    }
}
