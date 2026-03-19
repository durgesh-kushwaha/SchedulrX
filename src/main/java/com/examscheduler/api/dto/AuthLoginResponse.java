package com.examscheduler.api.dto;

import java.util.List;

public record AuthLoginResponse(
    String token,
    String username,
    List<String> roles
) {}
