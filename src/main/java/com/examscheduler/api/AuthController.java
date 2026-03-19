package com.examscheduler.api;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.examscheduler.api.dto.AuthLoginRequest;
import com.examscheduler.api.dto.AuthLoginResponse;
import com.examscheduler.api.dto.AuthSessionResponse;
import com.examscheduler.api.dto.AuthSignupRequest;
import com.examscheduler.api.dto.AuthSignupResponse;
import com.examscheduler.service.AuthService;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthLoginResponse> login(@Validated @RequestBody AuthLoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthSignupResponse> signup(@Validated @RequestBody AuthSignupRequest request) throws Exception {
        return ResponseEntity.ok(authService.signup(request));
    }

    @GetMapping("/session")
    public ResponseEntity<AuthSessionResponse> session(Authentication authentication) {
        List<String> roles = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        return ResponseEntity.ok(new AuthSessionResponse(authentication.getName(), roles));
    }
}
