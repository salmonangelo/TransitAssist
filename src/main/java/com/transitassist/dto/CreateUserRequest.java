package com.transitassist.dto;

import com.transitassist.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        @Size(max = 150, message = "Email must be at most 150 characters")
        String email,

        @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Phone must contain 7 to 15 digits (optional leading +)")
        String phone,

        @Size(min = 6, max = 60, message = "Password must be 6 to 60 characters")
        String password,

        Role role) {
}
