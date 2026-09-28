package com.transitassist.repository;

import com.transitassist.entity.AssistanceRequest;
import com.transitassist.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface AssistanceRequestRepository extends JpaRepository<AssistanceRequest, Long> {

    List<AssistanceRequest> findByStatus(RequestStatus status);

    List<AssistanceRequest> findByUserId(Long userId);

    List<AssistanceRequest> findByHelperId(Long helperId);

    List<AssistanceRequest> findByTripDate(LocalDate tripDate);

    List<AssistanceRequest> findByHelperIdAndTripDate(Long helperId, LocalDate tripDate);

    List<AssistanceRequest> findByStatusAndTripDate(RequestStatus status, LocalDate tripDate);

    /** Requests of a helper that are in one of the given statuses (used for the overlap check). */
    List<AssistanceRequest> findByHelperIdAndStatusIn(Long helperId, Collection<RequestStatus> statuses);

    /** Helper workload for one day, ordered by time. */
    List<AssistanceRequest> findByHelperIdAndTripDateAndStatusInOrderByTripTime(
            Long helperId, LocalDate tripDate, Collection<RequestStatus> statuses);
}
