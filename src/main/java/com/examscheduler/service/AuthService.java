package com.examscheduler.service;

import java.sql.SQLException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.examscheduler.api.dto.AuthLoginRequest;
import com.examscheduler.api.dto.AuthLoginResponse;
import com.examscheduler.api.dto.AuthSignupRequest;
import com.examscheduler.api.dto.AuthSignupResponse;
import com.examscheduler.dao.AppUserDAO;
import com.examscheduler.security.JwtService;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AppUserDAO appUserDAO;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AuthService(AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       AppUserDAO appUserDAO,
                       PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.appUserDAO = appUserDAO;
        this.passwordEncoder = passwordEncoder;
    }

    // Test-friendly constructor kept for existing unit tests.
    public AuthService(AuthenticationManager authenticationManager, JwtService jwtService) {
        this(authenticationManager, jwtService, null, null);
    }

    public AuthLoginResponse login(AuthLoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        UserDetails principal = (UserDetails) authentication.getPrincipal();
        List<String> roles = principal.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        String token = jwtService.generateToken(principal.getUsername(), roles);

        return new AuthLoginResponse(token, principal.getUsername(), roles);
    }

    public AuthSignupResponse signup(AuthSignupRequest request) throws SQLException {
        if (appUserDAO == null || passwordEncoder == null) {
            throw new IllegalStateException("Signup dependencies are not initialized");
        }

        if (appUserDAO.findByUsername(request.username()) != null) {
            throw new IllegalArgumentException("Username already exists");
        }

        String normalizedRole = normalizeRole(request.role());
        String passwordHash = passwordEncoder.encode(request.password());

        appUserDAO.createUserWithRole(request.username(), passwordHash, normalizedRole);
        return new AuthSignupResponse("User registered successfully", request.username(), normalizedRole);
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "ROLE_STUDENT";
        }

        String normalized = role.trim().toUpperCase();
        if (!normalized.startsWith("ROLE_")) {
            normalized = "ROLE_" + normalized;
        }

        return switch (normalized) {
            case "ROLE_STUDENT", "ROLE_TEACHER" -> normalized;
            default -> "ROLE_STUDENT";
        };
    }
}
