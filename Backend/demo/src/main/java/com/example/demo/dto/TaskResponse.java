package com.example.demo.dto;

public record TaskResponse(
        Long id,
        Long projectId,
        String title,
        String description,
        String status,
        Long assigneeId,
        String assigneeName,
        String tenantId
) {
}
