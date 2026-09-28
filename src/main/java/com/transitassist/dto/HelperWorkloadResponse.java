package com.transitassist.dto;

import java.time.LocalDate;
import java.util.List;

/** Workload of one helper for one day (cancelled requests are not counted). */
public record HelperWorkloadResponse(
        Long helperId,
        String helperName,
        LocalDate date,
        int totalRequests,
        long assignedCount,
        long completedCount,
        List<AssistanceRequestResponse> requests) {
}
