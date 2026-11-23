package com.applypilot.resume.controller;

import com.applypilot.resume.dto.MasterResumeResponse;
import com.applypilot.resume.dto.MasterResumeRolesRequest;
import com.applypilot.resume.service.MasterResumeService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/resumes")
public class ResumeController {
    private final MasterResumeService service;
    public ResumeController(MasterResumeService service) { this.service = service; }
    @GetMapping("/master") public MasterResumeResponse latest(Authentication auth) { return service.latest(userId(auth)); }
    @GetMapping("/master/all") public List<MasterResumeResponse> list(Authentication auth) { return service.list(userId(auth)); }
    @PostMapping(value = "/master", consumes = MediaType.MULTIPART_FORM_DATA_VALUE) public MasterResumeResponse upload(Authentication auth, @RequestPart("pdf") MultipartFile pdf, @RequestPart("tex") MultipartFile tex, @RequestParam(required = false) String targetRoles) { return service.upload(userId(auth), pdf, tex, targetRoles); }
    @PutMapping("/master/{id}/roles") public MasterResumeResponse updateRoles(Authentication auth, @PathVariable UUID id, @Valid @RequestBody MasterResumeRolesRequest request) { return service.updateRoles(userId(auth), id, request); }
    @GetMapping("/master/pdf") public ResponseEntity<Resource> pdf(Authentication auth) { return download(service.downloadPdf(userId(auth)), "master-resume.pdf", MediaType.APPLICATION_PDF); }
    @GetMapping("/master/tex") public ResponseEntity<Resource> tex(Authentication auth) { return download(service.downloadTex(userId(auth)), "master-resume.tex", MediaType.TEXT_PLAIN); }
    private UUID userId(Authentication auth) { return (UUID) auth.getPrincipal(); }
    private ResponseEntity<Resource> download(Resource resource, String fileName, MediaType mediaType) { return ResponseEntity.ok().contentType(mediaType).header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(fileName).build().toString()).body(resource); }
}
