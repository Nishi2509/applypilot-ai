package com.applypilot.emailtemplate.controller;

import com.applypilot.emailtemplate.dto.EmailTemplateRequest;
import com.applypilot.emailtemplate.dto.EmailTemplateResponse;
import com.applypilot.emailtemplate.service.EmailTemplateService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/email-templates")
public class EmailTemplateController {
    private final EmailTemplateService service;
    public EmailTemplateController(EmailTemplateService service) { this.service = service; }
    @GetMapping public EmailTemplateResponse get(Authentication auth) { return service.get(userId(auth)); }
    @PutMapping public EmailTemplateResponse save(Authentication auth, @Valid @RequestBody EmailTemplateRequest request) { return service.save(userId(auth), request); }
    private UUID userId(Authentication auth) { return (UUID) auth.getPrincipal(); }
}
