package com.applypilot.integration.repository;
import com.applypilot.integration.entity.OAuthConnection;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface OAuthConnectionRepository extends JpaRepository<OAuthConnection, UUID> { Optional<OAuthConnection> findByUserIdAndProvider(UUID userId, String provider); }
