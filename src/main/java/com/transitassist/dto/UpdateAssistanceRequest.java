package com.transitassist.dto;

import com.transitassist.enums.AssistanceType;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

/** Partial update: any field that is null is left unchanged. */
public record UpdateAssistanceRequest(
        @Pattern(regexp = ".*\\S.*", message = "Pickup point must not be blank")
        @Size(max = 150, message = "Pickup point must be at most 150 characters")
        String pickupPoint,

        @Pattern(regexp = ".*\\S.*", message = "Destination must not be blank")
        @Size(max = 150, message = "Destination must be at most 150 characters")
        String destination,

        @FutureOrPresent(message = "Trip date must be today or later")
        LocalDate tripDate,

        LocalTime tripTime,

        AssistanceType assistanceType,

        @Size(max = 500, message = "Notes must be at most 500 characters")
        String additionalNotes) {
}
