package com.examscheduler.api.dto;

import java.time.Instant;

public record AuditLogResponse(
    long id,
    String actionType,
    String actionDetails,
    String entityType,
    String entityId,
    String actorUsername,
    Instant createdAt
) {}
