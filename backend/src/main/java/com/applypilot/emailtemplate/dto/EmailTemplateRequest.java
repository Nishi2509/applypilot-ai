package com.applypilot.emailtemplate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EmailTemplateRequest(
    @NotBlank @Size(max = 998) @Pattern(regexp = "^[^\\r\\n]*$", message = "must not contain a line break") String subject,
    @NotBlank String body
) { }
