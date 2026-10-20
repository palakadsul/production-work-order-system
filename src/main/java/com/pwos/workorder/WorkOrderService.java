package com.pwos.workorder;

import com.pwos.audit.AuditService;
import com.pwos.item.Item;
import com.pwos.item.ItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Business rules for work orders. Each method runs in one transaction:
 * the order, the item stock and the history line are saved together
 * or not at all.
 */
@Service
public class WorkOrderService {

    static final Set<String> SETTABLE = Set.of("PENDING", "IN_PROGRESS", "DONE");

    private final WorkOrderRepository orders;
    private final ItemRepository items;
    private final AuditService audit;

    public WorkOrderService(WorkOrderRepository orders, ItemRepository items,
                            AuditService audit) {
        this.orders = orders;
        this.items = items;
        this.audit = audit;
    }

    /** True if the due date is within [from, to]; open ends are ignored. No range means every order matches. */
    static boolean dueBetween(WorkOrder o, LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return true;
        }
        LocalDate due = o.getDueDate();
        if (due == null) {
            return false;
        }
        return (from == null || !due.isBefore(from)) && (to == null || !due.isAfter(to));
    }

    static boolean isOpen(WorkOrder o) {
        return "PENDING".equals(o.getStatus()) || "IN_PROGRESS".equals(o.getStatus());
    }

    private WorkOrder find(Long id) {
        return orders.findById(id).orElseThrow(
                () -> new IllegalArgumentException("Order " + id + " not found."));
    }

    private static String describe(WorkOrder o) {
        return o.getQuantity() + " x " + o.getItem().getName();
    }

    @Transactional
    public WorkOrder create(Long itemId, Integer quantity, LocalDate dueDate) {
        if (quantity == null || quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1.");
        }
        Item item = items.findById(itemId).orElseThrow(
                () -> new IllegalArgumentException("Item not found."));
        WorkOrder order = new WorkOrder();
        order.setItem(item);
        order.setQuantity(quantity);
        order.setDueDate(dueDate);
        order.setStatus("PENDING");
        WorkOrder saved = orders.save(order);
        audit.record("CREATE", "ORDER", saved.getId(), "Order for " + describe(saved)
                + (dueDate != null ? ", due " + dueDate : ""));
        return saved;
    }

    /** Done adds the produced quantity to stock; reopening takes it back. */
    @Transactional
    public void changeStatus(Long orderId, String newStatus) {
        WorkOrder order = find(orderId);
        if (!SETTABLE.contains(newStatus)) {
            throw new IllegalArgumentException("Unknown status: " + newStatus);
        }
        String old = order.getStatus();
        if ("CANCELLED".equals(old)) {
            throw new IllegalArgumentException("A cancelled order cannot be changed.");
        }
        if (old.equals(newStatus)) {
            return;
        }
        Item item = order.getItem();
        int stockBefore = item.getQuantity();
        if ("DONE".equals(newStatus)) {
            item.setQuantity(item.getQuantity() + order.getQuantity());
        } else if ("DONE".equals(old)) {
            if (item.getQuantity() < order.getQuantity()) {
                throw new IllegalArgumentException("Cannot reopen order " + orderId
                        + ": only " + item.getQuantity() + " of " + item.getName()
                        + " left in stock.");
            }
            item.setQuantity(item.getQuantity() - order.getQuantity());
        }
        order.setStatus(newStatus);
        items.save(item);
        orders.save(order);

        String stockNote = stockBefore == item.getQuantity() ? ""
                : "; stock " + stockBefore + " -> " + item.getQuantity();
        audit.record("STATUS", "ORDER", orderId, describe(order) + ": "
                + old + " -> " + newStatus + stockNote);
    }

    @Transactional
    public void update(Long orderId, Integer quantity, LocalDate dueDate) {
        WorkOrder order = find(orderId);
        if (!isOpen(order)) {
            throw new IllegalArgumentException(
                    "Only pending or in-progress orders can be edited.");
        }
        if (quantity == null || quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1.");
        }
        List<String> changes = new ArrayList<>();
        if (!Objects.equals(order.getQuantity(), quantity)) {
            changes.add("quantity " + order.getQuantity() + " -> " + quantity);
        }
        if (!Objects.equals(order.getDueDate(), dueDate)) {
            changes.add("due date " + order.getDueDate() + " -> " + dueDate);
        }
        order.setQuantity(quantity);
        order.setDueDate(dueDate);
        orders.save(order);
        audit.record("UPDATE", "ORDER", orderId, order.getItem().getName() + ": "
                + (changes.isEmpty() ? "no changes" : String.join("; ", changes)));
    }

    @Transactional
    public void cancel(Long orderId) {
        WorkOrder order = find(orderId);
        if (!isOpen(order)) {
            throw new IllegalArgumentException(
                    "Only pending or in-progress orders can be cancelled.");
        }
        String old = order.getStatus();
        order.setStatus("CANCELLED");
        orders.save(order);
        audit.record("CANCEL", "ORDER", orderId, describe(order) + ": "
                + old + " -> CANCELLED");
    }

    @Transactional
    public void delete(Long orderId) {
        WorkOrder order = find(orderId);
        if ("DONE".equals(order.getStatus())) {
            throw new IllegalArgumentException(
                    "A finished order cannot be deleted. Reopen it first.");
        }
        audit.record("DELETE", "ORDER", orderId, "Deleted order for " + describe(order)
                + " (" + order.getStatus() + ")");
        orders.delete(order);
    }
}
