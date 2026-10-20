package com.pwos;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class PagingServiceTest {

    final PagingService paging = new PagingService();
    final List<Integer> fiftyFive = IntStream.rangeClosed(1, 55).boxed().toList();

    @Test
    void firstPageHasTwentyRows() {
        var p = paging.page(fiftyFive, null);
        assertEquals(1, p.page());
        assertEquals(3, p.totalPages());
        assertEquals(20, p.content().size());
        assertEquals(1, p.from());
        assertEquals(20, p.to());
        assertFalse(p.hasPrev());
        assertTrue(p.hasNext());
    }

    @Test
    void lastPageHasTheRemainder() {
        var p = paging.page(fiftyFive, "3");
        assertEquals(15, p.content().size());
        assertEquals(41, p.from());
        assertEquals(55, p.to());
        assertFalse(p.hasNext());
    }

    @Test
    void outOfRangeOrInvalidPageIsClamped() {
        assertEquals(3, paging.page(fiftyFive, "99").page());
        assertEquals(1, paging.page(fiftyFive, "-4").page());
        assertEquals(1, paging.page(fiftyFive, "abc").page());
    }

    @Test
    void thymeleafStringArrayIsAccepted() {
        assertEquals(2, paging.page(fiftyFive, new String[] {"2"}).page());
    }

    @Test
    void emptyListIsOnePageWithNoRows() {
        var p = paging.page(List.of(), null);
        assertEquals(1, p.totalPages());
        assertEquals(0, p.content().size());
        assertEquals(0, p.from());
    }
}
