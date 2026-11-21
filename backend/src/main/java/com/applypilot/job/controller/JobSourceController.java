package com.applypilot.job.controller;

import com.applypilot.job.dto.JobSearchResponse;
import com.applypilot.job.dto.JobSourceRequest;
import com.applypilot.job.dto.JobSourceResponse;
import com.applypilot.job.service.JobDiscoveryService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/job-sources")
public class JobSourceController {
    private final JobDiscoveryService service;
    public JobSourceController(JobDiscoveryService service) { this.service = service; }

    @GetMapping
    public List<JobSourceResponse> list(Authentication authentication) { return service.listSources(userId(authentication)); }

    @PostMapping
    public JobSourceResponse add(Authentication authentication, @Valid @RequestBody JobSourceRequest request) {
        return service.addSource(userId(authentication), request);
    }

    @DeleteMapping("/{id}")
    public void remove(Authentication authentication, @PathVariable UUID id) { service.removeSource(userId(authentication), id); }

    @PostMapping("/search")
    public JobSearchResponse search(Authentication authentication) { return service.search(userId(authentication)); }

    @PostMapping("/web-search")
    public JobSearchResponse webSearch(Authentication authentication) { return service.webSearch(userId(authentication)); }

    private UUID userId(Authentication authentication) { return (UUID) authentication.getPrincipal(); }
}
