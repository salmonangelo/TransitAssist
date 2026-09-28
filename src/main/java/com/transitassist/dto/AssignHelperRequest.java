package com.transitassist.dto;

import jakarta.validation.constraints.NotNull;

/** Body for POST /api/assistance-requests/{id}/assign (alternative to the /assign/{helperId} URL). */
public record AssignHelperRequest(
        @NotNull(message = "Helper id is required")
        Long helperId) {
}
