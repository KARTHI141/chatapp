package com.chatapp.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private static final String OTP_PREFIX = "otp:";
    private static final String PENDING_REG_PREFIX = "pending_reg:";
    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    private static final int OTP_LENGTH = 6;

    private final RedisTemplate<String, Object> redisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    public String generateOtp(String email) {
        String otp = String.format("%06d", secureRandom.nextInt(1000000));
        redisTemplate.opsForValue().set(OTP_PREFIX + email, otp, OTP_TTL);
        log.debug("OTP generated for email: {}", email);
        return otp;
    }

    public boolean verifyOtp(String email, String otp) {
        String key = OTP_PREFIX + email;
        Object stored = redisTemplate.opsForValue().get(key);
        if (stored != null && stored.toString().equals(otp)) {
            redisTemplate.delete(key);
            log.debug("OTP verified for email: {}", email);
            return true;
        }
        return false;
    }

    public void storePendingRegistration(String email, Object registrationData) {
        redisTemplate.opsForValue().set(PENDING_REG_PREFIX + email, registrationData, OTP_TTL);
    }

    public Object getPendingRegistration(String email) {
        return redisTemplate.opsForValue().get(PENDING_REG_PREFIX + email);
    }

    public void clearPendingRegistration(String email) {
        redisTemplate.delete(PENDING_REG_PREFIX + email);
    }
}
