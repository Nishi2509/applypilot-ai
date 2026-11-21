package com.applypilot.user.entity;

import com.applypilot.auth.entity.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "user_preferences")
public class UserPreferences {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false, unique = true) private User user;
    @Column(name = "roles_json", nullable = false, columnDefinition = "json") private String rolesJson;
    @Column(name = "locations_json", nullable = false, columnDefinition = "json") private String locationsJson;
    @Column(name = "work_modes_json", nullable = false, columnDefinition = "json") private String workModesJson;
    @Column(name = "experience_min", nullable = false) private int experienceMin;
    @Column(name = "experience_max", nullable = false) private int experienceMax;
    @Column(name = "manual_approval_required", nullable = false) private boolean manualApprovalRequired = true;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @PrePersist void created() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate void updated() { updatedAt = Instant.now(); }
    public UUID getId() { return id; } public int getExperienceMin() { return experienceMin; } public int getExperienceMax() { return experienceMax; } public boolean isManualApprovalRequired() { return manualApprovalRequired; }
    public String getRolesJson() { return rolesJson; } public String getLocationsJson() { return locationsJson; } public String getWorkModesJson() { return workModesJson; }
    public void set(User user, String roles, String locations, String workModes, int min, int max, boolean approval) { this.user = user; rolesJson = roles; locationsJson = locations; workModesJson = workModes; experienceMin = min; experienceMax = max; manualApprovalRequired = approval; }
    public void update(String roles, String locations, String workModes, int min, int max, boolean approval) { rolesJson = roles; locationsJson = locations; workModesJson = workModes; experienceMin = min; experienceMax = max; manualApprovalRequired = approval; }
}
