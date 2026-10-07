package com.pwos.item;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/items")
public class ItemController {

    @Autowired
    private ItemRepository itemRepository;

    @GetMapping
    public String listItems(
            @RequestParam(required = false) String q,
            Model model) {
        List<Item> allItems = itemRepository.findAll();
        String query = (q == null) ? "" : q.trim();

        List<Item> source = query.isEmpty()
                ? allItems
                : itemRepository
                    .findByNameContainingIgnoreCaseOrSkuContainingIgnoreCase(
                        query, query);
        List<Item> shown = source.stream()
                .sorted(Comparator.comparing(Item::getId))
                .collect(Collectors.toList());

        List<Item> lowStockItems = allItems.stream()
                .filter(i -> i.getQuantity() <= i.getReorderThreshold())
                .collect(Collectors.toList());

        model.addAttribute("items", shown);
        model.addAttribute("totalItems", allItems.size());
        model.addAttribute("lowStockItems", lowStockItems);
        model.addAttribute("q", query);
        model.addAttribute("newItem", new Item());
        return "items";
    }

    @PostMapping
    public String addItem(@ModelAttribute Item newItem,
                          RedirectAttributes redirect) {
        try {
            itemRepository.saveAndFlush(newItem);
            redirect.addFlashAttribute("message",
                    "Item " + newItem.getName() + " added.");
        } catch (DataIntegrityViolationException ex) {
            redirect.addFlashAttribute("formError",
                    "SKU " + newItem.getSku()
                    + " already exists. Use a different SKU.");
        }
        return "redirect:/items";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model,
                           RedirectAttributes redirect) {
        Item item = itemRepository.findById(id).orElse(null);
        if (item == null) {
            redirect.addFlashAttribute("formError", "Item not found.");
            return "redirect:/items";
        }
        model.addAttribute("item", item);
        return "item-edit";
    }

    @PostMapping("/{id}")
    public String updateItem(@PathVariable Long id,
                             @RequestParam String name,
                             @RequestParam String sku,
                             @RequestParam Integer quantity,
                             @RequestParam Integer reorderThreshold,
                             RedirectAttributes redirect) {
        Item item = itemRepository.findById(id).orElse(null);
        if (item == null) {
            redirect.addFlashAttribute("formError", "Item not found.");
            return "redirect:/items";
        }
        item.setName(name.trim());
        item.setSku(sku.trim());
        item.setQuantity(quantity);
        item.setReorderThreshold(reorderThreshold);
        try {
            itemRepository.saveAndFlush(item);
        } catch (DataIntegrityViolationException ex) {
            redirect.addFlashAttribute("formError",
                    "SKU " + sku + " is already used by another item.");
            return "redirect:/items/" + id + "/edit";
        }
        redirect.addFlashAttribute("message",
                "Item " + item.getName() + " updated.");
        return "redirect:/items";
    }

    @PostMapping("/{id}/delete")
    public String deleteItem(@PathVariable Long id,
                             RedirectAttributes redirect) {
        try {
            itemRepository.deleteById(id);
        } catch (DataIntegrityViolationException ex) {
            redirect.addFlashAttribute("deleteError",
                    "Cannot delete this item - it has existing "
                    + "work orders linked to it.");
        }
        return "redirect:/items";
    }
}
