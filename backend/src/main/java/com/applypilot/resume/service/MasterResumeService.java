package com.applypilot.resume.service;

import com.applypilot.common.ApiExceptionHandler.NotFoundException;
import com.applypilot.config.AppProperties;
import com.applypilot.resume.dto.MasterResumeResponse;
import com.applypilot.resume.dto.MasterResumeRolesRequest;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MasterResumeService {
    private static final long MAX_PDF_BYTES = 15L * 1024 * 1024;
    private static final long MAX_TEX_BYTES = 2L * 1024 * 1024;
    private final JdbcTemplate jdbc;
    private final Path root;

    public MasterResumeService(JdbcTemplate jdbc, AppProperties properties) {
        this.jdbc = jdbc;
        this.root = Path.of(properties.storage().resumesDir()).toAbsolutePath().normalize();
    }

    public MasterResumeResponse latest(UUID userId) {
        try {
            return jdbc.queryForObject("SELECT id, version_number, pdf_path, tex_path, target_roles, uploaded_at FROM master_resumes WHERE user_id=? ORDER BY version_number DESC LIMIT 1", (rs, row) -> response(rs), uuidBytes(userId));
        } catch (org.springframework.dao.EmptyResultDataAccessException exception) { throw new NotFoundException("No master resume has been uploaded"); }
    }

    public List<MasterResumeResponse> list(UUID userId) { return jdbc.query("SELECT id, version_number, pdf_path, tex_path, target_roles, uploaded_at FROM master_resumes WHERE user_id=? ORDER BY version_number DESC", (rs, row) -> response(rs), uuidBytes(userId)); }

    @Transactional
    public MasterResumeResponse upload(UUID userId, MultipartFile pdf, MultipartFile tex, String targetRoles) {
        validatePdf(pdf);
        validateTex(tex);
        String hash = hash(pdf);
        MasterResumeResponse existing = findByHash(userId, hash);
        if (existing != null) return existing;
        int version = nextVersion(userId);
        String relativeDirectory = userId + "/v" + version;
        Path directory = securePath(relativeDirectory);
        Path pdfPath = directory.resolve("master-resume.pdf");
        Path texPath = directory.resolve("master-resume.tex");
        try {
            Files.createDirectories(directory);
            copy(pdf, pdfPath);
            copy(tex, texPath);
        } catch (IOException exception) { throw new IllegalStateException("Could not save your master resume files", exception); }
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        String roles = cleanRoles(targetRoles);
        jdbc.update("INSERT INTO master_resumes (id,user_id,version_number,tex_path,pdf_path,content_hash,target_roles,uploaded_at,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,?,?)", uuidBytes(id), uuidBytes(userId), version, relativeDirectory + "/master-resume.tex", relativeDirectory + "/master-resume.pdf", hash, roles, Timestamp.from(now), Timestamp.from(now), Timestamp.from(now));
        return new MasterResumeResponse(id, version, "master-resume.pdf", "master-resume.tex", roles, now);
    }

    @Transactional
    public MasterResumeResponse updateRoles(UUID userId, UUID id, MasterResumeRolesRequest request) {
        Instant now = Instant.now();
        int changed = jdbc.update("UPDATE master_resumes SET target_roles=?, updated_at=? WHERE id=? AND user_id=?", cleanRoles(request.targetRoles()), Timestamp.from(now), uuidBytes(id), uuidBytes(userId));
        if (changed == 0) throw new NotFoundException("Master resume not found");
        return jdbc.queryForObject("SELECT id, version_number, pdf_path, tex_path, target_roles, uploaded_at FROM master_resumes WHERE id=? AND user_id=?", (rs, row) -> response(rs), uuidBytes(id), uuidBytes(userId));
    }

    public Resource downloadPdf(UUID userId) { return resource(userId, true); }
    public Resource downloadTex(UUID userId) { return resource(userId, false); }

    /** The default attachment for non-tailored applications is always the latest uploaded master PDF. */
    public Path defaultAttachment(UUID userId) {
        MasterResumeResponse latest = latest(userId);
        return securePath(userId + "/v" + latest.versionNumber() + "/master-resume.pdf");
    }

    /** Selects the labelled resume whose role keywords best match the job position, falling back to the latest upload. */
    public SelectedResume attachmentForPosition(UUID userId, String position) {
        List<MasterResumeResponse> resumes = list(userId);
        if (resumes.isEmpty()) throw new NotFoundException("No master resume has been uploaded");
        Set<String> positionTerms = terms(position);
        MasterResumeResponse selected = resumes.stream().max(java.util.Comparator.comparingInt((MasterResumeResponse resume) -> score(resume.targetRoles(), positionTerms)).thenComparingInt(MasterResumeResponse::versionNumber)).orElse(resumes.getFirst());
        if (score(selected.targetRoles(), positionTerms) == 0) selected = resumes.getFirst();
        return new SelectedResume(selected.id(), selected.versionNumber(), selected.targetRoles(), securePath(userId + "/v" + selected.versionNumber() + "/master-resume.pdf"));
    }

    public Path attachmentForId(UUID userId, UUID id) {
        MasterResumeResponse resume = jdbc.query("SELECT id, version_number, pdf_path, tex_path, target_roles, uploaded_at FROM master_resumes WHERE id=? AND user_id=?", (rs, row) -> response(rs), uuidBytes(id), uuidBytes(userId)).stream().findFirst().orElseThrow(() -> new NotFoundException("Selected master resume is unavailable"));
        return securePath(userId + "/v" + resume.versionNumber() + "/master-resume.pdf");
    }

    private Resource resource(UUID userId, boolean pdf) {
        MasterResumeResponse latest = latest(userId);
        Path file = securePath(userId + "/v" + latest.versionNumber() + (pdf ? "/master-resume.pdf" : "/master-resume.tex"));
        try {
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) throw new NotFoundException("Stored master resume file is unavailable");
            return resource;
        } catch (IOException exception) { throw new NotFoundException("Stored master resume file is unavailable"); }
    }

    private MasterResumeResponse findByHash(UUID userId, String hash) {
        var rows = jdbc.query("SELECT id, version_number, pdf_path, tex_path, target_roles, uploaded_at FROM master_resumes WHERE user_id=? AND content_hash=?", (rs, row) -> response(rs), uuidBytes(userId), hash);
        return rows.isEmpty() ? null : rows.getFirst();
    }
    private int nextVersion(UUID userId) { Integer max = jdbc.queryForObject("SELECT COALESCE(MAX(version_number), 0) FROM master_resumes WHERE user_id=?", Integer.class, uuidBytes(userId)); return (max == null ? 0 : max) + 1; }
    private MasterResumeResponse response(ResultSet rs) throws SQLException { return new MasterResumeResponse(uuid(rs.getBytes("id")), rs.getInt("version_number"), fileName(rs.getString("pdf_path")), fileName(rs.getString("tex_path")), rs.getString("target_roles"), rs.getTimestamp("uploaded_at").toInstant()); }
    private Path securePath(String relative) { Path path = root.resolve(relative).normalize(); if (!path.startsWith(root)) throw new IllegalArgumentException("Invalid storage path"); return path; }
    private void validatePdf(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("A PDF master resume is required");
        if (file.getSize() > MAX_PDF_BYTES) throw new IllegalArgumentException("Master resume PDF must be 15 MB or smaller");
        try (InputStream input = file.getInputStream()) { byte[] header = input.readNBytes(5); if (header.length != 5 || header[0] != '%' || header[1] != 'P' || header[2] != 'D' || header[3] != 'F' || header[4] != '-') throw new IllegalArgumentException("The uploaded resume is not a valid PDF"); }
        catch (IOException exception) { throw new IllegalArgumentException("Could not read the uploaded PDF"); }
    }
    private void validateTex(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("The LaTeX .tex source is required");
        if (file.getSize() > MAX_TEX_BYTES) throw new IllegalArgumentException("LaTeX source must be 2 MB or smaller");
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase().endsWith(".tex")) throw new IllegalArgumentException("Upload the original LaTeX file with a .tex extension");
    }
    private static void copy(MultipartFile upload, Path target) throws IOException { try (InputStream input = upload.getInputStream()) { Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING); } }
    private static String hash(MultipartFile file) { try (InputStream input = file.getInputStream()) { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.readAllBytes())); } catch (Exception exception) { throw new IllegalArgumentException("Could not process the uploaded PDF"); } }
    private static String cleanRoles(String value) { return value == null ? "" : value.trim(); }
    private static int score(String targetRoles, Set<String> positionTerms) { if (targetRoles == null || targetRoles.isBlank()) return 0; return (int) terms(targetRoles).stream().filter(positionTerms::contains).count(); }
    private static Set<String> terms(String value) { return java.util.Arrays.stream(value.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) .filter(word -> word.length() > 1 && !Set.of("developer", "engineer", "senior", "junior", "role", "position", "software").contains(word)).collect(java.util.stream.Collectors.toSet()); }
    private static byte[] uuidBytes(UUID id) { return ByteBuffer.allocate(16).putLong(id.getMostSignificantBits()).putLong(id.getLeastSignificantBits()).array(); }
    private static UUID uuid(byte[] bytes) { ByteBuffer buffer = ByteBuffer.wrap(bytes); return new UUID(buffer.getLong(), buffer.getLong()); }
    private static String fileName(String path) { return Path.of(path).getFileName().toString(); }
    public record SelectedResume(UUID id, int versionNumber, String targetRoles, Path path) { }
}
