package com.example.demo.controller;

import com.example.demo.dto.TaskRequest;
import com.example.demo.dto.TaskResponse;
import com.example.demo.service.TaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/projects/{projectId}/tasks")
    public ResponseEntity<List<TaskResponse>> getTasks(@PathVariable Long projectId) {
        return ResponseEntity.ok(taskService.getTasksForProject(projectId));
    }

    @PostMapping("/projects/{projectId}/tasks")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER', 'EMPLOYEE')")
    public ResponseEntity<TaskResponse> createTask(@PathVariable Long projectId, @RequestBody TaskRequest request) {
        return ResponseEntity.ok(taskService.createTask(projectId, request));
    }

    @PutMapping("/tasks/{taskId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER', 'EMPLOYEE')")
    public ResponseEntity<TaskResponse> updateTask(@PathVariable Long taskId, @RequestBody TaskRequest request) {
        return ResponseEntity.ok(taskService.updateTask(taskId, request));
    }

    @PatchMapping("/tasks/{taskId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER', 'EMPLOYEE')")
    public ResponseEntity<TaskResponse> updateStatus(@PathVariable Long taskId, @RequestParam String status) {
        return ResponseEntity.ok(taskService.updateStatus(taskId, status));
    }
}
