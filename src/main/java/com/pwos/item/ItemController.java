package com.pwos.item;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/items")
public class ItemController {

    @Autowired
    private ItemRepository itemRepository;

    @GetMapping
    public String listItems(Model model) {
        List<Item> allItems = itemRepository.findAll();
        model.addAttribute("items", allItems);
        model.addAttribute("newItem", new Item());

        List<Item> lowStockItems = allItems.stream()
                .filter(item -> item.getQuantity() <= item.getReorderThreshold())
                .collect(Collectors.toList());
        model.addAttribute("lowStockItems", lowStockItems);

        return "items";
    }

    @PostMapping
    public String addItem(@ModelAttribute Item newItem) {
        itemRepository.save(newItem);
        return "redirect:/items";
    }

    @PostMapping("/{id}/delete")
    public String deleteItem(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            itemRepository.deleteById(id);
        } catch (DataIntegrityViolationException ex) {
            redirectAttributes.addFlashAttribute("deleteError",
                    "Cannot delete this item — it has existing work orders linked to it.");
        }
        return "redirect:/items";
    }
}
