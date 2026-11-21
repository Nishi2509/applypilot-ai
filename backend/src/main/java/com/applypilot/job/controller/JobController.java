package com.applypilot.job.controller;
import com.applypilot.job.dto.*;
import com.applypilot.job.service.JobQueryService;
import jakarta.validation.constraints.*;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
@RestController @Validated @RequestMapping("/api/v1/jobs")
public class JobController { private final JobQueryService service; public JobController(JobQueryService service) { this.service = service; } @GetMapping public JobListResponse list(@RequestParam(defaultValue = "25") @Min(1) @Max(100) int limit, @RequestParam(defaultValue = "0") @Min(0) int offset) { return service.list(limit, offset); } @GetMapping("/{id}") public JobDetailResponse detail(@PathVariable UUID id) { return service.detail(id); } }
