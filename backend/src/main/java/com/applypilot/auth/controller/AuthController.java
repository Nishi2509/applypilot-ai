package com.applypilot.auth.controller;
import com.applypilot.auth.dto.*; import com.applypilot.auth.service.AuthService; import jakarta.validation.Valid; import java.util.UUID; import org.springframework.http.*; import org.springframework.security.core.Authentication; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1")
public class AuthController {
    private final AuthService auth; public AuthController(AuthService auth) { this.auth = auth; }
    @PostMapping("/auth/register") public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(auth.register(request)); }
    @PostMapping("/auth/login") public AuthResponse login(@Valid @RequestBody LoginRequest request) { return auth.login(request); }
    @GetMapping("/users/me") public UserResponse me(Authentication authentication) { return auth.me((UUID) authentication.getPrincipal()); }
}
