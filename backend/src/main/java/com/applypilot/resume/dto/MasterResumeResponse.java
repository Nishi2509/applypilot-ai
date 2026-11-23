package com.applypilot.resume.dto;

import java.time.Instant;
import java.util.UUID;

public record MasterResumeResponse(UUID id, int versionNumber, String pdfFileName, String texFileName, String targetRoles, Instant uploadedAt) {}
