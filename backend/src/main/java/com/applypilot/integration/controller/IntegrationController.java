package com.applypilot.integration.controller;

import com.applypilot.config.AppProperties;
import com.applypilot.integration.dto.*;
import com.applypilot.integration.service.GoogleOAuthService;
import com.applypilot.integration.service.IntegrationStatusService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@RestController @RequestMapping("/api/v1/integrations")
public class IntegrationController {
    private final GoogleOAuthService google; private final IntegrationStatusService status; private final AppProperties properties;
    public IntegrationController(GoogleOAuthService google, IntegrationStatusService status, AppProperties properties) { this.google = google; this.status = status; this.properties = properties; }
    @GetMapping("/status") public IntegrationStatusResponse status(Authentication authentication) { return status.status((UUID) authentication.getPrincipal()); }
    @PostMapping({"/gmail/connect", "/google-sheets/connect"}) public OAuthAuthorizationResponse connect(Authentication authentication) { return google.begin((UUID) authentication.getPrincipal()); }
    @GetMapping("/google/callback") public ResponseEntity<Void> callback(@RequestParam(required = false) String code, @RequestParam(required = false) String state, @RequestParam(required = false) String error) { try { google.complete(code, state, error); return redirect("connected"); } catch (IllegalArgumentException e) { return redirect("failed"); } }
    private ResponseEntity<Void> redirect(String result) { URI uri = UriComponentsBuilder.fromUriString(properties.frontend().baseUrl()).path("/integrations").queryParam("google", result).build().toUri(); return ResponseEntity.status(HttpStatus.FOUND).location(uri).build(); }
}
