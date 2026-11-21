package com.applypilot.user.service;

import com.applypilot.auth.entity.User; import com.applypilot.common.ApiExceptionHandler.NotFoundException; import com.applypilot.user.dto.*; import com.applypilot.user.entity.UserPreferences; import com.applypilot.user.repository.UserPreferencesRepository;
import com.fasterxml.jackson.core.JsonProcessingException; import com.fasterxml.jackson.core.type.TypeReference; import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
@Service
public class PreferencesService {
    private static final List<String> DEFAULT_ROLES = List.of("Java Developer", "Java Backend Developer", "Spring Boot Developer", "Java Full Stack Developer", "Full Stack Developer");
    private static final List<String> DEFAULT_LOCATIONS = List.of("Bangalore", "Pune", "Gurgaon", "Ahmedabad", "Jaipur", "Noida", "Hyderabad", "Mumbai", "Remote");
    private static final List<String> DEFAULT_MODES = List.of("Remote", "Hybrid", "Work From Office");
    private final UserPreferencesRepository repository; private final ObjectMapper mapper;
    public PreferencesService(UserPreferencesRepository repository, ObjectMapper mapper) { this.repository = repository; this.mapper = mapper; }
    @Transactional public void createDefaults(User user) { UserPreferences p = new UserPreferences(); p.set(user, json(DEFAULT_ROLES), json(DEFAULT_LOCATIONS), json(DEFAULT_MODES), 2, 3, true); repository.save(p); }
    @Transactional(readOnly = true) public PreferencesResponse get(UUID userId) { return response(repository.findByUserId(userId).orElseThrow(() -> new NotFoundException("Preferences not found"))); }
    @Transactional public PreferencesResponse update(UUID userId, PreferencesRequest r) { UserPreferences p = repository.findByUserId(userId).orElseThrow(() -> new NotFoundException("Preferences not found")); p.update(json(clean(r.roles())), json(clean(r.locations())), json(clean(r.workModes())), r.experienceMin(), r.experienceMax(), r.manualApprovalRequired()); return response(p); }
    private List<String> clean(List<String> values) { return values.stream().map(String::trim).filter(s -> !s.isEmpty()).distinct().toList(); }
    private String json(Object value) { try { return mapper.writeValueAsString(value); } catch (JsonProcessingException e) { throw new IllegalStateException("Could not serialize preferences", e); } }
    private List<String> list(String value) { try { return mapper.readValue(value, new TypeReference<>() {}); } catch (JsonProcessingException e) { throw new IllegalStateException("Stored preferences are invalid", e); } }
    private PreferencesResponse response(UserPreferences p) { return new PreferencesResponse(list(p.getRolesJson()), list(p.getLocationsJson()), list(p.getWorkModesJson()), p.getExperienceMin(), p.getExperienceMax(), p.isManualApprovalRequired()); }
}
