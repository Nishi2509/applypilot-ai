package com.applypilot.dashboard.service;

import com.applypilot.dashboard.dto.DashboardSummaryResponse;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {
    private final JdbcTemplate jdbc;
    public DashboardService(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public DashboardSummaryResponse summary(UUID userId) { return new DashboardSummaryResponse(count("SELECT COUNT(*) FROM jobs"), count("SELECT COUNT(*) FROM job_matches WHERE user_id = ?", userId), count("SELECT COUNT(*) FROM applications WHERE user_id = ? AND status = 'PENDING_APPROVAL'", userId), count("SELECT COUNT(*) FROM applications WHERE user_id = ? AND status = 'SENT'", userId), count("SELECT COUNT(*) FROM applications WHERE user_id = ? AND status = 'INTERVIEW'", userId), count("SELECT COUNT(*) FROM applications WHERE user_id = ? AND status = 'OFFER'", userId)); }
    private long count(String sql, Object... args) { Long result = jdbc.queryForObject(sql, Long.class, args); return result == null ? 0 : result; }
}
