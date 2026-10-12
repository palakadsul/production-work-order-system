package com.pwos.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Set;
import java.util.stream.Collectors;

/** Gives every page the user's name, role and what that role may do. */
@ControllerAdvice
public class CurrentUserAdvice {

    @ModelAttribute
    public void addCurrentUser(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean loggedIn = auth != null && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken);
        Set<String> roles = loggedIn
                ? auth.getAuthorities().stream()
                      .map(GrantedAuthority::getAuthority)
                      .collect(Collectors.toSet())
                : Set.of();
        boolean admin = roles.contains("ROLE_ADMIN");

        model.addAttribute("currentUser", loggedIn ? auth.getName() : null);
        model.addAttribute("currentRole", roles.stream()
                .map(r -> r.replace("ROLE_", ""))
                .findFirst().orElse(""));
        model.addAttribute("isAdmin", admin);
        model.addAttribute("canManageItems", admin || roles.contains("ROLE_CLERK"));
        model.addAttribute("canManageOrders", admin || roles.contains("ROLE_SUPERVISOR"));
        model.addAttribute("canViewHistory", admin || roles.contains("ROLE_VIEWER"));
    }
}
