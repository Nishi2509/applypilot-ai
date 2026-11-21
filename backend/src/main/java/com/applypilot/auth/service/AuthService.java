package com.applypilot.auth.service;

import com.applypilot.auth.dto.*; import com.applypilot.auth.entity.User; import com.applypilot.auth.repository.UserRepository; import com.applypilot.common.ApiExceptionHandler.ConflictException; import com.applypilot.common.ApiExceptionHandler.NotFoundException; import com.applypilot.security.JwtService; import com.applypilot.user.service.PreferencesService;
import java.util.Locale; import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
@Service
public class AuthService {
    private final UserRepository users; private final PasswordEncoder passwords; private final JwtService jwt; private final PreferencesService preferences;
    public AuthService(UserRepository users, PasswordEncoder passwords, JwtService jwt, PreferencesService preferences) { this.users = users; this.passwords = passwords; this.jwt = jwt; this.preferences = preferences; }
    @Transactional public AuthResponse register(RegisterRequest request) { String email = request.email().trim().toLowerCase(Locale.ROOT); if (users.findByEmailIgnoreCase(email).isPresent()) throw new ConflictException("An account already exists for that email address"); User user = new User(); user.register(email, passwords.encode(request.password()), request.displayName().trim()); users.save(user); preferences.createDefaults(user); return response(user); }
    @Transactional(readOnly = true) public AuthResponse login(LoginRequest request) { User user = users.findByEmailIgnoreCase(request.email().trim()).filter(candidate -> passwords.matches(request.password(), candidate.getPasswordHash())).orElseThrow(() -> new NotFoundException("Invalid email or password")); return response(user); }
    @Transactional(readOnly = true) public UserResponse me(UUID id) { User u = users.findById(id).orElseThrow(() -> new NotFoundException("User not found")); return new UserResponse(u.getId(), u.getEmail(), u.getDisplayName()); }
    private AuthResponse response(User user) { return new AuthResponse(jwt.issue(user.getId(), user.getEmail()), "Bearer", jwt.expirySeconds(), user.getId(), user.getEmail(), user.getDisplayName()); }
}
