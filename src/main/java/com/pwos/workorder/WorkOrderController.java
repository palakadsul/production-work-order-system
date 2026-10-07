package com.pwos.workorder;

import com.pwos.item.ItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/orders")
public class WorkOrderController {

    @Autowired
    private WorkOrderRepository workOrderRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private WorkOrderService workOrderService;

    @GetMapping
    public String listOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q,
            Model model) {
        String statusFilter = (status == null) ? "" : status.trim();
        String search = (q == null) ? "" : q.trim();
        String needle = search.toLowerCase();

        List<WorkOrder> allOrders = workOrderRepository.findAll();

        List<WorkOrder> orders = allOrders.stream()
                .filter(o -> statusFilter.isEmpty()
                        || statusFilter.equals(o.getStatus()))
                .filter(o -> needle.isEmpty()
                        || o.getItem().getName().toLowerCase().contains(needle)
                        || o.getItem().getSku().toLowerCase().contains(needle))
                .sorted(Comparator.comparing(WorkOrder::getId))
                .collect(Collectors.toList());

        LocalDate today = LocalDate.now();
        List<WorkOrder> overdueOrders = allOrders.stream()
                .filter(o -> o.getDueDate() != null
                        && o.getDueDate().isBefore(today)
                        && WorkOrderService.isOpen(o))
                .collect(Collectors.toList());

        model.addAttribute("orders", orders);
        model.addAttribute("items", itemRepository.findAll());
        model.addAttribute("statusFilter", statusFilter);
        model.addAttribute("q", search);
        model.addAttribute("overdueOrders", overdueOrders);
        return "orders";
    }

    @PostMapping
    public String createOrder(@RequestParam Long itemId,
                              @RequestParam Integer quantity,
                              @RequestParam(required = false) LocalDate dueDate,
                              RedirectAttributes redirect) {
        try {
            WorkOrder o = workOrderService.create(itemId, quantity, dueDate);
            redirect.addFlashAttribute("message",
                    "Work order " + o.getId() + " created.");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("formError", ex.getMessage());
        }
        return "redirect:/orders";
    }

    @PostMapping("/{orderId}/status")
    public String updateStatus(@PathVariable("orderId") Long orderId,
                               @RequestParam String status,
                               RedirectAttributes redirect) {
        try {
            workOrderService.changeStatus(orderId, status);
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("formError", ex.getMessage());
        }
        return "redirect:/orders";
    }

    @GetMapping("/{orderId}/edit")
    public String editForm(@PathVariable("orderId") Long orderId,
                           Model model, RedirectAttributes redirect) {
        WorkOrder order = workOrderRepository.findById(orderId).orElse(null);
        if (order == null || !WorkOrderService.isOpen(order)) {
            redirect.addFlashAttribute("formError",
                    "Only pending or in-progress orders can be edited.");
            return "redirect:/orders";
        }
        model.addAttribute("order", order);
        return "order-edit";
    }

    @PostMapping("/{orderId}")
    public String updateOrder(@PathVariable("orderId") Long orderId,
                              @RequestParam Integer quantity,
                              @RequestParam(required = false) LocalDate dueDate,
                              RedirectAttributes redirect) {
        try {
            workOrderService.update(orderId, quantity, dueDate);
            redirect.addFlashAttribute("message",
                    "Work order " + orderId + " updated.");
            return "redirect:/orders";
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("formError", ex.getMessage());
            return "redirect:/orders/" + orderId + "/edit";
        }
    }

    @PostMapping("/{orderId}/cancel")
    public String cancelOrder(@PathVariable("orderId") Long orderId,
                              RedirectAttributes redirect) {
        try {
            workOrderService.cancel(orderId);
            redirect.addFlashAttribute("message",
                    "Work order " + orderId + " cancelled.");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("formError", ex.getMessage());
        }
        return "redirect:/orders";
    }

    @PostMapping("/{orderId}/delete")
    public String deleteOrder(@PathVariable("orderId") Long orderId,
                              RedirectAttributes redirect) {
        try {
            workOrderService.delete(orderId);
            redirect.addFlashAttribute("message",
                    "Work order " + orderId + " deleted.");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("formError", ex.getMessage());
        }
        return "redirect:/orders";
    }
}
