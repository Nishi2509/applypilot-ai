package com.applypilot.emailtemplate.service;

import com.applypilot.emailtemplate.dto.EmailTemplateRequest;
import com.applypilot.emailtemplate.dto.EmailTemplateResponse;
import java.nio.ByteBuffer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailTemplateService {
    public static final String COMPANY = "{{companyName}}";
    public static final String POSITION = "{{position}}";
    private static final String DEFAULT_SUBJECT = "Application for {{position}}";
    private static final String DEFAULT_BODY = "Dear Hiring Team,\n\nI am writing to apply for the {{position}} position at {{companyName}}. Please find my resume attached for your consideration.\n\nThank you for your time.\n\nSincerely,";
    private final JdbcTemplate jdbc;

    public EmailTemplateService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public EmailTemplateResponse get(UUID userId) {
        var rows = jdbc.query("SELECT id, subject, body, updated_at FROM email_templates WHERE user_id=?", (rs, row) -> response(rs), uuidBytes(userId));
        return rows.isEmpty() ? new EmailTemplateResponse(null, DEFAULT_SUBJECT, DEFAULT_BODY, null) : rows.getFirst();
    }

    @Transactional
    public EmailTemplateResponse save(UUID userId, EmailTemplateRequest request) {
        Instant now = Instant.now();
        String subject = request.subject().trim();
        String body = request.body();
        int changed = jdbc.update("UPDATE email_templates SET subject=?, body=?, updated_at=? WHERE user_id=?", subject, body, Timestamp.from(now), uuidBytes(userId));
        if (changed == 0) jdbc.update("INSERT INTO email_templates (id,user_id,subject,body,created_at,updated_at) VALUES (?,?,?,?,?,?)", uuidBytes(UUID.randomUUID()), uuidBytes(userId), subject, body, Timestamp.from(now), Timestamp.from(now));
        return get(userId);
    }

    public RenderedTemplate render(UUID userId, String companyName, String position) {
        EmailTemplateResponse template = get(userId);
        return new RenderedTemplate(replace(template.subject(), companyName, position), replace(template.body(), companyName, position));
    }

    private static String replace(String value, String companyName, String position) { return value.replace(COMPANY, companyName).replace(POSITION, position); }
    private static EmailTemplateResponse response(ResultSet rs) throws SQLException { return new EmailTemplateResponse(uuid(rs.getBytes("id")), rs.getString("subject"), rs.getString("body"), rs.getTimestamp("updated_at").toInstant()); }
    private static byte[] uuidBytes(UUID id) { return ByteBuffer.allocate(16).putLong(id.getMostSignificantBits()).putLong(id.getLeastSignificantBits()).array(); }
    private static UUID uuid(byte[] bytes) { ByteBuffer buffer = ByteBuffer.wrap(bytes); return new UUID(buffer.getLong(), buffer.getLong()); }

    public record RenderedTemplate(String subject, String body) { }
}
