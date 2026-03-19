package com.examscheduler.service;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import com.examscheduler.api.dto.AuthLoginRequest;
import com.examscheduler.api.dto.AuthLoginResponse;
import com.examscheduler.security.JwtService;

class AuthServiceTest {

    @Test
    void loginReturnsTokenAndRoles() {
        AuthenticationManager authManager = mock(AuthenticationManager.class);
        JwtService jwtService = mock(JwtService.class);
        AuthService authService = new AuthService(authManager, jwtService);

        User principal = new User("admin", "x", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        when(authManager.authenticate(any())).thenReturn(authentication);
        when(jwtService.generateToken(any(), any())).thenReturn("token-123");

        AuthLoginResponse response = authService.login(new AuthLoginRequest("admin", "admin123"));

        assertNotNull(response);
        assertEquals("token-123", response.token());
        assertEquals("admin", response.username());
        assertEquals(List.of("ROLE_ADMIN"), response.roles());
    }
}
