package com.transitassist;

import com.transitassist.dto.AssistanceRequestResponse;
import com.transitassist.dto.CreateAssistanceRequest;
import com.transitassist.dto.CreateHelperRequest;
import com.transitassist.dto.CreateUserRequest;
import com.transitassist.dto.HelperResponse;
import com.transitassist.dto.HelperWorkloadResponse;
import com.transitassist.dto.UpdateAssistanceRequest;
import com.transitassist.dto.UserResponse;
import com.transitassist.enums.AssistanceType;
import com.transitassist.enums.RequestStatus;
import com.transitassist.enums.Role;
import com.transitassist.exception.BusinessRuleException;
import com.transitassist.exception.ResourceNotFoundException;
import com.transitassist.service.AssistanceRequestService;
import com.transitassist.service.HelperService;
import com.transitassist.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AssistanceRequestServiceTest {

    @Autowired
    private UserService userService;
    @Autowired
    private HelperService helperService;
    @Autowired
    private AssistanceRequestService requestService;

    private final LocalDate tripDate = LocalDate.now().plusDays(3);
    private UserResponse user;
    private HelperResponse helper;

    @BeforeEach
    void setUp() {
        user = userService.create(new CreateUserRequest(
                "Test User", TestData.uniqueEmail("user"), "9876543210", "secret123", Role.USER));
        helper = helperService.create(new CreateHelperRequest(
                "Test Helper", TestData.uniqueEmail("helper"), "9123456780",
                Set.of(AssistanceType.WHEELCHAIR), true));
    }

    private AssistanceRequestResponse newRequest(LocalTime time) {
        return requestService.create(new CreateAssistanceRequest(
                user.id(), "Main Gate", "Library", tripDate, time, AssistanceType.WHEELCHAIR, "Test notes"));
    }

    @Test
    void createRequest_startsInRequestedStatusWithoutHelper() {
        AssistanceRequestResponse request = newRequest(LocalTime.of(10, 0));

        assertNotNull(request.id());
        assertEquals(RequestStatus.REQUESTED, request.status());
        assertNull(request.helperId());
        assertEquals(user.id(), request.userId());
    }

    @Test
    void createRequest_inThePastIsRejected() {
        CreateAssistanceRequest past = new CreateAssistanceRequest(user.id(), "A", "B",
                LocalDate.now().minusDays(1), LocalTime.of(9, 0), AssistanceType.ESCORT, null);

        assertThrows(BusinessRuleException.class, () -> requestService.create(past));
    }

    @Test
    void createRequest_unknownUserReturnsNotFound() {
        CreateAssistanceRequest unknownUser = new CreateAssistanceRequest(999_999L, "A", "B",
                tripDate, LocalTime.of(9, 0), AssistanceType.ESCORT, null);

        assertThrows(ResourceNotFoundException.class, () -> requestService.create(unknownUser));
    }

    @Test
    void assignHelper_succeedsAndMovesRequestToAssigned() {
        AssistanceRequestResponse request = newRequest(LocalTime.of(10, 0));

        AssistanceRequestResponse assigned = requestService.assign(request.id(), helper.id());

        assertEquals(RequestStatus.ASSIGNED, assigned.status());
        assertEquals(helper.id(), assigned.helperId());
    }

    @Test
    void assignHelper_overlappingRequestIsRejectedWithConflict() {
        AssistanceRequestResponse first = newRequest(LocalTime.of(10, 0));
        AssistanceRequestResponse overlapping = newRequest(LocalTime.of(10, 30));
        requestService.assign(first.id(), helper.id());

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> requestService.assign(overlapping.id(), helper.id()));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("Helper is already assigned to another request at the requested time.", ex.getMessage());
        assertEquals(RequestStatus.REQUESTED, requestService.getById(overlapping.id()).status());
    }

    @Test
    void assignHelper_nonOverlappingRequestIsAccepted() {
        AssistanceRequestResponse first = newRequest(LocalTime.of(10, 0));
        AssistanceRequestResponse later = newRequest(LocalTime.of(11, 0));
        requestService.assign(first.id(), helper.id());

        AssistanceRequestResponse assigned = requestService.assign(later.id(), helper.id());

        assertEquals(RequestStatus.ASSIGNED, assigned.status());
    }

    @Test
    void assignHelper_inactiveOrUnavailableHelperIsRejected() {
        AssistanceRequestResponse request = newRequest(LocalTime.of(10, 0));
        helperService.deactivate(helper.id());

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> requestService.assign(request.id(), helper.id()));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void cancelRequest_movesRequestedToCancelled() {
        AssistanceRequestResponse request = newRequest(LocalTime.of(10, 0));

        AssistanceRequestResponse cancelled = requestService.cancel(request.id());

        assertEquals(RequestStatus.CANCELLED, cancelled.status());
    }

    @Test
    void cancelRequest_completedRequestCannotBeCancelled() {
        AssistanceRequestResponse request = newRequest(LocalTime.of(10, 0));
        requestService.assign(request.id(), helper.id());
        requestService.complete(request.id());

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> requestService.cancel(request.id()));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("Completed requests cannot be cancelled.", ex.getMessage());
    }

    @Test
    void assignHelper_completedRequestCannotBeReassigned() {
        AssistanceRequestResponse request = newRequest(LocalTime.of(10, 0));
        requestService.assign(request.id(), helper.id());
        requestService.complete(request.id());
        HelperResponse other = helperService.create(new CreateHelperRequest(
                "Other Helper", TestData.uniqueEmail("other"), null, Set.of(AssistanceType.WHEELCHAIR), true));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> requestService.assign(request.id(), other.id()));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void assignHelper_cancelledRequestCannotBeAssigned() {
        AssistanceRequestResponse request = newRequest(LocalTime.of(10, 0));
        requestService.cancel(request.id());

        assertThrows(BusinessRuleException.class, () -> requestService.assign(request.id(), helper.id()));
    }

    @Test
    void completeRequest_cancelledRequestCannotBeCompleted() {
        AssistanceRequestResponse request = newRequest(LocalTime.of(10, 0));
        requestService.cancel(request.id());

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> requestService.complete(request.id()));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void completeRequest_requiresAnAssignedHelperFirst() {
        AssistanceRequestResponse request = newRequest(LocalTime.of(10, 0));

        assertThrows(BusinessRuleException.class, () -> requestService.complete(request.id()));
    }

    @Test
    void updateRequest_completedRequestCannotBeModified() {
        AssistanceRequestResponse request = newRequest(LocalTime.of(10, 0));
        requestService.assign(request.id(), helper.id());
        requestService.complete(request.id());
        UpdateAssistanceRequest change = new UpdateAssistanceRequest(
                null, null, null, LocalTime.of(12, 0), AssistanceType.ESCORT, null);

        assertThrows(BusinessRuleException.class, () -> requestService.update(request.id(), change));
    }

    @Test
    void helperWorkload_listsAssignedAndCompletedRequestsForTheDay() {
        AssistanceRequestResponse first = newRequest(LocalTime.of(10, 0));
        AssistanceRequestResponse second = newRequest(LocalTime.of(12, 0));
        AssistanceRequestResponse cancelled = newRequest(LocalTime.of(14, 0));
        requestService.assign(first.id(), helper.id());
        requestService.assign(second.id(), helper.id());
        requestService.complete(first.id());
        requestService.cancel(cancelled.id());

        HelperWorkloadResponse workload = helperService.getWorkload(helper.id(), tripDate);

        assertEquals(2, workload.totalRequests());
        assertEquals(1, workload.assignedCount());
        assertEquals(1, workload.completedCount());
        assertEquals(LocalTime.of(10, 0), workload.requests().get(0).tripTime());
    }

    @Test
    void helperWorkload_isEmptyOnADayWithoutRequests() {
        HelperWorkloadResponse workload = helperService.getWorkload(helper.id(), tripDate.plusDays(10));

        assertEquals(0, workload.totalRequests());
        assertTrue(workload.requests().isEmpty());
    }
}
