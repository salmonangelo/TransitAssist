package com.transitassist.dto;

import com.transitassist.entity.Helper;
import com.transitassist.enums.AssistanceType;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.TreeSet;

public record HelperResponse(
        Long id,
        String name,
        String email,
        String phone,
        Set<AssistanceType> assistanceSkills,
        boolean available,
        boolean active,
        LocalDateTime createdAt) {

    public static HelperResponse from(Helper helper) {
        return new HelperResponse(helper.getId(), helper.getName(), helper.getEmail(), helper.getPhone(),
                new TreeSet<>(helper.getAssistanceSkills()), helper.isAvailable(), helper.isActive(),
                helper.getCreatedAt());
    }
}
