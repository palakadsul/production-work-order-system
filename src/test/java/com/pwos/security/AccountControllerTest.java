package com.pwos.security;

import com.pwos.audit.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.security.Principal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Unit tests for the change-password rules. */
class AccountControllerTest {

    final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    AppUserRepository users;
    AuditService audit;
    AccountController controller;
    AppUser ravi;
    final Principal asRavi = () -> "ravi";

    @BeforeEach
    void setUp() {
        users = mock(AppUserRepository.class);
        audit = mock(AuditService.class);
        controller = new AccountController(users, encoder, audit);
        ravi = new AppUser("ravi", encoder.encode("oldpass123"), "CLERK");
        when(users.findByUsername("ravi")).thenReturn(Optional.of(ravi));
    }

    private Object change(Principal who, String current, String next, String confirm) {
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();
        controller.changePassword(current, next, confirm, who, ra);
        return ra.getFlashAttributes().get("error");
    }

    @Test
    void correctDetailsChangeThePassword() {
        assertNull(change(asRavi, "oldpass123", "newpass456", "newpass456"));
        assertTrue(encoder.matches("newpass456", ravi.getPasswordHash()));
        verify(audit).record(eq("UPDATE"), eq("USER"), any(), contains("changed their own password"));
    }

    @Test
    void wrongCurrentPasswordIsRefused() {
        assertEquals("Current password is wrong.", change(asRavi, "nope", "newpass456", "newpass456"));
        assertTrue(encoder.matches("oldpass123", ravi.getPasswordHash()));
    }

    @Test
    void shortOrMismatchedPasswordIsRefused() {
        assertNotNull(change(asRavi, "oldpass123", "short", "short"));
        assertNotNull(change(asRavi, "oldpass123", "newpass456", "different99"));
        verify(users, never()).save(any());
    }

    @Test
    void reusingTheSamePasswordIsRefused() {
        assertNotNull(change(asRavi, "oldpass123", "oldpass123", "oldpass123"));
    }

    @Test
    void builtInAccountsCannotChangeHere() {
        AppUser admin = new AppUser("admin", encoder.encode("x"), "ADMIN");
        when(users.findByUsername("admin")).thenReturn(Optional.of(admin));
        assertNotNull(change(() -> "admin", "x", "newpass456", "newpass456"));
        verify(users, never()).save(any());
    }
}
