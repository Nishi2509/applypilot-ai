package com.applypilot.application.dto;

import java.util.List;

public record GoogleSheetDraftImportResponse(List<EmailDraftResponse> drafts, List<String> skippedRows) { }
