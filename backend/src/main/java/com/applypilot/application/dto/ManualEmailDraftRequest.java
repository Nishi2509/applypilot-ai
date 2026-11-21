package com.applypilot.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ManualEmailDraftRequest(@NotBlank String companyName, @Email @NotBlank String hrEmail, @NotBlank String position, String location, String jobDescription) {}
