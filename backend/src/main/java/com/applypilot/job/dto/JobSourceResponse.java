package com.applypilot.job.dto;

import java.time.Instant;
import java.util.UUID;

public record JobSourceResponse(UUID id, String provider, String boardSlug, String displayName, boolean enabled, Instant createdAt) {}
