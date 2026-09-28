package com.transitassist.dto;

import com.transitassist.enums.AssistanceType;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record CreateAssistanceRequest(
        @NotNull(message = "User id is required")
        Long userId,

        @NotBlank(message = "Pickup point is required")
        @Size(max = 150, message = "Pickup point must be at most 150 characters")
        String pickupPoint,

        @NotBlank(message = "Destination is required")
        @Size(max = 150, message = "Destination must be at most 150 characters")
        String destination,

        @NotNull(message = "Trip date is required")
        @FutureOrPresent(message = "Trip date must be today or later")
        LocalDate tripDate,

        @NotNull(message = "Trip time is required")
        LocalTime tripTime,

        @NotNull(message = "Assistance type is required")
        AssistanceType assistanceType,

        @Size(max = 500, message = "Notes must be at most 500 characters")
        String additionalNotes) {
}
