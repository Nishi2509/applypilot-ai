package com.applypilot.emailtemplate.dto;

import java.time.Instant;
import java.util.UUID;

public record EmailTemplateResponse(UUID id, String subject, String body, Instant updatedAt) { }
