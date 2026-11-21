package com.applypilot.job.service;

import com.applypilot.common.ApiExceptionHandler.NotFoundException;
import com.applypilot.job.dto.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class JobQueryService {
    private final JdbcTemplate jdbc;
    public JobQueryService(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public JobListResponse list(int limit, int offset) { String sql = "SELECT j.id, c.name company_name, j.title, j.location, j.work_mode, j.experience_min, j.experience_max, j.application_url, j.source, j.posted_at, j.discovered_at, j.observed_status FROM jobs j LEFT JOIN companies c ON c.id=j.company_id ORDER BY j.discovered_at DESC LIMIT ? OFFSET ?"; return new JobListResponse(jdbc.query(sql, (rs, row) -> summary(rs), limit, offset), count(), limit, offset); }
    public JobDetailResponse detail(UUID id) { try { return jdbc.queryForObject("SELECT j.id, c.name company_name, j.title, j.description_text, j.location, j.work_mode, j.experience_min, j.experience_max, j.application_url, j.source_url, j.source, j.posted_at, j.discovered_at, j.observed_status FROM jobs j LEFT JOIN companies c ON c.id=j.company_id WHERE j.id=?", (rs, row) -> detail(rs), idToBytes(id)); } catch (org.springframework.dao.EmptyResultDataAccessException e) { throw new NotFoundException("Job not found"); } }
    private long count() { Long count = jdbc.queryForObject("SELECT COUNT(*) FROM jobs", Long.class); return count == null ? 0 : count; }
    private JobResponse summary(ResultSet rs) throws SQLException { return new JobResponse(uuid(rs, "id"), rs.getString("company_name"), rs.getString("title"), rs.getString("location"), rs.getString("work_mode"), number(rs, "experience_min"), number(rs, "experience_max"), rs.getString("application_url"), rs.getString("source"), instant(rs, "posted_at"), instant(rs, "discovered_at"), rs.getString("observed_status")); }
    private JobDetailResponse detail(ResultSet rs) throws SQLException { return new JobDetailResponse(uuid(rs, "id"), rs.getString("company_name"), rs.getString("title"), rs.getString("description_text"), rs.getString("location"), rs.getString("work_mode"), number(rs, "experience_min"), number(rs, "experience_max"), rs.getString("application_url"), rs.getString("source_url"), rs.getString("source"), instant(rs, "posted_at"), instant(rs, "discovered_at"), rs.getString("observed_status")); }
    private UUID uuid(ResultSet rs, String column) throws SQLException { byte[] value = rs.getBytes(column); if (value == null) return null; java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(value); return new UUID(buffer.getLong(), buffer.getLong()); }
    private byte[] idToBytes(UUID id) { return java.nio.ByteBuffer.allocate(16).putLong(id.getMostSignificantBits()).putLong(id.getLeastSignificantBits()).array(); }
    private Double number(ResultSet rs, String column) throws SQLException { java.math.BigDecimal value = rs.getBigDecimal(column); return value == null ? null : value.doubleValue(); }
    private Instant instant(ResultSet rs, String column) throws SQLException { java.sql.Timestamp value = rs.getTimestamp(column); return value == null ? null : value.toInstant(); }
}
