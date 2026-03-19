package com.examscheduler.security;

import java.sql.SQLException;
import java.util.StringJoiner;

import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.examscheduler.dao.AppUserDAO;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final AppUserDAO appUserDAO;

    public CustomUserDetailsService(AppUserDAO appUserDAO) {
        this.appUserDAO = appUserDAO;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            AppUserPrincipal appUser = appUserDAO.findByUsername(username);
            if (appUser == null) {
                throw new UsernameNotFoundException("User not found: " + username);
            }

            StringJoiner roleJoiner = new StringJoiner(",");
            appUser.roles().forEach(roleJoiner::add);

            return User.withUsername(appUser.username())
                .password(appUser.passwordHash())
                .disabled(!appUser.enabled())
                .authorities(AuthorityUtils.commaSeparatedStringToAuthorityList(roleJoiner.toString()))
                .build();
        } catch (SQLException e) {
            throw new UsernameNotFoundException("Unable to load user: " + username, e);
        }
    }
}
