package com.example.demo.dto;

public record TaskRequest(String title, String description, Long assigneeId, String status, Long projectId) {
}
