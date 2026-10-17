package com.example.demo.service;

import com.example.demo.dto.TaskRequest;
import com.example.demo.dto.TaskResponse;
import com.example.demo.entity.Project;
import com.example.demo.entity.Task;
import com.example.demo.entity.TaskStatus;
import com.example.demo.entity.User;
import com.example.demo.repository.ProjectRepository;
import com.example.demo.repository.TaskRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public TaskService(TaskRepository taskRepository, ProjectRepository projectRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    public List<TaskResponse> getTasksForProject(Long projectId) {
        String tenantId = UserService.resolveTenant(null);
        return taskRepository.findByTenantIdAndProjectIdOrderByIdAsc(tenantId, projectId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public TaskResponse createTask(Long projectId, TaskRequest request) {
        String tenantId = UserService.resolveTenant(null);
        Project project = projectRepository.findByIdAndTenantId(projectId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        Task task = new Task();
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setTenantId(tenantId);
        task.setProject(project);
        if (request.assigneeId() != null) {
            User assignee = userRepository.findById(request.assigneeId())
                    .orElseThrow(() -> new IllegalArgumentException("Assignee not found"));
            if (!assignee.getTenantId().equals(tenantId)) {
                throw new IllegalArgumentException("Assignee belongs to another tenant");
            }
            task.setAssignee(assignee);
        }
        if (request.status() != null && !request.status().isBlank()) {
            task.setStatus(TaskStatus.valueOf(request.status().trim().toUpperCase()));
        }
        taskRepository.save(task);
        return toResponse(task);
    }

    public TaskResponse updateTask(Long taskId, TaskRequest request) {
        String tenantId = UserService.resolveTenant(null);
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));
        if (!task.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Task does not belong to the current tenant");
        }

        task.setTitle(request.title());
        task.setDescription(request.description());
        if (request.assigneeId() != null) {
            User assignee = userRepository.findById(request.assigneeId())
                    .orElseThrow(() -> new IllegalArgumentException("Assignee not found"));
            if (!assignee.getTenantId().equals(tenantId)) {
                throw new IllegalArgumentException("Assignee belongs to another tenant");
            }
            task.setAssignee(assignee);
        }
        if (request.status() != null && !request.status().isBlank()) {
            task.setStatus(TaskStatus.valueOf(request.status().trim().toUpperCase()));
        }
        return toResponse(task);
    }

    public TaskResponse updateStatus(Long taskId, String status) {
        String tenantId = UserService.resolveTenant(null);
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));
        if (!task.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Task does not belong to the current tenant");
        }
        task.setStatus(TaskStatus.valueOf(status.trim().toUpperCase()));
        return toResponse(task);
    }

    private TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getProject().getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus().name(),
                task.getAssignee() != null ? task.getAssignee().getId() : null,
                task.getAssignee() != null ? task.getAssignee().getUsername() : null,
                task.getTenantId()
        );
    }
}
