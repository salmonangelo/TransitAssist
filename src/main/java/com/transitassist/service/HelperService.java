package com.transitassist.service;

import com.transitassist.dto.AssistanceRequestResponse;
import com.transitassist.dto.CreateHelperRequest;
import com.transitassist.dto.HelperResponse;
import com.transitassist.dto.HelperWorkloadResponse;
import com.transitassist.dto.UpdateHelperRequest;
import com.transitassist.entity.AssistanceRequest;
import com.transitassist.entity.Helper;
import com.transitassist.enums.RequestStatus;
import com.transitassist.exception.BusinessRuleException;
import com.transitassist.exception.ResourceNotFoundException;
import com.transitassist.repository.AssistanceRequestRepository;
import com.transitassist.repository.HelperRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class HelperService {

    /** Requests that really occupy a helper's time (cancelled ones do not). */
    private static final List<RequestStatus> WORKLOAD_STATUSES =
            List.of(RequestStatus.ASSIGNED, RequestStatus.COMPLETED);

    private final HelperRepository helperRepository;
    private final AssistanceRequestRepository requestRepository;

    public HelperService(HelperRepository helperRepository, AssistanceRequestRepository requestRepository) {
        this.helperRepository = helperRepository;
        this.requestRepository = requestRepository;
    }

    public HelperResponse create(CreateHelperRequest request) {
        if (helperRepository.existsByEmail(request.email().trim().toLowerCase())) {
            throw new BusinessRuleException("A helper with this email already exists.", HttpStatus.CONFLICT);
        }
        Helper helper = new Helper();
        helper.setName(request.name().trim());
        helper.setEmail(request.email().trim().toLowerCase());
        helper.setPhone(request.phone());
        helper.setAssistanceSkills(request.assistanceSkills());
        helper.setAvailable(request.available() == null || request.available());
        return HelperResponse.from(helperRepository.save(helper));
    }

    @Transactional(readOnly = true)
    public List<HelperResponse> getAll() {
        return helperRepository.findAll().stream().map(HelperResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public HelperResponse getById(Long id) {
        return HelperResponse.from(findHelper(id));
    }

    public HelperResponse update(Long id, UpdateHelperRequest request) {
        Helper helper = findHelper(id);
        String email = request.email().trim().toLowerCase();
        if (helperRepository.existsByEmailAndIdNot(email, id)) {
            throw new BusinessRuleException("Another helper already uses this email.", HttpStatus.CONFLICT);
        }
        helper.setName(request.name().trim());
        helper.setEmail(email);
        helper.setPhone(request.phone());
        helper.setAssistanceSkills(request.assistanceSkills());
        if (request.available() != null) {
            helper.setAvailable(request.available());
        }
        if (request.active() != null) {
            helper.setActive(request.active());
        }
        return HelperResponse.from(helperRepository.saveAndFlush(helper));
    }

    /** "Delete" is a soft delete: the helper becomes inactive and unavailable. */
    public void deactivate(Long id) {
        Helper helper = findHelper(id);
        helper.setActive(false);
        helper.setAvailable(false);
        helperRepository.save(helper);
    }

    @Transactional(readOnly = true)
    public HelperWorkloadResponse getWorkload(Long helperId, LocalDate date) {
        Helper helper = findHelper(helperId);
        List<AssistanceRequestResponse> requests = requestRepository
                .findByHelperIdAndTripDateAndStatusInOrderByTripTime(helperId, date, WORKLOAD_STATUSES)
                .stream()
                .map(AssistanceRequestResponse::from)
                .toList();
        long assigned = requests.stream().filter(r -> r.status() == RequestStatus.ASSIGNED).count();
        long completed = requests.stream().filter(r -> r.status() == RequestStatus.COMPLETED).count();
        return new HelperWorkloadResponse(helper.getId(), helper.getName(), date,
                requests.size(), assigned, completed, requests);
    }

    private Helper findHelper(Long id) {
        return helperRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Helper not found with id " + id));
    }
}
