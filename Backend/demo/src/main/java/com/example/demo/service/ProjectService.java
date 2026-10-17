package com.example.demo.service;

import com.example.demo.dto.ProjectRequest;
import com.example.demo.dto.ProjectResponse;
import com.example.demo.dto.UserResponse;
import com.example.demo.entity.Project;
import com.example.demo.entity.Task;
import com.example.demo.entity.TaskStatus;
import com.example.demo.entity.User;
import com.example.demo.repository.ProjectRepository;
import com.example.demo.repository.TaskRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;

    public ProjectService(ProjectRepository projectRepository, UserRepository userRepository, TaskRepository taskRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
    }

    public List<ProjectResponse> getProjectsForCurrentTenant() {
        String tenantId = UserService.resolveTenant(null);
        return projectRepository.findByTenantIdOrderByNameAsc(tenantId).stream()
                .map(this::toResponse)
                .toList();
    }

    public ProjectResponse createProject(ProjectRequest request) {
        String tenantId = UserService.resolveTenant(null);
        Project project = new Project();
        project.setName(request.name());
        project.setDescription(request.description());
        project.setArchived(request.archived());
        project.setTenantId(tenantId);
        applyEmployees(project, request.employeeIds());
        projectRepository.save(project);
        return toResponse(project);
    }

    public ProjectResponse updateProject(Long projectId, ProjectRequest request) {
        Project project = getProjectForCurrentTenant(projectId);
        project.setName(request.name());
        project.setDescription(request.description());
        project.setArchived(request.archived());
        applyEmployees(project, request.employeeIds());
        return toResponse(project);
    }

    public ProjectResponse archiveProject(Long projectId) {
        Project project = getProjectForCurrentTenant(projectId);
        project.setArchived(true);
        return toResponse(project);
    }

    public ProjectResponse assignEmployees(Long projectId, List<Long> employeeIds) {
        Project project = getProjectForCurrentTenant(projectId);
        applyEmployees(project, employeeIds);
        return toResponse(project);
    }

    public Double getProjectProgress(Long projectId) {
        Project project = getProjectForCurrentTenant(projectId);
        List<Task> tasks = taskRepository.findByTenantIdAndProjectId(project.getTenantId(), project.getId());
        if (tasks.isEmpty()) {
            return 0.0;
        }
        long completed = tasks.stream().filter(task -> task.getStatus() == TaskStatus.COMPLETED).count();
        return Math.round(((completed * 100.0) / tasks.size()) * 10.0) / 10.0;
    }

    private Project getProjectForCurrentTenant(Long projectId) {
        String tenantId = UserService.resolveTenant(null);
        return projectRepository.findByIdAndTenantId(projectId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found for current tenant"));
    }

    private void applyEmployees(Project project, List<Long> employeeIds) {
        Set<User> employees = new HashSet<>();
        if (employeeIds != null) {
            for (Long employeeId : employeeIds) {
                User employee = userRepository.findById(employeeId)
                        .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));
                if (!employee.getTenantId().equals(project.getTenantId())) {
                    throw new IllegalArgumentException("Employee belongs to a different tenant");
                }
                employees.add(employee);
            }
        }
        project.setEmployees(employees);
    }

    private ProjectResponse toResponse(Project project) {
        List<UserResponse> employees = new ArrayList<>();
        for (User employee : project.getEmployees()) {
            employees.add(new UserResponse(employee.getId(), employee.getUsername(), employee.getRole().name(), employee.getTenantId()));
        }
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.isArchived(),
                project.getTenantId(),
                employees,
                getProjectProgress(project.getId())
        );
    }
}
