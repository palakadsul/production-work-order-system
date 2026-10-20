package com.pwos.security;

import com.pwos.audit.AuditService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/** Lets any signed-in user change their own password. */
@Controller
@RequestMapping("/account")
public class AccountController {

    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final AuditService audit;

    public AccountController(AppUserRepository users, PasswordEncoder encoder, AuditService audit) {
        this.users = users;
        this.encoder = encoder;
        this.audit = audit;
    }

    @GetMapping
    public String form() {
        return "account";
    }

    @PostMapping("/password")
    @Transactional
    public String changePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 Principal me, RedirectAttributes ra) {
        AppUser u = users.findByUsername(me.getName()).orElse(null);
        String error = null;
        if (u == null) {
            error = "Account not found.";
        } else if (UserAdminController.BUILT_IN.contains(u.getUsername())) {
            error = "Built-in accounts get their password from the secrets file and cannot change it here.";
        } else if (!encoder.matches(currentPassword, u.getPasswordHash())) {
            error = "Current password is wrong.";
        } else if (newPassword == null || newPassword.length() < 8) {
            error = "New password must be at least 8 characters.";
        } else if (!newPassword.equals(confirmPassword)) {
            error = "New password and confirmation do not match.";
        } else if (encoder.matches(newPassword, u.getPasswordHash())) {
            error = "New password must be different from the current one.";
        }
        if (error != null) {
            ra.addFlashAttribute("error", error);
            return "redirect:/account";
        }
        u.setPasswordHash(encoder.encode(newPassword));
        users.save(u);
        audit.record("UPDATE", "USER", u.getId(), u.getUsername() + " changed their own password");
        ra.addFlashAttribute("message", "Your password has been changed.");
        return "redirect:/account";
    }
}
