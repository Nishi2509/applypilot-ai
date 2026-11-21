package com.applypilot.job.dto;

import java.util.List;

public record JobSearchResponse(int sourcesSearched, int jobsDiscovered, int jobsUpdated, List<String> errors) {}
