package com.applypilot.integration.dto;
public record IntegrationStatusResponse(IntegrationConnection gmail, IntegrationConnection googleSheets, IntegrationConnection aiProvider) {
    public record IntegrationConnection(boolean configured, boolean connected, String detail) { }
}
