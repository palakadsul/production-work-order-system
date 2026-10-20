package com.pwos.export;

import com.pwos.item.Item;
import com.pwos.workorder.WorkOrder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvExportServiceTest {

    final CsvExportService csv = new CsvExportService();

    @Test
    void plainValuesAreUnchangedAndNullIsEmpty() {
        assertEquals("steel", CsvExportService.cell("steel"));
        assertEquals("", CsvExportService.cell(null));
        assertEquals("42", CsvExportService.cell(42));
    }

    @Test
    void commasAndQuotesAreEscaped() {
        assertEquals("\"bolts, M8\"", CsvExportService.cell("bolts, M8"));
        assertEquals("\"the \"\"best\"\" rod\"", CsvExportService.cell("the \"best\" rod"));
    }

    @Test
    void formulaInjectionIsBlocked() {
        assertEquals("'=SUM(A1:A9)", CsvExportService.cell("=SUM(A1:A9)"));
        assertEquals("'@cmd", CsvExportService.cell("@cmd"));
        assertEquals("'-5", CsvExportService.cell("-5"));
    }

    @Test
    void itemsCsvHasHeaderAndLowStockFlag() {
        Item low = new Item("bolts", "105", 3, 10);
        Item ok = new Item("tiles", "104", 40, 5);
        String out = csv.items(List.of(low, ok));
        String[] lines = out.split("\r\n");
        assertEquals(3, lines.length);
        assertTrue(lines[0].startsWith("ID,Name,SKU"));
        assertTrue(lines[1].endsWith(",YES"));
        assertTrue(lines[2].endsWith(",NO"));
    }

    @Test
    void orderWithoutDueDateHasEmptyCell() {
        WorkOrder o = new WorkOrder();
        o.setItem(new Item("steel", "101", 10, 5));
        o.setQuantity(5);
        o.setStatus("PENDING");
        String line = csv.orders(List.of(o)).split("\r\n")[1];
        assertEquals(",steel,101,5,,PENDING", line);
    }
}
