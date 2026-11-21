package com.applypilot.user.controller;
import com.applypilot.user.dto.*; import com.applypilot.user.service.PreferencesService; import jakarta.validation.Valid; import java.util.UUID; import org.springframework.security.core.Authentication; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/users/preferences")
public class PreferencesController {
    private final PreferencesService service; public PreferencesController(PreferencesService service) { this.service = service; }
    @GetMapping public PreferencesResponse get(Authentication a) { return service.get((UUID) a.getPrincipal()); }
    @PutMapping public PreferencesResponse update(Authentication a, @Valid @RequestBody PreferencesRequest request) { return service.update((UUID) a.getPrincipal(), request); }
}
