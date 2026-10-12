package com.pwos.audit;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HistoryController {

    private final AuditRepository repo;

    public HistoryController(AuditRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/history")
    public String history(Model model) {
        model.addAttribute("entries", repo.findTop200ByOrderByEventTimeDesc());
        return "history";
    }
}
