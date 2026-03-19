package com.examscheduler.api.dto;

import java.time.Instant;

public record NotificationResponse(
    long id,
    String type,
    String title,
    String message,
    boolean isRead,
    Instant createdAt
) {}
