package com.applypilot.application.service;

import com.applypilot.application.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.applypilot.common.ApiExceptionHandler.NotFoundException;
import com.applypilot.emailtemplate.service.EmailTemplateService;
import com.applypilot.resume.service.MasterResumeService;
import com.applypilot.integration.service.GoogleOAuthService;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.nio.ByteBuffer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplicationService {
    private final JdbcTemplate jdbc;
    private final MasterResumeService resumes;
    private final EmailTemplateService templates;
    private final GoogleOAuthService google;
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();
    public ApplicationService(JdbcTemplate jdbc, MasterResumeService resumes, EmailTemplateService templates, GoogleOAuthService google) { this.jdbc = jdbc; this.resumes = resumes; this.templates = templates; this.google = google; }

    public List<ApplicationResponse> list(UUID userId) {
        return jdbc.query("SELECT a.id, c.name company_name, j.title position, j.location, a.status, j.source, a.created_at, a.updated_at FROM applications a JOIN jobs j ON j.id=a.job_id LEFT JOIN companies c ON c.id=j.company_id WHERE a.user_id=? ORDER BY a.updated_at DESC", (rs, row) -> response(rs), uuidBytes(userId));
    }

    @Transactional
    public ApplicationResponse createManual(UUID userId, ManualApplicationRequest request) {
        String companyName = request.companyName().trim();
        String position = request.position().trim();
        String location = request.location().trim();
        String status = request.status().trim().toUpperCase(Locale.ROOT);
        Instant now = Instant.now();
        byte[] companyId = companyId(companyName, now);
        UUID jobId = UUID.randomUUID();
        jdbc.update("INSERT INTO jobs (id,company_id,source,external_job_id,title,location,observed_status,last_checked_at,discovered_at,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?)", uuidBytes(jobId), companyId, "MANUAL", "manual-" + jobId, position, location, "MANUAL", Timestamp.from(now), Timestamp.from(now), Timestamp.from(now), Timestamp.from(now));
        UUID applicationId = UUID.randomUUID();
        jdbc.update("INSERT INTO applications (id,user_id,job_id,status,email_version,created_at,updated_at) VALUES (?,?,?,?,?,?,?)", uuidBytes(applicationId), uuidBytes(userId), uuidBytes(jobId), status, 0, Timestamp.from(now), Timestamp.from(now));
        history(applicationId, null, status, "Manual application entry", now);
        return lookup(userId, applicationId);
    }

    @Transactional
    public EmailDraftResponse createEmailDraft(UUID userId, ManualEmailDraftRequest request) {
        String company = request.companyName().trim(), position = request.position().trim();
        String location = request.location() == null || request.location().isBlank() ? "Unspecified" : request.location().trim();
        String description = request.jobDescription() == null ? "" : request.jobDescription().trim();
        Instant now = Instant.now(); byte[] companyId = companyId(company, now); UUID jobId = UUID.randomUUID(); UUID appId = UUID.randomUUID();
        jdbc.update("INSERT INTO jobs (id,company_id,source,external_job_id,title,description_text,location,observed_status,last_checked_at,discovered_at,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)", uuidBytes(jobId), companyId, "MANUAL_EMAIL", "manual-email-" + jobId, position, description.isBlank() ? null : description, location, "MANUAL", Timestamp.from(now), Timestamp.from(now), Timestamp.from(now), Timestamp.from(now));
        EmailTemplateService.RenderedTemplate template = templates.render(userId, company, position);
        MasterResumeService.SelectedResume resume = resumes.attachmentForPosition(userId, position);
        String subject = template.subject();
        String body = template.body();
        jdbc.update("INSERT INTO applications (id,user_id,job_id,status,recipient_email,email_subject,email_body,master_resume_id,email_version,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?)", uuidBytes(appId), uuidBytes(userId), uuidBytes(jobId), "PENDING_APPROVAL", request.hrEmail().trim(), subject, body, uuidBytes(resume.id()), 1, Timestamp.from(now), Timestamp.from(now));
        history(appId, null, "PENDING_APPROVAL", "Manual email draft created", now);
        boolean needsTailoring = !description.isBlank();
        return new EmailDraftResponse(appId, request.hrEmail().trim(), subject, body, needsTailoring ? "TAILORING_REQUIRED" : "MASTER_PDF", needsTailoring ? "A tailored resume must be generated and reviewed before this draft can be approved." : attachmentNote(resume), "PENDING_APPROVAL", 1, resume.id());
    }

    public List<EmailDraftResponse> emailDrafts(UUID userId) { return jdbc.query("SELECT id,recipient_email,email_subject,email_body,status,job_id,email_version,master_resume_id FROM applications WHERE user_id=? AND recipient_email IS NOT NULL ORDER BY updated_at DESC", (rs,row) -> draft(rs), uuidBytes(userId)); }
    public GoogleSheetDraftImportResponse importGoogleSheetDrafts(UUID userId, GoogleSheetDraftImportRequest request) {
        List<List<String>> rows = readSheet(userId, request.spreadsheet(), request.range());
        if (rows.isEmpty()) throw new IllegalArgumentException("The selected Google Sheet range has no rows.");
        Map<String, Integer> columns = columns(rows.getFirst());
        int company = requiredColumn(columns, "company");
        int email = columns.containsKey("hr email") ? columns.get("hr email") : requiredColumn(columns, "email");
        int position = requiredColumn(columns, "position");
        List<EmailDraftResponse> drafts = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        for (int row = 1; row < rows.size(); row++) {
            List<String> values = rows.get(row);
            String companyName = value(values, company), hrEmail = value(values, email), jobPosition = value(values, position);
            if (companyName.isBlank() && hrEmail.isBlank() && jobPosition.isBlank()) continue;
            if (companyName.isBlank() || hrEmail.isBlank() || jobPosition.isBlank()) { skipped.add("Row " + (row + 1) + " is missing Company, HR Email, or Position."); continue; }
            try { drafts.add(createEmailDraft(userId, new ManualEmailDraftRequest(companyName, hrEmail, jobPosition, request.location(), null))); }
            catch (IllegalArgumentException exception) { skipped.add("Row " + (row + 1) + ": " + exception.getMessage()); }
        }
        return new GoogleSheetDraftImportResponse(drafts, skipped);
    }
    @Transactional
    public EmailDraftResponse updateEmailDraft(UUID userId, UUID id, EmailDraftUpdateRequest request) {
        EmailDraftResponse draft = draftForUser(userId, id);
        if (!"PENDING_APPROVAL".equals(draft.status())) throw new IllegalArgumentException("Only drafts awaiting approval can be edited.");
        Instant now = Instant.now();
        jdbc.update("UPDATE applications SET recipient_email=?, email_subject=?, email_body=?, email_version=email_version+1, updated_at=? WHERE id=? AND user_id=?", request.recipientEmail().trim(), request.subject().trim(), request.body(), Timestamp.from(now), uuidBytes(id), uuidBytes(userId));
        history(id, "PENDING_APPROVAL", "PENDING_APPROVAL", "Manual email draft edited", now);
        return draftForUser(userId, id);
    }
    @Transactional public EmailDraftResponse approveEmailDraft(UUID userId, UUID id) { EmailDraftResponse draft = draftForUser(userId, id); if ("TAILORING_REQUIRED".equals(draft.attachmentMode())) throw new IllegalArgumentException("This draft has a job description and needs a tailored derived resume before approval."); if (!"PENDING_APPROVAL".equals(draft.status())) return draft; Instant now = Instant.now(); jdbc.update("UPDATE applications SET status='APPROVED', updated_at=? WHERE id=? AND user_id=?", Timestamp.from(now), uuidBytes(id), uuidBytes(userId)); jdbc.update("INSERT INTO approvals (id,application_id,email_version,custom_resume_id,approved_at,created_at,updated_at) VALUES (?,?,?,?,?,?,?)", uuidBytes(UUID.randomUUID()), uuidBytes(id), draft.emailVersion(), null, Timestamp.from(now), Timestamp.from(now), Timestamp.from(now)); history(id, "PENDING_APPROVAL", "APPROVED", "Manual email draft approved", now); return draftForUser(userId,id); }
    @Transactional public EmailDraftResponse sendApprovedEmail(UUID userId, UUID id) {
        EmailDraftResponse draft = draftForUser(userId,id); if (!"APPROVED".equals(draft.status())) throw new IllegalArgumentException("Approve this exact draft before sending it.");
        try { byte[] pdf = Files.readAllBytes(draft.masterResumeId() == null ? resumes.defaultAttachment(userId) : resumes.attachmentForId(userId, draft.masterResumeId())); String raw = mime(draft, pdf); HttpRequest request = HttpRequest.newBuilder(URI.create("https://gmail.googleapis.com/gmail/v1/users/me/messages/send")).header("Authorization", "Bearer " + google.accessToken(userId)).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString("{\"raw\":\"" + Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8)) + "\"}" )).build(); HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString()); if (response.statusCode() < 200 || response.statusCode() >= 300) throw new IllegalArgumentException("Gmail rejected the send request (HTTP " + response.statusCode() + ")."); Instant now=Instant.now(); jdbc.update("UPDATE applications SET status='SENT', sent_at=?, updated_at=? WHERE id=?", Timestamp.from(now),Timestamp.from(now),uuidBytes(id)); history(id,"APPROVED","SENT","Gmail accepted manual send",now); return draftForUser(userId,id); } catch (java.io.IOException | InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalArgumentException("Could not send the Gmail message."); }
    }

    @Transactional
    public ApplicationResponse updateStatus(UUID userId, UUID applicationId, ApplicationStatusRequest request) {
        ApplicationResponse existing = lookup(userId, applicationId);
        String status = request.status().trim().toUpperCase(Locale.ROOT);
        if (!existing.status().equals(status)) {
            Instant now = Instant.now();
            jdbc.update("UPDATE applications SET status=?, updated_at=? WHERE id=? AND user_id=?", status, Timestamp.from(now), uuidBytes(applicationId), uuidBytes(userId));
            history(applicationId, existing.status(), status, "Status updated manually", now);
        }
        return lookup(userId, applicationId);
    }

    private ApplicationResponse lookup(UUID userId, UUID applicationId) {
        try { return jdbc.queryForObject("SELECT a.id, c.name company_name, j.title position, j.location, a.status, j.source, a.created_at, a.updated_at FROM applications a JOIN jobs j ON j.id=a.job_id LEFT JOIN companies c ON c.id=j.company_id WHERE a.user_id=? AND a.id=?", (rs, row) -> response(rs), uuidBytes(userId), uuidBytes(applicationId)); }
        catch (org.springframework.dao.EmptyResultDataAccessException exception) { throw new NotFoundException("Application not found"); }
    }
    private EmailDraftResponse draftForUser(UUID userId, UUID id) { try { return jdbc.queryForObject("SELECT id,recipient_email,email_subject,email_body,status,job_id,email_version,master_resume_id FROM applications WHERE user_id=? AND id=? AND recipient_email IS NOT NULL", (rs,row)->draft(rs),uuidBytes(userId),uuidBytes(id)); } catch(org.springframework.dao.EmptyResultDataAccessException e){ throw new NotFoundException("Email draft not found"); } }
    private EmailDraftResponse draft(ResultSet rs) throws SQLException { String jd=jdbc.queryForObject("SELECT description_text FROM jobs WHERE id=?",String.class,rs.getBytes("job_id")); boolean tailoring=jd!=null&&!jd.isBlank(); byte[] masterResumeId=rs.getBytes("master_resume_id"); UUID resumeId=masterResumeId == null ? null : uuid(masterResumeId); return new EmailDraftResponse(uuid(rs.getBytes("id")),rs.getString("recipient_email"),rs.getString("email_subject"),rs.getString("email_body"),tailoring?"TAILORING_REQUIRED":"MASTER_PDF",tailoring?"Tailored resume required before approval.":resumeId == null ? "Latest master PDF will be attached." : selectedAttachmentNote(resumeId),rs.getString("status"),rs.getInt("email_version"),resumeId); }
    private static String mime(EmailDraftResponse draft, byte[] pdf) { String boundary="applypilot-boundary"; return "To: "+draft.recipientEmail()+"\r\nSubject: "+draft.subject()+"\r\nMIME-Version: 1.0\r\nContent-Type: multipart/mixed; boundary=\""+boundary+"\"\r\n\r\n--"+boundary+"\r\nContent-Type: text/plain; charset=UTF-8\r\n\r\n"+draft.body()+"\r\n--"+boundary+"\r\nContent-Type: application/pdf; name=\"master-resume.pdf\"\r\nContent-Transfer-Encoding: base64\r\nContent-Disposition: attachment; filename=\"master-resume.pdf\"\r\n\r\n"+Base64.getMimeEncoder().encodeToString(pdf)+"\r\n--"+boundary+"--\r\n"; }
    private static String attachmentNote(MasterResumeService.SelectedResume resume) { return "Master resume version " + resume.versionNumber() + (resume.targetRoles().isBlank() ? " will be attached." : " (matched for " + resume.targetRoles() + ") will be attached."); }
    private String selectedAttachmentNote(UUID resumeId) { return jdbc.query("SELECT version_number,target_roles FROM master_resumes WHERE id=?", (rs, row) -> "Master resume version " + rs.getInt("version_number") + (rs.getString("target_roles").isBlank() ? " will be attached." : " (matched for " + rs.getString("target_roles") + ") will be attached."), uuidBytes(resumeId)).stream().findFirst().orElse("Selected master resume will be attached."); }
    private List<List<String>> readSheet(UUID userId, String spreadsheet, String range) {
        String spreadsheetId = spreadsheetId(spreadsheet);
        String url = "https://sheets.googleapis.com/v4/spreadsheets/" + encode(spreadsheetId) + "/values/" + encode(range);
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).header("Authorization", "Bearer " + google.accessToken(userId)).GET().build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) throw new IllegalArgumentException("Google Sheets could not read that range (HTTP " + response.statusCode() + "). Check the sheet link, range, and connection permissions.");
            SheetValues body = json.readValue(response.body(), SheetValues.class);
            return body.values() == null ? List.of() : body.values();
        } catch (java.io.IOException | InterruptedException exception) { Thread.currentThread().interrupt(); throw new IllegalArgumentException("Could not read the Google Sheet."); }
    }
    private static Map<String, Integer> columns(List<String> header) { Map<String, Integer> columns = new HashMap<>(); for (int index = 0; index < header.size(); index++) columns.put(header.get(index).trim().toLowerCase(Locale.ROOT), index); return columns; }
    private static int requiredColumn(Map<String, Integer> columns, String name) { Integer index = columns.get(name); if (index == null) throw new IllegalArgumentException("The first row must include a '" + name + "' column."); return index; }
    private static String value(List<String> row, int index) { return index < row.size() && row.get(index) != null ? row.get(index).trim() : ""; }
    private static String spreadsheetId(String value) { String trimmed = value.trim(); java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("/spreadsheets/d/([a-zA-Z0-9_-]+)").matcher(trimmed); return matcher.find() ? matcher.group(1) : trimmed; }
    private static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20"); }
    private record SheetValues(List<List<String>> values) { }
    private byte[] companyId(String companyName, Instant now) {
        String normalized = companyName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
        jdbc.update("INSERT INTO companies (id,name,normalized_name,created_at,updated_at) VALUES (?,?,?,?,?) ON DUPLICATE KEY UPDATE name=VALUES(name), updated_at=VALUES(updated_at)", uuidBytes(UUID.randomUUID()), companyName, normalized, Timestamp.from(now), Timestamp.from(now));
        return jdbc.queryForObject("SELECT id FROM companies WHERE normalized_name=?", byte[].class, normalized);
    }
    private void history(UUID applicationId, String from, String to, String note, Instant now) { jdbc.update("INSERT INTO application_status_history (id,application_id,from_status,to_status,changed_at,note) VALUES (?,?,?,?,?,?)", uuidBytes(UUID.randomUUID()), uuidBytes(applicationId), from, to, Timestamp.from(now), note); }
    private ApplicationResponse response(ResultSet rs) throws SQLException { return new ApplicationResponse(uuid(rs.getBytes("id")), rs.getString("company_name"), rs.getString("position"), rs.getString("location"), rs.getString("status"), "MANUAL".equals(rs.getString("source")), rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant()); }
    private static byte[] uuidBytes(UUID id) { return ByteBuffer.allocate(16).putLong(id.getMostSignificantBits()).putLong(id.getLeastSignificantBits()).array(); }
    private static UUID uuid(byte[] bytes) { ByteBuffer buffer = ByteBuffer.wrap(bytes); return new UUID(buffer.getLong(), buffer.getLong()); }
}
