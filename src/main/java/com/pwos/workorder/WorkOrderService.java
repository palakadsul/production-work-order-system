package com.pwos.workorder;

import com.pwos.item.Item;
import com.pwos.item.ItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;

/**
 * Business rules for work orders. Each method runs in one
 * transaction: the order and the item stock are saved together
 * or not at all.
 */
@Service
public class WorkOrderService {

    static final Set<String> SETTABLE = Set.of("PENDING", "IN_PROGRESS", "DONE");

    private final WorkOrderRepository orders;
    private final ItemRepository items;

    public WorkOrderService(WorkOrderRepository orders, ItemRepository items) {
        this.orders = orders;
        this.items = items;
    }

    static boolean isOpen(WorkOrder o) {
        return "PENDING".equals(o.getStatus()) || "IN_PROGRESS".equals(o.getStatus());
    }

    private WorkOrder find(Long id) {
        return orders.findById(id).orElseThrow(
                () -> new IllegalArgumentException("Order " + id + " not found."));
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
        return orders.save(order);
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
        order.setQuantity(quantity);
        order.setDueDate(dueDate);
        orders.save(order);
    }

    @Transactional
    public void cancel(Long orderId) {
        WorkOrder order = find(orderId);
        if (!isOpen(order)) {
            throw new IllegalArgumentException(
                    "Only pending or in-progress orders can be cancelled.");
        }
        order.setStatus("CANCELLED");
        orders.save(order);
    }

    @Transactional
    public void delete(Long orderId) {
        WorkOrder order = find(orderId);
        if ("DONE".equals(order.getStatus())) {
            throw new IllegalArgumentException(
                    "A finished order cannot be deleted. Reopen it first.");
        }
        orders.delete(order);
    }
}
