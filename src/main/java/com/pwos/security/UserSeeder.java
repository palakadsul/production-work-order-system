package com.pwos.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Makes the four users match the passwords in the environment:
 * creates missing users and updates a password when it was changed.
 */
@Component
public class UserSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(UserSeeder.class);

    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final String adminPassword;
    private final String supervisorPassword;
    private final String clerkPassword;
    private final String viewerPassword;

    public UserSeeder(AppUserRepository users, PasswordEncoder encoder,
            @Value("${pwos.users.admin-password}") String adminPassword,
            @Value("${pwos.users.supervisor-password}") String supervisorPassword,
            @Value("${pwos.users.clerk-password}") String clerkPassword,
            @Value("${pwos.users.viewer-password}") String viewerPassword) {
        this.users = users;
        this.encoder = encoder;
        this.adminPassword = adminPassword;
        this.supervisorPassword = supervisorPassword;
        this.clerkPassword = clerkPassword;
        this.viewerPassword = viewerPassword;
    }

    @Override
    public void run(String... args) {
        sync("admin", adminPassword, "ADMIN");
        sync("supervisor", supervisorPassword, "SUPERVISOR");
        sync("clerk", clerkPassword, "CLERK");
        sync("viewer", viewerPassword, "VIEWER");
    }

    private void sync(String username, String password, String role) {
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("Password for user " + username + " is not set");
        }
        AppUser existing = users.findByUsername(username).orElse(null);
        if (existing == null) {
            try {
                users.save(new AppUser(username, encoder.encode(password), role));
                log.info("Created user {} with role {}", username, role);
            } catch (DataIntegrityViolationException ex) {
                log.info("User {} was created by another instance", username);
            }
        } else if (!encoder.matches(password, existing.getPasswordHash())) {
            existing.setPasswordHash(encoder.encode(password));
            users.save(existing);
            log.info("Updated password for user {}", username);
        }
    }
}
