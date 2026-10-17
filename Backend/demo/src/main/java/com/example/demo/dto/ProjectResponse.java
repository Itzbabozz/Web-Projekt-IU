package com.example.demo.dto;

import java.util.List;

public record ProjectResponse(
        Long id,
        String name,
        String description,
        boolean archived,
        String tenantId,
        List<UserResponse> employees,
        Double progress
) {
}
