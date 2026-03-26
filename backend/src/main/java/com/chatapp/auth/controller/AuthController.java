package com.chatapp.auth.controller;

import com.chatapp.auth.dto.AuthResponse;
import com.chatapp.auth.dto.LoginRequest;
import com.chatapp.auth.dto.RefreshTokenRequest;
import com.chatapp.auth.dto.RegisterRequest;
import com.chatapp.auth.dto.VerifyOtpRequest;
import com.chatapp.auth.service.AuthService;
import com.chatapp.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(
            @Valid @RequestBody RegisterRequest request) {
        authService.initiateRegistration(request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(null, "OTP sent to " + request.getEmail()));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request) {
        AuthResponse response = authService.verifyOtpAndRegister(
                request.getEmail(), request.getOtp());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Account created successfully"));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<Void>> resendOtp(
            @RequestParam String email) {
        authService.resendOtp(email);
        return ResponseEntity.ok(ApiResponse.success(null, "OTP resent to " + email));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(response, "Token refreshed"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            @RequestBody(required = false) RefreshTokenRequest body) {
        String authHeader = request.getHeader("Authorization");
        String accessToken = (authHeader != null && authHeader.startsWith("Bearer "))
                ? authHeader.substring(7) : null;
        String refreshToken = (body != null) ? body.getRefreshToken() : null;

        if (accessToken != null) {
            authService.logout(accessToken, refreshToken);
        }
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out successfully"));
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("OK", "Service is running"));
    }
}
