package com.applypilot.application.controller;

import com.applypilot.application.dto.*;
import com.applypilot.application.service.ApplicationService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/applications")
public class ApplicationController {
    private final ApplicationService service;
    public ApplicationController(ApplicationService service) { this.service = service; }
    @GetMapping public List<ApplicationResponse> list(Authentication auth) { return service.list(userId(auth)); }
    @PostMapping("/manual") public ApplicationResponse createManual(Authentication auth, @Valid @RequestBody ManualApplicationRequest request) { return service.createManual(userId(auth), request); }
    @PostMapping("/manual-email-drafts") public EmailDraftResponse createEmailDraft(Authentication auth, @Valid @RequestBody ManualEmailDraftRequest request) { return service.createEmailDraft(userId(auth), request); }
    @GetMapping("/email-drafts") public List<EmailDraftResponse> emailDrafts(Authentication auth) { return service.emailDrafts(userId(auth)); }
    @PostMapping("/{id}/approve-email") public EmailDraftResponse approveEmail(Authentication auth, @PathVariable UUID id) { return service.approveEmailDraft(userId(auth), id); }
    @PostMapping("/{id}/send-email") public EmailDraftResponse sendEmail(Authentication auth, @PathVariable UUID id) { return service.sendApprovedEmail(userId(auth), id); }
    @PatchMapping("/{id}/status") public ApplicationResponse updateStatus(Authentication auth, @PathVariable UUID id, @Valid @RequestBody ApplicationStatusRequest request) { return service.updateStatus(userId(auth), id, request); }
    private UUID userId(Authentication auth) { return (UUID) auth.getPrincipal(); }
}
