package com.applypilot.job.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record JobSourceRequest(
    @NotBlank @Pattern(regexp = "GREENHOUSE|LEVER", message = "Provider must be GREENHOUSE or LEVER") String provider,
    @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,255}", message = "Use the board identifier from the careers URL") String boardSlug,
    @NotBlank @Pattern(regexp = ".{1,255}") String displayName
) {}
