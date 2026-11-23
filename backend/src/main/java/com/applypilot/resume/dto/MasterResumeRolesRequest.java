package com.applypilot.resume.dto;

import jakarta.validation.constraints.Size;

public record MasterResumeRolesRequest(@Size(max = 1000) String targetRoles) { }
