package com.applypilot.job.service;

import com.applypilot.common.ApiExceptionHandler.NotFoundException;
import com.applypilot.config.AppProperties;
import com.applypilot.job.dto.JobSearchResponse;
import com.applypilot.job.dto.JobSourceRequest;
import com.applypilot.job.dto.JobSourceResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Fetches only published roles from company boards explicitly configured by the user. */
@Service
public class JobDiscoveryService {
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final AppProperties properties;

    public JobDiscoveryService(JdbcTemplate jdbc, ObjectMapper mapper, AppProperties properties) { this.jdbc = jdbc; this.mapper = mapper; this.properties = properties; }

    public List<JobSourceResponse> listSources(UUID userId) {
        return jdbc.query("SELECT id, provider, board_slug, display_name, enabled, created_at FROM job_source_configs WHERE user_id=? ORDER BY display_name", (rs, row) -> source(rs), uuidBytes(userId));
    }

    @Transactional
    public JobSourceResponse addSource(UUID userId, JobSourceRequest request) {
        String provider = request.provider().trim().toUpperCase(Locale.ROOT);
        String slug = request.boardSlug().trim().toLowerCase(Locale.ROOT);
        String name = request.displayName().trim();
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        jdbc.update("INSERT INTO job_source_configs (id,user_id,provider,board_slug,display_name,enabled,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE display_name=VALUES(display_name), enabled=TRUE, updated_at=VALUES(updated_at)", uuidBytes(id), uuidBytes(userId), provider, slug, name, true, Timestamp.from(now), Timestamp.from(now));
        return jdbc.queryForObject("SELECT id, provider, board_slug, display_name, enabled, created_at FROM job_source_configs WHERE user_id=? AND provider=? AND board_slug=?", (rs, row) -> source(rs), uuidBytes(userId), provider, slug);
    }

    @Transactional
    public void removeSource(UUID userId, UUID id) {
        if (jdbc.update("DELETE FROM job_source_configs WHERE id=? AND user_id=?", uuidBytes(id), uuidBytes(userId)) == 0) throw new NotFoundException("Job source not found");
    }

    public JobSearchResponse search(UUID userId) {
        List<Source> sources = jdbc.query("SELECT provider, board_slug, display_name FROM job_source_configs WHERE user_id=? AND enabled=TRUE", (rs, row) -> new Source(rs.getString("provider"), rs.getString("board_slug"), rs.getString("display_name")), uuidBytes(userId));
        if (sources.isEmpty()) throw new IllegalArgumentException("Add at least one Greenhouse or Lever company board before searching.");
        int discovered = 0;
        int updated = 0;
        List<String> errors = new ArrayList<>();
        for (Source source : sources) {
            try {
                SearchCount count = "GREENHOUSE".equals(source.provider()) ? fetchGreenhouse(source) : fetchLever(source);
                discovered += count.discovered();
                updated += count.updated();
            } catch (Exception exception) {
                errors.add(source.displayName() + ": " + safeMessage(exception));
            }
        }
        return new JobSearchResponse(sources.size(), discovered, updated, errors);
    }

    /** Searches the web through the configured provider; it never scrapes a search-engine result page. */
    public JobSearchResponse webSearch(UUID userId) {
        String openAiKey = properties.webSearch() == null ? null : properties.webSearch().openaiApiKey();
        String tavilyKey = properties.webSearch() == null ? null : properties.webSearch().tavilyApiKey();
        if (isBlank(openAiKey) && isBlank(tavilyKey)) throw new IllegalArgumentException("Web search is not configured. Add OPENAI_API_KEY or TAVILY_API_KEY to .env and restart the backend.");
        List<String> roles = preferenceList(userId, "roles_json");
        List<String> locations = preferenceList(userId, "locations_json");
        if (roles.isEmpty()) throw new IllegalArgumentException("Save at least one target role before running a web search.");
        String location = locations.isEmpty() ? "" : " in " + locations.getFirst();
        int discovered = 0, updated = 0;
        List<String> errors = new ArrayList<>();
        for (String role : roles.stream().limit(3).toList()) {
            try {
                String query = role + " jobs" + location + " careers";
                SearchCount count = !isBlank(openAiKey) ? fetchOpenAiWebResults(query, location, openAiKey) : fetchWebResults(query, location, tavilyKey);
                discovered += count.discovered(); updated += count.updated();
            } catch (Exception exception) { errors.add(role + ": " + safeMessage(exception)); }
        }
        return new JobSearchResponse(Math.min(roles.size(), 3), discovered, updated, errors);
    }

    private SearchCount fetchGreenhouse(Source source) throws Exception {
        JsonNode root = fetch("https://boards-api.greenhouse.io/v1/boards/" + source.slug() + "/jobs?content=true");
        int discovered = 0, updated = 0;
        for (JsonNode job : root.path("jobs")) {
            String externalId = job.path("id").asText();
            if (externalId.isBlank()) continue;
            boolean exists = jobExists("GREENHOUSE", externalId);
            saveJob(source, "GREENHOUSE", externalId, job.path("title").asText("Untitled role"), job.path("content").asText(""), job.path("location").path("name").asText(null), job.path("absolute_url").asText(null), job.path("absolute_url").asText(null), parseInstant(job.path("updated_at").asText(null)));
            if (exists) updated++; else discovered++;
        }
        return new SearchCount(discovered, updated);
    }

    private SearchCount fetchLever(Source source) throws Exception {
        JsonNode jobs = fetch("https://api.lever.co/v0/postings/" + source.slug() + "?mode=json");
        int discovered = 0, updated = 0;
        for (JsonNode job : jobs) {
            String externalId = job.path("id").asText();
            if (externalId.isBlank()) continue;
            boolean exists = jobExists("LEVER", externalId);
            String url = job.path("applyUrl").asText(job.path("hostedUrl").asText(null));
            saveJob(source, "LEVER", externalId, job.path("text").asText("Untitled role"), job.path("descriptionPlain").asText(job.path("description").asText("")), job.path("categories").path("location").asText(null), url, job.path("hostedUrl").asText(url), epochMillis(job.path("createdAt").asLong(0)));
            if (exists) updated++; else discovered++;
        }
        return new SearchCount(discovered, updated);
    }

    private SearchCount fetchWebResults(String query, String location, String apiKey) throws Exception {
        var body = mapper.createObjectNode();
        body.put("query", query);
        body.put("topic", "general");
        body.put("search_depth", "basic");
        body.put("max_results", 10);
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.tavily.com/search")).timeout(Duration.ofSeconds(25)).header("Content-Type", "application/json").header("Authorization", "Bearer " + apiKey).POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) throw new IllegalStateException("web search returned HTTP " + response.statusCode());
        int discovered = 0, updated = 0;
        for (JsonNode result : mapper.readTree(response.body()).path("results")) {
            String url = result.path("url").asText();
            if (!url.startsWith("https://") && !url.startsWith("http://")) continue;
            String externalId = sha256(url);
            boolean exists = jobExists("WEB_SEARCH", externalId);
            String company = companyFromUrl(url);
            saveJob(new Source("WEB_SEARCH", "web", company), "WEB_SEARCH", externalId, result.path("title").asText("Job lead"), result.path("content").asText(""), blankToNull(location), url, url, null);
            if (exists) updated++; else discovered++;
        }
        return new SearchCount(discovered, updated);
    }

    private SearchCount fetchOpenAiWebResults(String query, String location, String apiKey) throws Exception {
        var body = mapper.createObjectNode();
        body.put("model", isBlank(properties.webSearch().openaiModel()) ? "gpt-5.4" : properties.webSearch().openaiModel());
        body.put("input", "Find up to ten currently open job opportunities for: " + query + ". Prefer direct company careers or ATS pages. Cite every job link, and do not invent vacancies.");
        body.putArray("tools").addObject().put("type", "web_search").put("search_context_size", "medium");
        body.putObject("reasoning").put("effort", "low");
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.openai.com/v1/responses")).timeout(Duration.ofSeconds(45)).header("Content-Type", "application/json").header("Authorization", "Bearer " + apiKey).POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) throw new IllegalStateException("OpenAI web search returned HTTP " + response.statusCode());
        JsonNode root = mapper.readTree(response.body());
        String summary = root.path("output_text").asText("");
        int discovered = 0, updated = 0;
        java.util.Set<String> urls = new java.util.HashSet<>();
        for (JsonNode output : root.path("output")) for (JsonNode content : output.path("content")) for (JsonNode annotation : content.path("annotations")) {
            if (!"url_citation".equals(annotation.path("type").asText())) continue;
            String url = annotation.path("url").asText();
            if ((!url.startsWith("https://") && !url.startsWith("http://")) || !urls.add(url)) continue;
            String externalId = sha256(url);
            boolean exists = jobExists("WEB_SEARCH_OPENAI", externalId);
            String title = annotation.path("title").asText("Web job lead");
            saveJob(new Source("WEB_SEARCH_OPENAI", "openai", companyFromUrl(url)), "WEB_SEARCH_OPENAI", externalId, title, summary, blankToNull(location), url, url, null);
            if (exists) updated++; else discovered++;
        }
        return new SearchCount(discovered, updated);
    }

    private JsonNode fetch(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(25)).header("Accept", "application/json").header("User-Agent", "ApplyPilot/0.1 (published-job-board-sync)").GET().build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) throw new IllegalStateException("source returned HTTP " + response.statusCode());
        return mapper.readTree(response.body());
    }

    private List<String> preferenceList(UUID userId, String column) {
        try {
            String json = jdbc.queryForObject("SELECT " + column + " FROM user_preferences WHERE user_id=?", String.class, uuidBytes(userId));
            if (json == null) return List.of();
            List<String> values = new ArrayList<>();
            for (JsonNode value : mapper.readTree(json)) if (value.isTextual() && !value.asText().isBlank()) values.add(value.asText());
            return values;
        } catch (org.springframework.dao.EmptyResultDataAccessException exception) { return List.of(); }
        catch (Exception exception) { throw new IllegalStateException("Could not read your saved search preferences"); }
    }

    @Transactional
    private void saveJob(Source source, String provider, String externalId, String title, String description, String location, String applicationUrl, String sourceUrl, Instant postedAt) {
        byte[] companyId = companyId(source.displayName());
        Instant now = Instant.now();
        jdbc.update("INSERT INTO jobs (id,company_id,source,external_job_id,title,description_text,location,application_url,source_url,posted_at,observed_status,last_checked_at,discovered_at,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE company_id=VALUES(company_id), title=VALUES(title), description_text=VALUES(description_text), location=VALUES(location), application_url=VALUES(application_url), source_url=VALUES(source_url), posted_at=VALUES(posted_at), observed_status='OPEN', last_checked_at=VALUES(last_checked_at), updated_at=VALUES(updated_at)", uuidBytes(UUID.randomUUID()), companyId, provider, externalId, title, description, blankToNull(location), blankToNull(applicationUrl), blankToNull(sourceUrl), timestamp(postedAt), "OPEN", Timestamp.from(now), Timestamp.from(now), Timestamp.from(now), Timestamp.from(now));
    }

    private byte[] companyId(String displayName) {
        String normalized = displayName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
        jdbc.update("INSERT INTO companies (id,name,normalized_name,created_at,updated_at) VALUES (?,?,?,?,?) ON DUPLICATE KEY UPDATE name=VALUES(name), updated_at=VALUES(updated_at)", uuidBytes(UUID.randomUUID()), displayName, normalized, Timestamp.from(Instant.now()), Timestamp.from(Instant.now()));
        return jdbc.queryForObject("SELECT id FROM companies WHERE normalized_name=?", byte[].class, normalized);
    }

    private boolean jobExists(String provider, String externalId) { Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM jobs WHERE source=? AND external_job_id=?", Integer.class, provider, externalId); return count != null && count > 0; }
    private JobSourceResponse source(ResultSet rs) throws SQLException { return new JobSourceResponse(uuid(rs.getBytes("id")), rs.getString("provider"), rs.getString("board_slug"), rs.getString("display_name"), rs.getBoolean("enabled"), rs.getTimestamp("created_at").toInstant()); }
    private static byte[] uuidBytes(UUID id) { return ByteBuffer.allocate(16).putLong(id.getMostSignificantBits()).putLong(id.getLeastSignificantBits()).array(); }
    private static UUID uuid(byte[] bytes) { ByteBuffer buffer = ByteBuffer.wrap(bytes); return new UUID(buffer.getLong(), buffer.getLong()); }
    private static Timestamp timestamp(Instant value) { return value == null ? null : Timestamp.from(value); }
    private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value; }
    private static boolean isBlank(String value) { return value == null || value.isBlank(); }
    private static Instant parseInstant(String value) { try { return value == null || value.isBlank() ? null : Instant.parse(value); } catch (Exception ignored) { return null; } }
    private static Instant epochMillis(long value) { return value <= 0 ? null : Instant.ofEpochMilli(value); }
    private static String safeMessage(Exception exception) { String message = exception.getMessage(); return message == null || message.isBlank() ? "could not read the published board" : message.substring(0, Math.min(message.length(), 160)); }
    private static String companyFromUrl(String url) { try { String host = URI.create(url).getHost(); return host == null || host.isBlank() ? "Web result" : host.replaceFirst("^www\\.", ""); } catch (Exception ignored) { return "Web result"; } }
    private static String sha256(String value) { try { byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)); StringBuilder text = new StringBuilder(64); for (byte b : hash) text.append(String.format("%02x", b)); return text.toString(); } catch (Exception exception) { throw new IllegalStateException("Could not create job fingerprint", exception); } }
    private record Source(String provider, String slug, String displayName) {}
    private record SearchCount(int discovered, int updated) {}
}
