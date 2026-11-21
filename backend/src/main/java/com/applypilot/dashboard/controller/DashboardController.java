package com.applypilot.dashboard.controller;
import com.applypilot.dashboard.dto.DashboardSummaryResponse;
import com.applypilot.dashboard.service.DashboardService;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/dashboard")
public class DashboardController { private final DashboardService service; public DashboardController(DashboardService service) { this.service = service; } @GetMapping("/summary") public DashboardSummaryResponse summary(Authentication authentication) { return service.summary((UUID) authentication.getPrincipal()); } }
