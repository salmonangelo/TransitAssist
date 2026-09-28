package com.transitassist.controller;

import com.transitassist.dto.AssignHelperRequest;
import com.transitassist.dto.AssistanceRequestResponse;
import com.transitassist.dto.CreateAssistanceRequest;
import com.transitassist.dto.UpdateAssistanceRequest;
import com.transitassist.enums.RequestStatus;
import com.transitassist.service.AssistanceRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/assistance-requests")
@Tag(name = "Assistance Requests")
public class AssistanceRequestController {

    private final AssistanceRequestService requestService;

    public AssistanceRequestController(AssistanceRequestService requestService) {
        this.requestService = requestService;
    }

    @Operation(summary = "Create an assistance request")
    @PostMapping
    public ResponseEntity<AssistanceRequestResponse> create(@Valid @RequestBody CreateAssistanceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(requestService.create(request));
    }

    @Operation(summary = "List requests (optional filters: status, userId, date)")
    @GetMapping
    public List<AssistanceRequestResponse> getAll(
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return requestService.getAll(status, userId, date);
    }

    @Operation(summary = "Get a request by id")
    @GetMapping("/{id}")
    public AssistanceRequestResponse getById(@PathVariable Long id) {
        return requestService.getById(id);
    }

    @Operation(summary = "Update a request (not allowed once completed or cancelled)")
    @PutMapping("/{id}")
    public AssistanceRequestResponse update(@PathVariable Long id,
                                            @Valid @RequestBody UpdateAssistanceRequest request) {
        return requestService.update(id, request);
    }

    @Operation(summary = "Cancel a request (same as POST /{id}/cancel)")
    @DeleteMapping("/{id}")
    public AssistanceRequestResponse delete(@PathVariable Long id) {
        return requestService.cancel(id);
    }

    @Operation(summary = "Assign a helper (409 if the helper already has an overlapping request)")
    @PostMapping("/{id}/assign/{helperId}")
    public AssistanceRequestResponse assign(@PathVariable Long id, @PathVariable Long helperId) {
        return requestService.assign(id, helperId);
    }

    @Operation(summary = "Assign a helper using a JSON body {\"helperId\": 1}")
    @PostMapping("/{id}/assign")
    public AssistanceRequestResponse assignWithBody(@PathVariable Long id,
                                                    @Valid @RequestBody AssignHelperRequest request) {
        return requestService.assign(id, request.helperId());
    }

    @Operation(summary = "Mark an ASSIGNED request as COMPLETED")
    @PostMapping("/{id}/complete")
    public AssistanceRequestResponse complete(@PathVariable Long id) {
        return requestService.complete(id);
    }

    @Operation(summary = "Cancel a request (rejected if already COMPLETED)")
    @PostMapping("/{id}/cancel")
    public AssistanceRequestResponse cancel(@PathVariable Long id) {
        return requestService.cancel(id);
    }
}
