package com.pwos;

import com.pwos.item.Item;
import com.pwos.workorder.WorkOrder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Data for the dashboard charts, drawn as plain HTML bars (no JavaScript needed). */
@Service
public class DashboardChartService {

    public record StatusBar(String label, long count, int percent, String css) {}

    public record StockBar(String name, int quantity, int threshold,
                           int widthPct, int thresholdPct, boolean low) {}

    static int pct(double part, double whole) {
        if (whole <= 0) return 0;
        return (int) Math.min(100, Math.round(part * 100.0 / whole));
    }

    public List<StatusBar> statusBars(List<WorkOrder> orders) {
        String[][] statuses = {
            {"PENDING", "Pending", "bar-pending"},
            {"IN_PROGRESS", "In Progress", "bar-progress"},
            {"DONE", "Done", "bar-done"},
            {"CANCELLED", "Cancelled", "bar-cancelled"}};
        List<StatusBar> bars = new ArrayList<>();
        for (String[] s : statuses) {
            long n = orders.stream().filter(o -> s[0].equals(o.getStatus())).count();
            bars.add(new StatusBar(s[1], n, pct(n, orders.size()), s[2]));
        }
        return bars;
    }

    /** Low-stock items first, then by name; widths share one scale so bars compare fairly. */
    public List<StockBar> stockBars(List<Item> items, int limit) {
        int scale = items.stream()
                .mapToInt(i -> Math.max(i.getQuantity(), i.getReorderThreshold()))
                .max().orElse(1);
        return items.stream()
                .sorted(Comparator.comparing((Item i) -> i.getQuantity() > i.getReorderThreshold())
                        .thenComparing(Item::getName, String.CASE_INSENSITIVE_ORDER))
                .limit(limit)
                .map(i -> new StockBar(i.getName(), i.getQuantity(), i.getReorderThreshold(),
                        pct(i.getQuantity(), scale), pct(i.getReorderThreshold(), scale),
                        i.getQuantity() <= i.getReorderThreshold()))
                .toList();
    }
}
