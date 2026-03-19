package com.examscheduler.security;

import java.util.List;

public record AppUserPrincipal(
    String username,
    String passwordHash,
    boolean enabled,
    List<String> roles
) {}
