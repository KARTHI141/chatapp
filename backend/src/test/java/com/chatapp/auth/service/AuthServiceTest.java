package com.chatapp.auth.service;

import com.chatapp.auth.dto.LoginRequest;
import com.chatapp.auth.dto.RegisterRequest;
import com.chatapp.auth.dto.AuthResponse;
import com.chatapp.auth.entity.Role;
import com.chatapp.auth.entity.User;
import com.chatapp.auth.repository.RoleRepository;
import com.chatapp.auth.repository.UserRepository;
import com.chatapp.auth.security.JwtService;
import com.chatapp.common.exception.BadRequestException;
import com.chatapp.common.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private TokenBlacklistService tokenBlacklistService;
    @Mock private OtpService otpService;
    @Mock private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private Role userRole;
    private User savedUser;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setDisplayName("Test User");

        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        userRole = new Role(Role.RoleName.ROLE_USER);
        userRole.setId(1L);

        savedUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .displayName("Test User")
                .roles(Set.of(userRole))
                .build();
    }

    @Test
    @DisplayName("Initiate registration should store pending data and send OTP")
    void initiateRegistration_Success() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(otpService.generateOtp("test@example.com")).thenReturn("123456");

        authService.initiateRegistration(registerRequest);

        verify(otpService).storePendingRegistration("test@example.com", registerRequest);
        verify(otpService).generateOtp("test@example.com");
        verify(emailService).sendOtpEmail("test@example.com", "123456");
    }

    @Test
    @DisplayName("Initiate registration should throw when username already exists")
    void initiateRegistration_DuplicateUsername_ThrowsBadRequest() {
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThatThrownBy(() -> authService.initiateRegistration(registerRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Username is already taken");

        verify(otpService, never()).generateOtp(any());
    }

    @Test
    @DisplayName("Initiate registration should throw when email already exists")
    void initiateRegistration_DuplicateEmail_ThrowsBadRequest() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.initiateRegistration(registerRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Email is already registered");
    }

    @Test
    @DisplayName("Verify OTP should create user and return tokens")
    void verifyOtpAndRegister_Success() {
        when(otpService.verifyOtp("test@example.com", "123456")).thenReturn(true);
        when(otpService.getPendingRegistration("test@example.com")).thenReturn(registerRequest);
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(roleRepository.findByName(Role.RoleName.ROLE_USER)).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(any(User.class))).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refresh-token");

        AuthResponse response = authService.verifyOtpAndRegister("test@example.com", "123456");

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getUsername()).isEqualTo("testuser");
        verify(userRepository).save(any(User.class));
        verify(otpService).clearPendingRegistration("test@example.com");
    }

    @Test
    @DisplayName("Login should authenticate and return tokens")
    void login_Success() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(savedUser));
        when(jwtService.generateToken(savedUser)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(savedUser)).thenReturn("refresh-token");

        AuthResponse response = authService.login(loginRequest);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getUsername()).isEqualTo("testuser");
        verify(authenticationManager).authenticate(any());
    }

    @Test
    @DisplayName("Refresh token should return new access token")
    void refreshToken_Success() {
        when(jwtService.extractUsername("valid-refresh")).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(savedUser));
        when(jwtService.isTokenValid("valid-refresh", savedUser)).thenReturn(true);
        when(tokenBlacklistService.isBlacklisted("valid-refresh")).thenReturn(false);
        when(jwtService.generateToken(savedUser)).thenReturn("new-access-token");

        AuthResponse response = authService.refreshToken("valid-refresh");

        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("valid-refresh");
    }

    @Test
    @DisplayName("Refresh token should throw when token is blacklisted")
    void refreshToken_Blacklisted_ThrowsUnauthorized() {
        when(jwtService.extractUsername("blacklisted-token")).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(savedUser));
        when(jwtService.isTokenValid("blacklisted-token", savedUser)).thenReturn(true);
        when(tokenBlacklistService.isBlacklisted("blacklisted-token")).thenReturn(true);

        assertThatThrownBy(() -> authService.refreshToken("blacklisted-token"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Refresh token has been revoked");
    }

    @Test
    @DisplayName("Logout should blacklist access and refresh tokens")
    void logout_Success() {
        when(jwtService.getRemainingValidity("access-token")).thenReturn(Duration.ofHours(1));
        when(jwtService.getRemainingValidity("refresh-token")).thenReturn(Duration.ofDays(7));

        authService.logout("access-token", "refresh-token");

        verify(tokenBlacklistService).blacklist("access-token", Duration.ofHours(1));
        verify(tokenBlacklistService).blacklist("refresh-token", Duration.ofDays(7));
    }
}
