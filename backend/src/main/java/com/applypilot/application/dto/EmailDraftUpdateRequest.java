package com.applypilot.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** The fields a user may change before approving an application email. */
public record EmailDraftUpdateRequest(
    @Email @NotBlank @Size(max = 320) String recipientEmail,
    @NotBlank @Size(max = 998) @Pattern(regexp = "^[^\\r\\n]*$", message = "must not contain a line break") String subject,
    @NotBlank String body
) { }
