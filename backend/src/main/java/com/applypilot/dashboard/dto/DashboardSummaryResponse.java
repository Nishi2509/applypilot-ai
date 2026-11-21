package com.applypilot.dashboard.dto;
public record DashboardSummaryResponse(long jobsDiscovered, long jobsMatched, long pendingApprovals, long emailsSent, long interviews, long offers) { }
