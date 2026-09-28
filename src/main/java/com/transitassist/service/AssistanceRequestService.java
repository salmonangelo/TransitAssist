package com.transitassist.service;

import com.transitassist.dto.AssistanceRequestResponse;
import com.transitassist.dto.CreateAssistanceRequest;
import com.transitassist.dto.UpdateAssistanceRequest;
import com.transitassist.entity.AssistanceRequest;
import com.transitassist.entity.Helper;
import com.transitassist.entity.User;
import com.transitassist.enums.RequestStatus;
import com.transitassist.exception.BusinessRuleException;
import com.transitassist.exception.ResourceNotFoundException;
import com.transitassist.repository.AssistanceRequestRepository;
import com.transitassist.repository.HelperRepository;
import com.transitassist.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * All business rules of TransitAssist live in this class (not in controllers, not only in the database).
 */
@Service
@Transactional
public class AssistanceRequestService {

    /** A helper is "busy" for requests in these statuses. */
    private static final List<RequestStatus> BLOCKING_STATUSES =
            List.of(RequestStatus.ASSIGNED, RequestStatus.COMPLETED);

    private final AssistanceRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final HelperRepository helperRepository;
    private final int durationMinutes;

    public AssistanceRequestService(AssistanceRequestRepository requestRepository,
                                    UserRepository userRepository,
                                    HelperRepository helperRepository,
                                    @Value("${transitassist.assistance-duration-minutes:60}") int durationMinutes) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.helperRepository = helperRepository;
        this.durationMinutes = durationMinutes;
    }

    // ------------------------------------------------------------------ create / read

    public AssistanceRequestResponse create(CreateAssistanceRequest dto) {
        User user = userRepository.findById(dto.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + dto.userId()));
        if (!user.isActive()) {
            throw new BusinessRuleException("Inactive users cannot create assistance requests.");
        }
        validateTripInFuture(dto.tripDate(), dto.tripTime());   // RULE 8

        AssistanceRequest request = new AssistanceRequest();
        request.setUser(user);
        request.setPickupPoint(dto.pickupPoint().trim());
        request.setDestination(dto.destination().trim());
        request.setTripDate(dto.tripDate());
        request.setTripTime(dto.tripTime());
        request.setAssistanceType(dto.assistanceType());
        request.setAdditionalNotes(dto.additionalNotes());
        request.setStatus(RequestStatus.REQUESTED);
        return AssistanceRequestResponse.from(requestRepository.save(request));
    }

    @Transactional(readOnly = true)
    public List<AssistanceRequestResponse> getAll(RequestStatus status, Long userId, LocalDate date) {
        List<AssistanceRequest> result;
        if (status != null && date != null) {
            result = requestRepository.findByStatusAndTripDate(status, date);
        } else if (status != null) {
            result = requestRepository.findByStatus(status);
        } else if (userId != null) {
            result = requestRepository.findByUserId(userId);
        } else if (date != null) {
            result = requestRepository.findByTripDate(date);
        } else {
            result = requestRepository.findAll(Sort.by("tripDate", "tripTime"));
        }
        return result.stream().map(AssistanceRequestResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AssistanceRequestResponse getById(Long id) {
        return AssistanceRequestResponse.from(findRequest(id));
    }

    // ------------------------------------------------------------------ update

    public AssistanceRequestResponse update(Long id, UpdateAssistanceRequest dto) {
        AssistanceRequest request = findRequest(id);

        // RULE 5: completed requests are final. (Cancelled ones are final too.)
        if (request.getStatus() == RequestStatus.COMPLETED) {
            throw new BusinessRuleException("Completed requests are final and cannot be modified.");
        }
        if (request.getStatus() == RequestStatus.CANCELLED) {
            throw new BusinessRuleException("Cancelled requests cannot be modified.");
        }

        boolean timeChanged = dto.tripDate() != null || dto.tripTime() != null;
        if (dto.pickupPoint() != null) {
            request.setPickupPoint(dto.pickupPoint().trim());
        }
        if (dto.destination() != null) {
            request.setDestination(dto.destination().trim());
        }
        if (dto.tripDate() != null) {
            request.setTripDate(dto.tripDate());
        }
        if (dto.tripTime() != null) {
            request.setTripTime(dto.tripTime());
        }
        if (dto.assistanceType() != null) {
            request.setAssistanceType(dto.assistanceType());
        }
        if (dto.additionalNotes() != null) {
            request.setAdditionalNotes(dto.additionalNotes());
        }

        if (timeChanged) {
            validateTripInFuture(request.getTripDate(), request.getTripTime());
            // An already-assigned request must not be moved into another booking of the same helper.
            if (request.getHelper() != null) {
                ensureNoOverlap(request.getHelper(), request);
            }
        }
        return AssistanceRequestResponse.from(requestRepository.saveAndFlush(request));
    }

    // ------------------------------------------------------------------ assign

    public AssistanceRequestResponse assign(Long requestId, Long helperId) {
        AssistanceRequest request = findRequest(requestId);

        // RULE 3 and RULE 6
        if (request.getStatus() == RequestStatus.COMPLETED) {
            throw new BusinessRuleException("Completed requests cannot be reassigned to another helper.");
        }
        if (request.getStatus() == RequestStatus.CANCELLED) {
            throw new BusinessRuleException("Cancelled requests cannot be assigned to a helper.");
        }

        // Lock the helper row so two simultaneous assignments cannot both pass the overlap check.
        Helper helper = helperRepository.findByIdForUpdate(helperId)
                .orElseThrow(() -> new ResourceNotFoundException("Helper not found with id " + helperId));

        // RULE 7
        if (!helper.isActive()) {
            throw new BusinessRuleException("Helper is inactive and cannot be assigned.");
        }
        if (!helper.isAvailable()) {
            throw new BusinessRuleException("Helper is not available for assignment.");
        }
        if (request.getStatus() == RequestStatus.ASSIGNED
                && request.getHelper() != null
                && request.getHelper().getId().equals(helperId)) {
            throw new BusinessRuleException("Request is already assigned to this helper.");
        }

        // RULE 1
        ensureNoOverlap(helper, request);

        if (request.getStatus() == RequestStatus.REQUESTED) {
            requireTransition(request.getStatus(), RequestStatus.ASSIGNED);
            request.setStatus(RequestStatus.ASSIGNED);
        }
        // (an ASSIGNED request that is handed to a different helper simply stays ASSIGNED)
        request.setHelper(helper);
        return AssistanceRequestResponse.from(requestRepository.saveAndFlush(request));
    }

    // ------------------------------------------------------------------ complete / cancel

    public AssistanceRequestResponse complete(Long id) {
        AssistanceRequest request = findRequest(id);

        // RULE 4
        if (request.getStatus() == RequestStatus.CANCELLED) {
            throw new BusinessRuleException("Cancelled requests cannot be marked as completed.");
        }
        if (request.getStatus() == RequestStatus.COMPLETED) {
            throw new BusinessRuleException("Request is already completed.");
        }
        if (request.getStatus() == RequestStatus.REQUESTED) {
            throw new BusinessRuleException("A helper must be assigned before the request can be completed.");
        }
        requireTransition(request.getStatus(), RequestStatus.COMPLETED);
        request.setStatus(RequestStatus.COMPLETED);
        return AssistanceRequestResponse.from(requestRepository.saveAndFlush(request));
    }

    public AssistanceRequestResponse cancel(Long id) {
        AssistanceRequest request = findRequest(id);

        // RULE 2
        if (request.getStatus() == RequestStatus.COMPLETED) {
            throw new BusinessRuleException("Completed requests cannot be cancelled.");
        }
        if (request.getStatus() == RequestStatus.CANCELLED) {
            throw new BusinessRuleException("Request is already cancelled.");
        }
        requireTransition(request.getStatus(), RequestStatus.CANCELLED);
        request.setStatus(RequestStatus.CANCELLED);
        return AssistanceRequestResponse.from(requestRepository.saveAndFlush(request));
    }

    // ------------------------------------------------------------------ helpers (private)

    private AssistanceRequest findRequest(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assistance request not found with id " + id));
    }

    /** Generic guard that uses the transition table defined in {@link RequestStatus}. */
    private void requireTransition(RequestStatus from, RequestStatus to) {
        if (!from.canTransitionTo(to)) {
            throw new BusinessRuleException("Invalid status transition from " + from + " to " + to + ".");
        }
    }

    /** RULE 8: date and time must exist and must not be in the past. */
    private void validateTripInFuture(LocalDate date, LocalTime time) {
        if (date == null || time == null) {
            throw new BusinessRuleException("Trip date and trip time are required.");
        }
        if (LocalDateTime.of(date, time).isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("Trip date and time must not be in the past.");
        }
    }

    /**
     * RULE 1: a helper cannot have two overlapping requests.
     * Every request occupies a slot of {@code durationMinutes} starting at its trip time.
     * Two slots overlap when each one starts before the other one ends.
     */
    private void ensureNoOverlap(Helper helper, AssistanceRequest candidate) {
        LocalDateTime start = LocalDateTime.of(candidate.getTripDate(), candidate.getTripTime());
        LocalDateTime end = start.plusMinutes(durationMinutes);

        List<AssistanceRequest> existing =
                requestRepository.findByHelperIdAndStatusIn(helper.getId(), BLOCKING_STATUSES);
        for (AssistanceRequest other : existing) {
            if (other.getId().equals(candidate.getId())) {
                continue;
            }
            LocalDateTime otherStart = LocalDateTime.of(other.getTripDate(), other.getTripTime());
            LocalDateTime otherEnd = otherStart.plusMinutes(durationMinutes);
            if (start.isBefore(otherEnd) && otherStart.isBefore(end)) {
                throw new BusinessRuleException(
                        "Helper is already assigned to another request at the requested time.",
                        HttpStatus.CONFLICT);
            }
        }
    }
}
