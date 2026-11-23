package com.applypilot.application.dto;

import java.util.UUID;

public record EmailDraftResponse(UUID applicationId, String recipientEmail, String subject, String body, String attachmentMode, String attachmentNote, String status, int emailVersion, UUID masterResumeId) {}
