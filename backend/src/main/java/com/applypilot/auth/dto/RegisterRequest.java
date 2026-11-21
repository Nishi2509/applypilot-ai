package com.applypilot.auth.dto;
import jakarta.validation.constraints.*;
public record RegisterRequest(@NotBlank @Size(max = 120) String displayName, @NotBlank @Email @Size(max = 320) String email, @NotBlank @Size(min = 12, max = 72) String password) { }
