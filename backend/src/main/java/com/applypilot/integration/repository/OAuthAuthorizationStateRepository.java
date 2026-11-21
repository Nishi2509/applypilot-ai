package com.applypilot.integration.repository;
import com.applypilot.integration.entity.OAuthAuthorizationState;
import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface OAuthAuthorizationStateRepository extends JpaRepository<OAuthAuthorizationState, UUID> { Optional<OAuthAuthorizationState> findByStateHash(String stateHash); long deleteByExpiresAtBefore(Instant instant); }
