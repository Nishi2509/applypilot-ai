package com.applypilot.user.repository;
import com.applypilot.user.entity.UserPreferences;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface UserPreferencesRepository extends JpaRepository<UserPreferences, UUID> { Optional<UserPreferences> findByUserId(UUID userId); }
