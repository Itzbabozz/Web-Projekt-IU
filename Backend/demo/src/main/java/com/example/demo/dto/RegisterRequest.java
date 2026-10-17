package com.example.demo.dto;

public record RegisterRequest(String username, String password, String role, String tenantId) {
}
