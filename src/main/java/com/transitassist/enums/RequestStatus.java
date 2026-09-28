package com.transitassist.enums;

/**
 * Life cycle of an assistance request.
 *
 * REQUESTED -> ASSIGNED -> COMPLETED
 * REQUESTED -> CANCELLED
 * ASSIGNED  -> CANCELLED
 *
 * COMPLETED and CANCELLED are final states.
 */
public enum RequestStatus {
    REQUESTED,
    ASSIGNED,
    COMPLETED,
    CANCELLED;

    public boolean canTransitionTo(RequestStatus target) {
        return switch (this) {
            case REQUESTED -> target == ASSIGNED || target == CANCELLED;
            case ASSIGNED -> target == COMPLETED || target == CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
    }
}
