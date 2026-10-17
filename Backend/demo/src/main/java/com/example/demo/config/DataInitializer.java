package com.example.demo.config;

import com.example.demo.entity.Project;
import com.example.demo.entity.Role;
import com.example.demo.entity.Task;
import com.example.demo.entity.TaskStatus;
import com.example.demo.entity.User;
import com.example.demo.repository.ProjectRepository;
import com.example.demo.repository.TaskRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                          ProjectRepository projectRepository,
                          TaskRepository taskRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }

        User acmeAdmin = createUser("admin", "admin123", Role.ADMIN, "acme");
        User acmePm = createUser("pm1", "pm123", Role.PROJECT_MANAGER, "acme");
        User acmeEmployee = createUser("employee1", "emp123", Role.EMPLOYEE, "acme");
        User northwindAdmin = createUser("northwind-admin", "admin123", Role.ADMIN, "northwind");

        Project project = new Project();
        project.setName("Website Redesign");
        project.setDescription("Modernise the customer portal and delivery workflow.");
        project.setTenantId("acme");
        project.setEmployees(Set.of(acmePm, acmeEmployee));
        projectRepository.save(project);

        Task task1 = new Task();
        task1.setTitle("Create mockups");
        task1.setDescription("Create a clickable wireframe for the dashboard");
        task1.setStatus(TaskStatus.OPEN);
        task1.setTenantId("acme");
        task1.setProject(project);
        task1.setAssignee(acmeEmployee);
        taskRepository.save(task1);

        Task task2 = new Task();
        task2.setTitle("Development sprint");
        task2.setDescription("Complete the frontend implementation for the new homepage");
        task2.setStatus(TaskStatus.IN_PROGRESS);
        task2.setTenantId("acme");
        task2.setProject(project);
        task2.setAssignee(acmeEmployee);
        taskRepository.save(task2);

        Task task3 = new Task();
        task3.setTitle("QA review");
        task3.setDescription("Finalize the release checklist and test cases");
        task3.setStatus(TaskStatus.COMPLETED);
        task3.setTenantId("acme");
        task3.setProject(project);
        task3.setAssignee(acmePm);
        taskRepository.save(task3);

        Project secondProject = new Project();
        secondProject.setName("Mobile App Launch");
        secondProject.setDescription("Coordinate the rollout and support tasks.");
        secondProject.setTenantId("northwind");
        secondProject.setEmployees(Set.of(northwindAdmin));
        projectRepository.save(secondProject);

        Task secondTask = new Task();
        secondTask.setTitle("Prepare launch assets");
        secondTask.setDescription("Gather marketing and release materials");
        secondTask.setStatus(TaskStatus.OPEN);
        secondTask.setTenantId("northwind");
        secondTask.setProject(secondProject);
        secondTask.setAssignee(northwindAdmin);
        taskRepository.save(secondTask);
    }

    private User createUser(String username, String password, Role role, String tenantId) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setTenantId(tenantId);
        return userRepository.save(user);
    }
}
