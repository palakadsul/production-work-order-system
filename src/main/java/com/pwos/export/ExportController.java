package com.pwos.export;

import com.pwos.audit.AuditRepository;
import com.pwos.item.ItemRepository;
import com.pwos.workorder.WorkOrderRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/** Downloads of the lists as CSV files that open in Excel. */
@Controller
public class ExportController {

    private final CsvExportService csv;
    private final ItemRepository items;
    private final WorkOrderRepository orders;
    private final AuditRepository audit;

    public ExportController(CsvExportService csv, ItemRepository items,
                            WorkOrderRepository orders, AuditRepository audit) {
        this.csv = csv;
        this.items = items;
        this.orders = orders;
        this.audit = audit;
    }

    private static ResponseEntity<byte[]> file(String name, String body) {
        // The byte-order mark makes Excel read the file as UTF-8.
        byte[] bytes = ("\uFEFF" + body).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"pwos-" + name + "-" + LocalDate.now() + ".csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(bytes);
    }

    @GetMapping("/export/items.csv")
    public ResponseEntity<byte[]> items() {
        return file("items", csv.items(items.findAll(Sort.by("id"))));
    }

    @GetMapping("/export/orders.csv")
    public ResponseEntity<byte[]> orders() {
        return file("orders", csv.orders(orders.findAll(Sort.by("id"))));
    }

    @GetMapping("/export/history.csv")
    public ResponseEntity<byte[]> history() {
        return file("history", csv.history(audit.findTop200ByOrderByEventTimeDesc()));
    }
}
