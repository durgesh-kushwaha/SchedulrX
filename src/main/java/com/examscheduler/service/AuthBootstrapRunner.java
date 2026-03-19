package com.examscheduler.service;

import java.sql.SQLException;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.examscheduler.dao.AppUserDAO;

@Component
public class AuthBootstrapRunner implements CommandLineRunner {

    private final AppUserDAO appUserDAO;
    private final PasswordEncoder passwordEncoder;

    public AuthBootstrapRunner(AppUserDAO appUserDAO, PasswordEncoder passwordEncoder) {
        this.appUserDAO = appUserDAO;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        ensureRole("ROLE_ADMIN");
        ensureRole("ROLE_TEACHER");
        ensureRole("ROLE_STUDENT");

        if (appUserDAO.countUsers() == 0) {
            appUserDAO.createUserWithRole("admin", passwordEncoder.encode("admin123"), "ROLE_ADMIN");
            appUserDAO.createUserWithRole("teacher", passwordEncoder.encode("teacher123"), "ROLE_TEACHER");
            appUserDAO.createUserWithRole("student", passwordEncoder.encode("student123"), "ROLE_STUDENT");
        }
    }

    private void ensureRole(String roleName) throws SQLException {
        appUserDAO.upsertRole(roleName);
    }
}
