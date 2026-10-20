package com.pwos;

import com.pwos.item.Item;
import com.pwos.workorder.WorkOrder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DashboardChartServiceTest {

    final DashboardChartService charts = new DashboardChartService();

    private WorkOrder order(String status) {
        WorkOrder o = new WorkOrder();
        o.setStatus(status);
        return o;
    }

    @Test
    void statusBarsCountAndPercentages() {
        var bars = charts.statusBars(List.of(order("PENDING"), order("PENDING"), order("DONE"), order("CANCELLED")));
        assertEquals(4, bars.size());
        assertEquals(2, bars.get(0).count());
        assertEquals(50, bars.get(0).percent());
        assertEquals(0, bars.get(1).count());
        assertEquals(25, bars.get(2).percent());
    }

    @Test
    void noOrdersGivesZeroPercentNotAnError() {
        charts.statusBars(List.of()).forEach(b -> assertEquals(0, b.percent()));
    }

    @Test
    void stockBarsShareOneScaleAndFlagLowStock() {
        var bars = charts.stockBars(List.of(new Item("tiles", "1", 40, 5), new Item("bolts", "2", 3, 10)), 10);
        assertEquals("bolts", bars.get(0).name());
        assertTrue(bars.get(0).low());
        assertEquals(100, bars.get(1).widthPct());
        assertEquals(25, bars.get(0).thresholdPct());
        assertFalse(bars.get(1).low());
    }

    @Test
    void stockBarsRespectTheLimit() {
        var items = List.of(new Item("a", "1", 5, 1), new Item("b", "2", 5, 1), new Item("c", "3", 5, 1));
        assertEquals(2, charts.stockBars(items, 2).size());
    }
}
