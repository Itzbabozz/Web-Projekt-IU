package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.Task;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByTenantIdAndProjectIdOrderByIdAsc(String tenantId, Long projectId);
    List<Task> findByTenantIdAndProjectId(String tenantId, Long projectId);
}
