package com.pwos.workorder;

import com.pwos.audit.AuditService;
import com.pwos.item.Item;
import com.pwos.item.ItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Unit tests for the work order rules. No database, no browser: repositories are mocks. */
@ExtendWith(MockitoExtension.class)
class WorkOrderServiceTest {

    @Mock WorkOrderRepository orders;
    @Mock ItemRepository items;
    @Mock AuditService audit;
    @InjectMocks WorkOrderService service;

    Item steel;

    @BeforeEach
    void setUp() {
        steel = new Item("steel", "S-1", 100, 10);
        steel.setId(1L);
        lenient().when(orders.save(any(WorkOrder.class))).thenAnswer(inv -> {
            WorkOrder o = inv.getArgument(0);
            if (o.getId() == null) o.setId(7L);
            return o;
        });
    }

    private WorkOrder order(String status, int qty) {
        WorkOrder o = new WorkOrder();
        o.setId(7L);
        o.setItem(steel);
        o.setQuantity(qty);
        o.setStatus(status);
        when(orders.findById(7L)).thenReturn(Optional.of(o));
        return o;
    }

    // ---------- create
    @Test
    void createStartsAsPendingAndWritesHistory() {
        when(items.findById(1L)).thenReturn(Optional.of(steel));
        WorkOrder o = service.create(1L, 5, LocalDate.of(2026, 10, 30));
        assertEquals("PENDING", o.getStatus());
        assertEquals(5, o.getQuantity());
        verify(audit).record(eq("CREATE"), eq("ORDER"), eq(7L), contains("5 x steel"));
    }

    @Test
    void createRejectsQuantityBelowOne() {
        assertThrows(IllegalArgumentException.class, () -> service.create(1L, 0, null));
        verify(orders, never()).save(any());
    }

    @Test
    void createRejectsUnknownItem() {
        when(items.findById(99L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.create(99L, 5, null));
    }

    // ---------- status and stock
    @Test
    void doneAddsOrderQuantityToStock() {
        order("IN_PROGRESS", 5);
        service.changeStatus(7L, "DONE");
        assertEquals(105, steel.getQuantity());
        verify(audit).record(eq("STATUS"), eq("ORDER"), eq(7L), contains("stock 100 -> 105"));
    }

    @Test
    void reopeningDoneOrderTakesStockBack() {
        steel.setQuantity(105);
        order("DONE", 5);
        service.changeStatus(7L, "IN_PROGRESS");
        assertEquals(100, steel.getQuantity());
    }

    @Test
    void reopeningIsRefusedWhenStockIsTooLow() {
        steel.setQuantity(3);
        WorkOrder o = order("DONE", 5);
        assertThrows(IllegalArgumentException.class, () -> service.changeStatus(7L, "PENDING"));
        assertEquals(3, steel.getQuantity());
        assertEquals("DONE", o.getStatus());
        verify(items, never()).save(any());
    }

    @Test
    void pendingToInProgressDoesNotTouchStock() {
        order("PENDING", 5);
        service.changeStatus(7L, "IN_PROGRESS");
        assertEquals(100, steel.getQuantity());
    }

    @Test
    void cancelledOrderCannotBeChanged() {
        order("CANCELLED", 5);
        assertThrows(IllegalArgumentException.class, () -> service.changeStatus(7L, "DONE"));
    }

    @Test
    void unknownStatusIsRejected() {
        order("PENDING", 5);
        assertThrows(IllegalArgumentException.class, () -> service.changeStatus(7L, "SHIPPED"));
    }

    @Test
    void sameStatusChangesNothing() {
        order("PENDING", 5);
        service.changeStatus(7L, "PENDING");
        verifyNoInteractions(audit);
    }

    // ---------- edit, cancel, delete
    @Test
    void editingADoneOrderIsRefused() {
        order("DONE", 5);
        assertThrows(IllegalArgumentException.class, () -> service.update(7L, 8, null));
    }

    @Test
    void cancelSetsCancelledStatus() {
        WorkOrder o = order("PENDING", 5);
        service.cancel(7L);
        assertEquals("CANCELLED", o.getStatus());
        verify(audit).record(eq("CANCEL"), eq("ORDER"), eq(7L), anyString());
    }

    @Test
    void finishedOrderCannotBeDeleted() {
        order("DONE", 5);
        assertThrows(IllegalArgumentException.class, () -> service.delete(7L));
        verify(orders, never()).delete(any());
    }

    @Test
    void editChangesQuantityAndDueDate() {
        WorkOrder o = order("PENDING", 5);
        LocalDate due = LocalDate.of(2026, 11, 1);
        service.update(7L, 8, due);
        assertEquals(8, o.getQuantity());
        assertEquals(due, o.getDueDate());
        verify(audit).record(eq("UPDATE"), eq("ORDER"), eq(7L), contains("quantity 5 -> 8"));
    }

    @Test
    void editRejectsQuantityBelowOne() {
        order("PENDING", 5);
        assertThrows(IllegalArgumentException.class, () -> service.update(7L, 0, null));
    }

    @Test
    void cancellingAFinishedOrderIsRefused() {
        order("DONE", 5);
        assertThrows(IllegalArgumentException.class, () -> service.cancel(7L));
    }

    @Test
    void pendingOrderCanBeDeleted() {
        WorkOrder o = order("PENDING", 5);
        service.delete(7L);
        verify(orders).delete(o);
        verify(audit).record(eq("DELETE"), eq("ORDER"), eq(7L), anyString());
    }
}
