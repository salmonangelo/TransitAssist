package com.transitassist.dto;

import com.transitassist.entity.AssistanceRequest;
import com.transitassist.enums.AssistanceType;
import com.transitassist.enums.RequestStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record AssistanceRequestResponse(
        Long id,
        Long userId,
        String userName,
        Long helperId,
        String helperName,
        String pickupPoint,
        String destination,
        LocalDate tripDate,
        LocalTime tripTime,
        AssistanceType assistanceType,
        String additionalNotes,
        RequestStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    /** Must be called inside a transaction because user/helper are loaded lazily. */
    public static AssistanceRequestResponse from(AssistanceRequest r) {
        return new AssistanceRequestResponse(
                r.getId(),
                r.getUser().getId(),
                r.getUser().getName(),
                r.getHelper() == null ? null : r.getHelper().getId(),
                r.getHelper() == null ? null : r.getHelper().getName(),
                r.getPickupPoint(),
                r.getDestination(),
                r.getTripDate(),
                r.getTripTime(),
                r.getAssistanceType(),
                r.getAdditionalNotes(),
                r.getStatus(),
                r.getCreatedAt(),
                r.getUpdatedAt());
    }
}
