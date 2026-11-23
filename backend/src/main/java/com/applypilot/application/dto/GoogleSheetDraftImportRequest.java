package com.applypilot.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GoogleSheetDraftImportRequest(
    @NotBlank @Size(max = 2048) String spreadsheet,
    @NotBlank @Size(max = 500) String range,
    @Size(max = 255) String location
) { }
