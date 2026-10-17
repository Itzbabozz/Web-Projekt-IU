package com.example.demo.controller;

import com.example.demo.dto.ProjectRequest;
import com.example.demo.dto.ProjectResponse;
import com.example.demo.service.ProjectService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getProjects() {
        return ResponseEntity.ok(projectService.getProjectsForCurrentTenant());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ProjectResponse> createProject(@RequestBody ProjectRequest request) {
        return ResponseEntity.ok(projectService.createProject(request));
    }

    @PutMapping("/{projectId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ProjectResponse> updateProject(@PathVariable Long projectId, @RequestBody ProjectRequest request) {
        return ResponseEntity.ok(projectService.updateProject(projectId, request));
    }

    @PatchMapping("/{projectId}/archive")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ProjectResponse> archiveProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(projectService.archiveProject(projectId));
    }

    @PutMapping("/{projectId}/employees")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ProjectResponse> assignEmployees(@PathVariable Long projectId, @RequestBody List<Long> employeeIds) {
        return ResponseEntity.ok(projectService.assignEmployees(projectId, employeeIds));
    }

    @GetMapping("/{projectId}/progress")
    public ResponseEntity<Map<String, Double>> getProgress(@PathVariable Long projectId) {
        return ResponseEntity.ok(Map.of("progress", projectService.getProjectProgress(projectId)));
    }
}
