package com.pwos.workorder;

import com.pwos.item.Item;
import com.pwos.item.ItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/orders")
public class WorkOrderController {

    @Autowired
    private WorkOrderRepository workOrderRepository;

    @Autowired
    private ItemRepository itemRepository;

    @GetMapping
    public String listOrders(@RequestParam(required = false) String status, Model model) {
        List<WorkOrder> orders;
        if (status != null && !status.isEmpty()) {
            orders = workOrderRepository.findByStatus(status);
        } else {
            orders = workOrderRepository.findAll();
        }
        model.addAttribute("orders", orders);
        model.addAttribute("items", itemRepository.findAll());
        model.addAttribute("statusFilter", status);

        List<WorkOrder> allOrders = workOrderRepository.findAll();
        LocalDate today = LocalDate.now();
        List<WorkOrder> overdueOrders = allOrders.stream()
                .filter(o -> o.getDueDate() != null
                        && o.getDueDate().isBefore(today)
                        && !"DONE".equals(o.getStatus()))
                .collect(Collectors.toList());
        model.addAttribute("overdueOrders", overdueOrders);

        return "orders";
    }

    @PostMapping
    public String createOrder(@RequestParam Long itemId,
                               @RequestParam Integer quantity,
                               @RequestParam(required = false) LocalDate dueDate) {
        Item item = itemRepository.findById(itemId).orElseThrow();
        WorkOrder order = new WorkOrder();
        order.setItem(item);
        order.setQuantity(quantity);
        order.setDueDate(dueDate);
        order.setStatus("PENDING");
        workOrderRepository.save(order);
        return "redirect:/orders";
    }

    @PostMapping("/{orderId}/status")
    public String updateStatus(@PathVariable("orderId") Long orderId, @RequestParam String status) {
        WorkOrder order = workOrderRepository.findById(orderId).orElseThrow();
        order.setStatus(status);
        workOrderRepository.save(order);
        return "redirect:/orders";
    }
}
