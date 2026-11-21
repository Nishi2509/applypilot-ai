package com.applypilot.job.dto;
import java.time.Instant;
import java.util.UUID;
public record JobResponse(UUID id, String companyName, String title, String location, String workMode, Double experienceMin, Double experienceMax, String applicationUrl, String source, Instant postedAt, Instant discoveredAt, String observedStatus) { }
