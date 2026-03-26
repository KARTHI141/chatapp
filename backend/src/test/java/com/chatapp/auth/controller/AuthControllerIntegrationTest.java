package com.chatapp.auth.controller;

import com.chatapp.auth.dto.LoginRequest;
import com.chatapp.auth.dto.RegisterRequest;
import com.chatapp.auth.dto.VerifyOtpRequest;
import com.chatapp.auth.service.OtpService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for the full authentication flow with OTP verification.
 * Tests register → verify OTP → login → access protected endpoint → refresh → logout.
 *
 * Note: This test requires MySQL, Redis, and MongoDB to be running.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static String accessToken;
    private static String refreshToken;

    @Test
    @Order(1)
    @DisplayName("POST /api/auth/register - should initiate registration and send OTP")
    void register_shouldSendOtp() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("integrationuser");
        request.setEmail("integration@test.com");
        request.setPassword("password123");
        request.setDisplayName("Integration Test User");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("OTP sent to integration@test.com"));
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/auth/verify-otp - should verify OTP and create user")
    void verifyOtp_shouldReturnTokens() throws Exception {
        // Read OTP from Redis directly (integration test only)
        String otp = redisTemplate.opsForValue().get("otp:integration@test.com");

        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setEmail("integration@test.com");
        request.setOtp(otp);

        MvcResult result = mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.username").value("integrationuser"))
                .andExpect(jsonPath("$.data.email").value("integration@test.com"))
                .andExpect(jsonPath("$.data.roles", hasItem("ROLE_USER")))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        accessToken = objectMapper.readTree(body).at("/data/accessToken").asText();
        refreshToken = objectMapper.readTree(body).at("/data/refreshToken").asText();
    }

    @Test
    @Order(3)
    @DisplayName("POST /api/auth/register - duplicate username should return 400")
    void register_duplicateUsername_shouldReturn400() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("integrationuser");
        request.setEmail("other@test.com");
        request.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Username is already taken"));
    }

    @Test
    @Order(4)
    @DisplayName("POST /api/auth/register - validation errors on empty body")
    void register_invalidInput_shouldReturn400() throws Exception {
        RegisterRequest request = new RegisterRequest();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.username").exists())
                .andExpect(jsonPath("$.data.email").exists())
                .andExpect(jsonPath("$.data.password").exists());
    }

    @Test
    @Order(5)
    @DisplayName("POST /api/auth/login - should login and return tokens")
    void login_shouldReturnTokens() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("integrationuser");
        request.setPassword("password123");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.username").value("integrationuser"))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        accessToken = objectMapper.readTree(body).at("/data/accessToken").asText();
        refreshToken = objectMapper.readTree(body).at("/data/refreshToken").asText();
    }

    @Test
    @Order(6)
    @DisplayName("POST /api/auth/login - invalid password should return 401")
    void login_wrongPassword_shouldReturn401() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("integrationuser");
        request.setPassword("wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @Order(7)
    @DisplayName("GET /api/users/me - should return user with valid token")
    void protectedEndpoint_withValidToken_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("integrationuser"));
    }

    @Test
    @Order(8)
    @DisplayName("GET /api/users/me - should return 403 without token")
    void protectedEndpoint_withoutToken_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(9)
    @DisplayName("POST /api/auth/refresh - should return new access token")
    void refreshToken_shouldReturnNewAccessToken() throws Exception {
        String body = "{\"refreshToken\": \"" + refreshToken + "\"}";

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    @Test
    @Order(10)
    @DisplayName("GET /api/auth/health - public endpoint should be accessible")
    void healthEndpoint_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/auth/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("OK"));
    }
}
