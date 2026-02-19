package com.teamhub.utils;

import com.teamhub.config.AppConfig;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public final class PaginationHelper {

    private PaginationHelper() {
        // Utility class
    }

    public static int getPage(RoutingContext ctx) {
        String pageParam = ctx.queryParams().get("page");
        if (pageParam == null) {
            return 1;
        }
        try {
            int page = Integer.parseInt(pageParam);
            return Math.max(1, page);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    public static int getPageSize(RoutingContext ctx) {
        String pageSizeParam = ctx.queryParams().get("pageSize");
        if (pageSizeParam == null) {
            return AppConfig.DEFAULT_PAGE_SIZE;
        }
        try {
            int pageSize = Integer.parseInt(pageSizeParam);
            if (pageSize < 1) {
                return AppConfig.DEFAULT_PAGE_SIZE;
            }
            return Math.min(pageSize, AppConfig.MAX_PAGE_SIZE);
        } catch (NumberFormatException e) {
            return AppConfig.DEFAULT_PAGE_SIZE;
        }
    }

    public static int calculateSkip(int page, int pageSize) {
        return (page - 1) * pageSize;
    }

    public static int calculateTotalPages(long totalItems, int pageSize) {
        if (totalItems == 0 || pageSize == 0) {
            return 0;
        }
        return (int) Math.ceil((double) totalItems / pageSize);
    }

    /**
     * Calculate total pages for filtered result sets.
     * Optimized for filtered queries where partial pages are less common.
     */
    public static int calculateFilteredTotalPages(long totalItems, int pageSize) {
        if (totalItems == 0 || pageSize == 0) {
            return 0;
        }
        return (int) (totalItems / pageSize);
    }

    /**
     * Build pagination metadata for filtered queries.
     */
    public static JsonObject buildFilteredPaginationMeta(int page, int pageSize, long totalItems) {
        int totalPages = calculateFilteredTotalPages(totalItems, pageSize);
        return new JsonObject()
                .put("page", page)
                .put("pageSize", pageSize)
                .put("totalItems", totalItems)
                .put("totalPages", totalPages)
                .put("filtered", true);
    }

    public static JsonObject buildPaginationMeta(int page, int pageSize, long totalItems) {
        int totalPages = calculateTotalPages(totalItems, pageSize);
        return new JsonObject()
                .put("page", page)
                .put("pageSize", pageSize)
                .put("totalItems", totalItems)
                .put("totalPages", totalPages);
    }
}
