package com.transitassist.controller;

import com.transitassist.dto.CreateHelperRequest;
import com.transitassist.dto.HelperResponse;
import com.transitassist.dto.HelperWorkloadResponse;
import com.transitassist.dto.UpdateHelperRequest;
import com.transitassist.service.HelperService;
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
@RequestMapping("/api/helpers")
@Tag(name = "Helpers")
public class HelperController {

    private final HelperService helperService;

    public HelperController(HelperService helperService) {
        this.helperService = helperService;
    }

    @Operation(summary = "Create a helper")
    @PostMapping
    public ResponseEntity<HelperResponse> create(@Valid @RequestBody CreateHelperRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(helperService.create(request));
    }

    @Operation(summary = "List all helpers")
    @GetMapping
    public List<HelperResponse> getAll() {
        return helperService.getAll();
    }

    @Operation(summary = "Get a helper by id")
    @GetMapping("/{id}")
    public HelperResponse getById(@PathVariable Long id) {
        return helperService.getById(id);
    }

    @Operation(summary = "Update a helper")
    @PutMapping("/{id}")
    public HelperResponse update(@PathVariable Long id, @Valid @RequestBody UpdateHelperRequest request) {
        return helperService.update(id, request);
    }

    @Operation(summary = "Deactivate (soft delete) a helper")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        helperService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Helper workload for one day (date=YYYY-MM-DD)")
    @GetMapping("/{id}/workload")
    public HelperWorkloadResponse workload(@PathVariable Long id,
                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return helperService.getWorkload(id, date);
    }
}
