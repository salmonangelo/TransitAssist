package com.transitassist.dto;

import com.transitassist.entity.User;
import com.transitassist.enums.Role;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String name,
        String email,
        String phone,
        Role role,
        boolean active,
        LocalDateTime createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getPhone(),
                user.getRole(), user.isActive(), user.getCreatedAt());
    }
}
