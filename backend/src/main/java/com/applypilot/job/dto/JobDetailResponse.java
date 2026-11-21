package com.applypilot.job.dto;
import java.time.Instant;
import java.util.UUID;
public record JobDetailResponse(UUID id, String companyName, String title, String description, String location, String workMode, Double experienceMin, Double experienceMax, String applicationUrl, String sourceUrl, String source, Instant postedAt, Instant discoveredAt, String observedStatus) { }
