package com.releasepilot.query.model;

import java.util.List;

/**
 * Generic read model representing a single page of results, along with the
 * paging metadata required by API consumers to navigate further pages.
 *
 * @param items         the items contained in this page
 * @param page          the zero-based index of this page
 * @param size          the maximum number of items requested per page
 * @param totalElements the total number of elements across all pages
 * @param <T>           the type of item contained in this page
 */
public record PagedResult<T>(
        List<T> items,
        int page,
        int size,
        long totalElements
) {
}
