package com.applypilot.application.dto;

import java.time.Instant;
import java.util.UUID;

public record ApplicationResponse(UUID id, String companyName, String position, String location, String status, boolean manualEntry, Instant createdAt, Instant updatedAt) {}
