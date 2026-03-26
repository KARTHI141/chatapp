package com.chatapp.user.service;

import com.chatapp.auth.entity.User;
import com.chatapp.auth.repository.UserRepository;
import com.chatapp.common.exception.ResourceNotFoundException;
import com.chatapp.user.dto.UserDto;
import com.chatapp.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        return userMapper.toDto(user);
    }

    public UserDto getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        return userMapper.toDto(user);
    }

    public List<UserDto> searchUsers(String query) {
        return userRepository
                .findByUsernameContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(query, query)
                .stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserDto updateProfile(Long userId, String displayName, String avatarUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (displayName != null) {
            user.setDisplayName(displayName);
        }
        if (avatarUrl != null) {
            user.setAvatarUrl(avatarUrl);
        }

        User updated = userRepository.save(user);
        log.info("User profile updated: {}", userId);
        return userMapper.toDto(updated);
    }

    public List<UserDto> getUsersByIds(List<Long> ids) {
        return userRepository.findByIdIn(ids).stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }
}
