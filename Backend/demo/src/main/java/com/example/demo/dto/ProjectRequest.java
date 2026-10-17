package com.example.demo.dto;

import java.util.List;

public record ProjectRequest(String name, String description, List<Long> employeeIds, boolean archived) {
}
