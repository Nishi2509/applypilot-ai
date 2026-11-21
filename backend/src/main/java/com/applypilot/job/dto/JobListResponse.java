package com.applypilot.job.dto;
import java.util.List;
public record JobListResponse(List<JobResponse> items, long total, int limit, int offset) { }
