package com.pwos.audit;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Writes history lines. Joins the caller's transaction, so a change
 * that is rolled back leaves no history line behind.
 */
@Service
public class AuditService {

    private final AuditRepository repo;

    public AuditService(AuditRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public void record(String action, String entityType, Long entityId, String details) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String user = (auth == null || auth instanceof AnonymousAuthenticationToken)
                ? "system" : auth.getName();
        String text = details == null ? "" : details;
        if (text.length() > 500) {
            text = text.substring(0, 497) + "...";
        }
        repo.save(new AuditEntry(LocalDateTime.now(), user, action, entityType, entityId, text));
    }
}
