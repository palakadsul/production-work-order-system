package com.pwos.security;

import com.pwos.audit.AuditService;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/** Admin-only page to manage logins: create users, change roles, reset passwords, enable or disable. */
@Controller
@RequestMapping("/users")
public class UserAdminController {

    static final List<String> ROLES = List.of("ADMIN", "SUPERVISOR", "CLERK", "VIEWER");

    /** Built-in accounts: passwords come from the secrets file and the tests rely on their roles. */
    static final Set<String> BUILT_IN = Set.of("admin", "supervisor", "clerk", "viewer");

    private static final Pattern USERNAME = Pattern.compile("[a-z0-9._-]{3,30}");

    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final AuditService audit;

    public UserAdminController(AppUserRepository users, PasswordEncoder encoder, AuditService audit) {
        this.users = users;
        this.encoder = encoder;
        this.audit = audit;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("users", users.findAll(Sort.by("username")));
        model.addAttribute("roles", ROLES);
        model.addAttribute("builtIn", BUILT_IN);
        return "users";
    }

    @PostMapping
    @Transactional
    public String create(@RequestParam String username, @RequestParam String password,
                         @RequestParam String role, RedirectAttributes ra) {
        String name = username == null ? "" : username.trim().toLowerCase();
        String error;
        if (!USERNAME.matcher(name).matches()) {
            error = "Username must be 3 to 30 characters: lowercase letters, digits, dot, dash or underscore.";
        } else if (users.findByUsername(name).isPresent()) {
            error = "User " + name + " already exists.";
        } else if (!ROLES.contains(role)) {
            error = "Unknown role.";
        } else {
            error = checkPassword(password);
        }
        if (error != null) {
            ra.addFlashAttribute("error", error);
            return "redirect:/users";
        }
        AppUser u = users.save(new AppUser(name, encoder.encode(password), role));
        audit.record("CREATE", "USER", u.getId(), "Created user " + name + " with role " + role);
        ra.addFlashAttribute("message", "User " + name + " created with role " + role + ".");
        return "redirect:/users";
    }

    @PostMapping("/{id}/role")
    @Transactional
    public String changeRole(@PathVariable Long id, @RequestParam String role,
                             Principal me, RedirectAttributes ra) {
        AppUser u = users.findById(id).orElse(null);
        String error = guard(u, me);
        if (error == null && !ROLES.contains(role)) {
            error = "Unknown role.";
        }
        if (error != null) {
            ra.addFlashAttribute("error", error);
            return "redirect:/users";
        }
        String old = u.getRole();
        if (!old.equals(role)) {
            u.setRole(role);
            users.save(u);
            audit.record("UPDATE", "USER", id, "Role of " + u.getUsername() + ": " + old + " -> " + role);
        }
        ra.addFlashAttribute("message", "Role of " + u.getUsername() + " is now " + role
                + ". It applies from their next sign-in.");
        return "redirect:/users";
    }

    @PostMapping("/{id}/password")
    @Transactional
    public String resetPassword(@PathVariable Long id, @RequestParam String password,
                                Principal me, RedirectAttributes ra) {
        AppUser u = users.findById(id).orElse(null);
        String error = guard(u, me);
        if (error == null) {
            error = checkPassword(password);
        }
        if (error != null) {
            ra.addFlashAttribute("error", error);
            return "redirect:/users";
        }
        u.setPasswordHash(encoder.encode(password));
        users.save(u);
        audit.record("UPDATE", "USER", id, "Password reset for " + u.getUsername());
        ra.addFlashAttribute("message", "Password of " + u.getUsername() + " was reset.");
        return "redirect:/users";
    }

    @PostMapping("/{id}/toggle")
    @Transactional
    public String toggle(@PathVariable Long id, Principal me, RedirectAttributes ra) {
        AppUser u = users.findById(id).orElse(null);
        String error = guard(u, me);
        if (error != null) {
            ra.addFlashAttribute("error", error);
            return "redirect:/users";
        }
        boolean enable = !u.isEnabled();
        u.setEnabled(enable);
        users.save(u);
        audit.record("STATUS", "USER", id, (enable ? "Enabled" : "Disabled") + " user " + u.getUsername());
        ra.addFlashAttribute("message", "User " + u.getUsername() + (enable ? " enabled." : " disabled."));
        return "redirect:/users";
    }

    private String guard(AppUser u, Principal me) {
        if (u == null) {
            return "User not found.";
        }
        if (BUILT_IN.contains(u.getUsername())) {
            return "Built-in accounts are fixed: their passwords come from the secrets file.";
        }
        if (me != null && me.getName().equals(u.getUsername())) {
            return "You cannot change your own account.";
        }
        return null;
    }

    private static String checkPassword(String password) {
        if (password == null || password.length() < 8) {
            return "Password must be at least 8 characters.";
        }
        return null;
    }
}
