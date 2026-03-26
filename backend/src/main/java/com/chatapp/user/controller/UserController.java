package com.chatapp.user.controller;

import com.chatapp.auth.entity.User;
import com.chatapp.common.dto.ApiResponse;
import com.chatapp.user.dto.UserDto;
import com.chatapp.user.service.UserPresenceService;
import com.chatapp.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserPresenceService userPresenceService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser(
            @AuthenticationPrincipal User currentUser) {
        UserDto userDto = userService.getUserById(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(userDto, "Current user retrieved"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable Long id) {
        UserDto userDto = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(userDto, "User retrieved"));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<UserDto>>> searchUsers(
            @RequestParam String q) {
        List<UserDto> users = userService.searchUsers(q);
        return ResponseEntity.ok(ApiResponse.success(users, "Search results"));
    }

    @PutMapping("/me/profile")
    public ResponseEntity<ApiResponse<UserDto>> updateProfile(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) String displayName,
            @RequestParam(required = false) String avatarUrl) {
        UserDto updated = userService.updateProfile(
                currentUser.getId(), displayName, avatarUrl);
        return ResponseEntity.ok(ApiResponse.success(updated, "Profile updated"));
    }

    @GetMapping("/online")
    public ResponseEntity<ApiResponse<Set<Long>>> getOnlineUsers() {
        Set<Long> onlineIds = userPresenceService.getOnlineUserIds();
        return ResponseEntity.ok(ApiResponse.success(onlineIds, "Online users retrieved"));
    }
}
