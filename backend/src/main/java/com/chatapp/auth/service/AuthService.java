package com.chatapp.auth.service;

import com.chatapp.auth.dto.AuthResponse;
import com.chatapp.auth.dto.LoginRequest;
import com.chatapp.auth.dto.RegisterRequest;
import com.chatapp.auth.entity.Role;
import com.chatapp.auth.entity.User;
import com.chatapp.auth.repository.RoleRepository;
import com.chatapp.auth.repository.UserRepository;
import com.chatapp.auth.security.JwtService;
import com.chatapp.common.exception.BadRequestException;
import com.chatapp.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final TokenBlacklistService tokenBlacklistService;
    private final OtpService otpService;
    private final EmailService emailService;

    /**
     * Step 1: Validate registration data, store pending registration in Redis, send OTP email.
     */
    @Transactional(readOnly = true)
    public void initiateRegistration(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered");
        }

        // Store pending registration in Redis
        otpService.storePendingRegistration(request.getEmail(), request);

        // Generate and send OTP
        String otp = otpService.generateOtp(request.getEmail());
        emailService.sendOtpEmail(request.getEmail(), otp);

        log.info("OTP sent for registration: {}", request.getEmail());
    }

    /**
     * Step 2: Verify OTP and complete registration.
     */
    @Transactional
    public AuthResponse verifyOtpAndRegister(String email, String otp) {
        if (!otpService.verifyOtp(email, otp)) {
            throw new BadRequestException("Invalid or expired OTP");
        }

        Object pendingData = otpService.getPendingRegistration(email);
        if (pendingData == null) {
            throw new BadRequestException("Registration session expired. Please register again.");
        }

        // Convert pending data back to RegisterRequest
        RegisterRequest request;
        if (pendingData instanceof RegisterRequest) {
            request = (RegisterRequest) pendingData;
        } else {
            // Redis may deserialize as LinkedHashMap
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            request = mapper.convertValue(pendingData, RegisterRequest.class);
        }

        // Re-check uniqueness (race condition guard)
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered");
        }

        Role userRole = roleRepository.findByName(Role.RoleName.ROLE_USER)
                .orElseGet(() -> roleRepository.save(new Role(Role.RoleName.ROLE_USER)));

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .displayName(request.getDisplayName() != null
                        ? request.getDisplayName() : request.getUsername())
                .roles(Set.of(userRole))
                .build();

        User savedUser = userRepository.save(user);
        otpService.clearPendingRegistration(email);
        log.info("User registered after OTP verification: {}", savedUser.getUsername());

        String accessToken = jwtService.generateToken(savedUser);
        String refreshToken = jwtService.generateRefreshToken(savedUser);

        return buildAuthResponse(savedUser, accessToken, refreshToken);
    }

    /**
     * Resend OTP for a pending registration.
     */
    public void resendOtp(String email) {
        Object pendingData = otpService.getPendingRegistration(email);
        if (pendingData == null) {
            throw new BadRequestException("No pending registration found. Please register again.");
        }

        String otp = otpService.generateOtp(email);
        emailService.sendOtpEmail(email, otp);
        log.info("OTP resent for: {}", email);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(), request.getPassword()));

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadRequestException("Invalid credentials"));

        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        log.info("User logged in: {}", user.getUsername());
        return buildAuthResponse(user, accessToken, refreshToken);
    }

    /**
     * Refresh an expired access token using a valid refresh token.
     *
     * Interview note: Refresh tokens have a longer TTL than access tokens.
     * The client sends the refresh token to get a new access token without
     * re-entering credentials. The old refresh token remains valid until
     * it expires or the user logs out.
     */
    public AuthResponse refreshToken(String refreshToken) {
        String username;
        try {
            username = jwtService.extractUsername(refreshToken);
        } catch (Exception e) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        if (!jwtService.isTokenValid(refreshToken, user)) {
            throw new UnauthorizedException("Refresh token is expired or invalid");
        }

        if (tokenBlacklistService.isBlacklisted(refreshToken)) {
            throw new UnauthorizedException("Refresh token has been revoked");
        }

        String newAccessToken = jwtService.generateToken(user);
        log.info("Access token refreshed for user: {}", username);
        return buildAuthResponse(user, newAccessToken, refreshToken);
    }

    /**
     * Logout by blacklisting both the access and refresh tokens in Redis.
     * Tokens are stored with TTL = remaining validity, so Redis auto-cleans them.
     */
    public void logout(String accessToken, String refreshToken) {
        try {
            Duration accessTtl = jwtService.getRemainingValidity(accessToken);
            tokenBlacklistService.blacklist(accessToken, accessTtl);
        } catch (Exception e) {
            log.warn("Could not blacklist access token: {}", e.getMessage());
        }

        if (refreshToken != null) {
            try {
                Duration refreshTtl = jwtService.getRemainingValidity(refreshToken);
                tokenBlacklistService.blacklist(refreshToken, refreshTtl);
            } catch (Exception e) {
                log.warn("Could not blacklist refresh token: {}", e.getMessage());
            }
        }

        log.info("User logged out, tokens blacklisted");
    }

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .roles(user.getRoles().stream()
                        .map(r -> r.getName().name())
                        .collect(Collectors.toSet()))
                .build();
    }
}
