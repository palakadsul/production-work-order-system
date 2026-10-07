package com.pwos;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.pwos.item.Item;
import com.pwos.item.ItemRepository;
import com.pwos.workorder.WorkOrder;
import com.pwos.workorder.WorkOrderRepository;

@Controller
public class HomeController {

    private final ItemRepository itemRepository;
    private final WorkOrderRepository workOrderRepository;

    public HomeController(ItemRepository itemRepository,
                          WorkOrderRepository workOrderRepository) {
        this.itemRepository = itemRepository;
        this.workOrderRepository = workOrderRepository;
    }

    @GetMapping("/")
    public String home(Model model) {
        List<Item> items = itemRepository.findAll();
        List<WorkOrder> orders = workOrderRepository.findAll();
        LocalDate today = LocalDate.now();

        List<Item> lowStockItems = items.stream()
                .filter(i -> i.getQuantity() <= i.getReorderThreshold())
                .toList();

        List<WorkOrder> overdueOrders = orders.stream()
                .filter(o -> o.getDueDate() != null
                        && o.getDueDate().isBefore(today)
                        && !"DONE".equals(o.getStatus())
                        && !"CANCELLED".equals(o.getStatus()))
                .toList();

        long pending = orders.stream()
                .filter(o -> "PENDING".equals(o.getStatus())).count();
        long inProgress = orders.stream()
                .filter(o -> "IN_PROGRESS".equals(o.getStatus())).count();
        long done = orders.stream()
                .filter(o -> "DONE".equals(o.getStatus())).count();

        List<WorkOrder> recentOrders = orders.stream()
                .sorted(Comparator.comparing(WorkOrder::getId).reversed())
                .limit(5)
                .toList();

        model.addAttribute("totalItems", items.size());
        model.addAttribute("lowStockItems", lowStockItems);
        model.addAttribute("overdueOrders", overdueOrders);
        model.addAttribute("openOrders", pending + inProgress);
        model.addAttribute("pendingCount", pending);
        model.addAttribute("inProgressCount", inProgress);
        model.addAttribute("doneCount", done);
        model.addAttribute("recentOrders", recentOrders);
        return "index";
    }
}