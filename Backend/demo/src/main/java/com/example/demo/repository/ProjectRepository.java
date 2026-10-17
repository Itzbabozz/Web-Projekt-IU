package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.Project;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByTenantIdOrderByNameAsc(String tenantId);
    Optional<Project> findByIdAndTenantId(Long id, String tenantId);
}
