package com.applypilot.user.dto;
import java.util.List;
public record PreferencesResponse(List<String> roles, List<String> locations, List<String> workModes, int experienceMin, int experienceMax, boolean manualApprovalRequired) { }
