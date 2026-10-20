package com.pwos;

import org.springframework.stereotype.Service;

import java.util.List;

/** Splits a list into pages of 20. Called from the templates as @pagingService. */
@Service
public class PagingService {

    public static final int SIZE = 20;

    public record Page<T>(List<T> content, int page, int totalPages, int total, int from, int to) {
        public boolean hasPrev() { return page > 1; }
        public boolean hasNext() { return page < totalPages; }
    }

    /** requested may be null, a String, or the String[] Thymeleaf gives for ?page= */
    public <T> Page<T> page(List<T> list, Object requested) {
        int total = list == null ? 0 : list.size();
        int totalPages = Math.max(1, (total + SIZE - 1) / SIZE);
        int p = parse(requested);
        p = Math.max(1, Math.min(p, totalPages));
        int fromIdx = (p - 1) * SIZE;
        int toIdx = Math.min(fromIdx + SIZE, total);
        List<T> content = total == 0 ? List.of() : list.subList(fromIdx, toIdx);
        return new Page<>(content, p, totalPages, total, total == 0 ? 0 : fromIdx + 1, toIdx);
    }

    static int parse(Object requested) {
        Object v = requested;
        if (v instanceof String[] arr) v = arr.length > 0 ? arr[0] : null;
        if (v == null) return 1;
        try {
            return Integer.parseInt(v.toString().trim());
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
