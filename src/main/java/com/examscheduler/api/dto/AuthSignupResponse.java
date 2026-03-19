package com.examscheduler.api.dto;

public record AuthSignupResponse(
    String message,
    String username,
    String role
) {}
