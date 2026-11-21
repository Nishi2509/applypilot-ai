package com.applypilot.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ManualApplicationRequest(
    @NotBlank @Pattern(regexp = ".{1,255}") String companyName,
    @NotBlank @Pattern(regexp = ".{1,500}") String position,
    @NotBlank @Pattern(regexp = ".{1,255}") String location,
    @NotBlank @Pattern(regexp = "APPLIED|INTERVIEW|REJECTED|SUCCESS", message = "Status must be APPLIED, INTERVIEW, REJECTED, or SUCCESS") String status
) {}
