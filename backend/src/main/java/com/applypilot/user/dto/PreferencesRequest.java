package com.applypilot.user.dto;
import jakarta.validation.constraints.*;
import java.util.List;
public record PreferencesRequest(@NotEmpty List<@NotBlank String> roles, @NotEmpty List<@NotBlank String> locations, @NotEmpty List<@NotBlank String> workModes, @Min(0) @Max(50) int experienceMin, @Min(0) @Max(50) int experienceMax, boolean manualApprovalRequired) {
    @AssertTrue(message = "experienceMax must be at least experienceMin") public boolean isExperienceRangeValid() { return experienceMax >= experienceMin; }
}
