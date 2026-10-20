package com.pwos.export;

import com.pwos.audit.AuditEntry;
import com.pwos.item.Item;
import com.pwos.workorder.WorkOrder;
import org.springframework.stereotype.Service;

import java.util.List;

/** Builds CSV text. Escapes commas and quotes, and blocks spreadsheet formula injection. */
@Service
public class CsvExportService {

    static String cell(Object value) {
        if (value == null) {
            return "";
        }
        String s = value.toString();
        if (!s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0) {
            s = "'" + s;
        }
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            s = "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    static String row(Object... values) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) b.append(',');
            b.append(cell(values[i]));
        }
        return b.append("\r\n").toString();
    }

    public String items(List<Item> items) {
        StringBuilder b = new StringBuilder(row("ID", "Name", "SKU", "Quantity", "Reorder threshold", "Low stock"));
        for (Item i : items) {
            boolean low = i.getQuantity() != null && i.getReorderThreshold() != null
                    && i.getQuantity() <= i.getReorderThreshold();
            b.append(row(i.getId(), i.getName(), i.getSku(), i.getQuantity(), i.getReorderThreshold(), low ? "YES" : "NO"));
        }
        return b.toString();
    }

    public String orders(List<WorkOrder> orders) {
        StringBuilder b = new StringBuilder(row("ID", "Item", "SKU", "Quantity", "Due date", "Status"));
        for (WorkOrder o : orders) {
            b.append(row(o.getId(), o.getItem().getName(), o.getItem().getSku(), o.getQuantity(), o.getDueDate(), o.getStatus()));
        }
        return b.toString();
    }

    public String history(List<AuditEntry> entries) {
        StringBuilder b = new StringBuilder(row("Time", "User", "Action", "Entity", "Entity ID", "Details"));
        for (AuditEntry e : entries) {
            b.append(row(e.getEventTime(), e.getUsername(), e.getAction(), e.getEntityType(), e.getEntityId(), e.getDetails()));
        }
        return b.toString();
    }
}
