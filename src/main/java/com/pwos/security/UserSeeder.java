package com.pwos.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates the four demo users the first time the app starts. */
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
        create("admin", adminPassword, "ADMIN");
        create("supervisor", supervisorPassword, "SUPERVISOR");
        create("clerk", clerkPassword, "CLERK");
        create("viewer", viewerPassword, "VIEWER");
    }

    private void create(String username, String password, String role) {
        if (users.findByUsername(username).isPresent()) {
            return;
        }
        try {
            users.save(new AppUser(username, encoder.encode(password), role));
            log.info("Created user {} with role {}", username, role);
        } catch (DataIntegrityViolationException ex) {
            log.info("User {} was created by another instance", username);
        }
    }
}
