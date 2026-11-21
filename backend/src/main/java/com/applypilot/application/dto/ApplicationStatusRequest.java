package com.applypilot.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ApplicationStatusRequest(@NotBlank @Pattern(regexp = "APPLIED|INTERVIEW|REJECTED|SUCCESS", message = "Status must be APPLIED, INTERVIEW, REJECTED, or SUCCESS") String status) {}
