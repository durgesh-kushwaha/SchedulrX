package com.examscheduler.api.dto;

import java.util.List;

public record AuthSessionResponse(
    String username,
    List<String> roles
) {}