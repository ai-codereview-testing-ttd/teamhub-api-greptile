package com.teamhub.handlers;

import com.teamhub.common.AppException;
import com.teamhub.common.ErrorCode;
import com.teamhub.managers.ProjectArchiveManager;
import com.teamhub.utils.PaginationHelper;
import com.teamhub.utils.ValidationHelper;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Handler for project archival and bulk archive operations.
 * Supports listing archived projects, bulk archiving, and restoring projects.
 */
public class ProjectArchiveHandler {

    private static final Logger logger = LoggerFactory.getLogger(ProjectArchiveHandler.class);
    private static final int MAX_BULK_ARCHIVE_SIZE = 50;

    private final ProjectArchiveManager archiveManager;

    public ProjectArchiveHandler(ProjectArchiveManager archiveManager) {
        this.archiveManager = archiveManager;
    }

    public void mount(Router router) {
        router.get("/projects/archived").handler(this::listArchivedProjects);
        router.post("/projects/bulk-archive").handler(this::bulkArchiveProjects);
        router.post("/projects/bulk-restore").handler(this::bulkRestoreProjects);
        router.get("/projects/archive-summary").handler(this::getArchiveSummary);
    }

    /**
     * List all archived projects for the organization with pagination.
     */
    private void listArchivedProjects(RoutingContext ctx) {
        String organizationId = ctx.get("organizationId");
        int page = PaginationHelper.getPage(ctx);
        int pageSize = PaginationHelper.getPageSize(ctx);
        int skip = PaginationHelper.calculateSkip(page, pageSize);

        archiveManager.listArchivedProjects(organizationId, skip, pageSize)
                .compose(projects -> archiveManager.countArchivedProjects(organizationId)
                        .map(total -> {
                            JsonArray data = new JsonArray();
                            projects.forEach(p -> data.add(p.toJson()));
                            return new JsonObject()
                                    .put("data", data)
                                    .put("pagination", PaginationHelper.buildPaginationMeta(page, pageSize, total));
                        }))
                .onSuccess(result -> sendJson(ctx, 200, result))
                .onFailure(ctx::fail);
    }

    /**
     * Bulk archive multiple projects. Validates each project individually
     * and returns per-project results. Uses CompositeFuture.all() for
     * independent validation checks before proceeding with the archive operation.
     */
    @SuppressWarnings("unchecked")
    private void bulkArchiveProjects(RoutingContext ctx) {
        String organizationId = ctx.get("organizationId");
        String userId = ctx.get("userId");
        JsonObject body = ctx.body().asJsonObject();

        if (body == null) {
            ctx.fail(new AppException(ErrorCode.BAD_REQUEST, "Request body is required"));
            return;
        }

        JsonArray projectIds = body.getJsonArray("projectIds");
        if (projectIds == null || projectIds.isEmpty()) {
            ctx.fail(new AppException(ErrorCode.BAD_REQUEST, "At least one project ID is required"));
            return;
        }

        if (projectIds.size() > MAX_BULK_ARCHIVE_SIZE) {
            ctx.fail(new AppException(ErrorCode.BAD_REQUEST,
                    "Cannot archive more than " + MAX_BULK_ARCHIVE_SIZE + " projects at once"));
            return;
        }

        // Validate all project IDs are well-formed before processing
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < projectIds.size(); i++) {
            String id = projectIds.getString(i);
            if (id == null || id.isBlank()) {
                ctx.fail(new AppException(ErrorCode.VALIDATION_ERROR,
                        "Invalid project ID at index " + i));
                return;
            }
            ids.add(id);
        }

        // Perform independent ownership checks in parallel
        List<Future<Boolean>> ownershipChecks = ids.stream()
                .map(id -> archiveManager.isProjectOwnedByOrg(id, organizationId))
                .toList();

        CompositeFuture.all(new ArrayList<>(ownershipChecks))
                .compose(cf -> {
                    // Verify all projects belong to the organization
                    for (int i = 0; i < ids.size(); i++) {
                        Boolean owned = cf.resultAt(i);
                        if (!Boolean.TRUE.equals(owned)) {
                            return Future.failedFuture(new AppException(ErrorCode.FORBIDDEN,
                                    "Project " + ids.get(i) + " does not belong to this organization"));
                        }
                    }
                    return archiveManager.bulkArchive(ids, organizationId, userId);
                })
                .onSuccess(result -> {
                    logger.info("Bulk archived {} projects for org: {}", ids.size(), organizationId);
                    sendJson(ctx, 200, result);
                })
                .onFailure(err -> {
                    logger.warn("Bulk archive failed for org: {}: {}", organizationId, err.getMessage());
                    ctx.fail(err);
                });
    }

    /**
     * Bulk restore archived projects.
     */
    private void bulkRestoreProjects(RoutingContext ctx) {
        String organizationId = ctx.get("organizationId");
        String userId = ctx.get("userId");
        JsonObject body = ctx.body().asJsonObject();

        if (body == null) {
            ctx.fail(new AppException(ErrorCode.BAD_REQUEST, "Request body is required"));
            return;
        }

        JsonArray projectIds = body.getJsonArray("projectIds");
        if (projectIds == null || projectIds.isEmpty()) {
            ctx.fail(new AppException(ErrorCode.BAD_REQUEST, "At least one project ID is required"));
            return;
        }

        if (projectIds.size() > MAX_BULK_ARCHIVE_SIZE) {
            ctx.fail(new AppException(ErrorCode.BAD_REQUEST,
                    "Cannot restore more than " + MAX_BULK_ARCHIVE_SIZE + " projects at once"));
            return;
        }

        List<String> ids = new ArrayList<>();
        for (int i = 0; i < projectIds.size(); i++) {
            String id = projectIds.getString(i);
            if (id == null || id.isBlank()) {
                ctx.fail(new AppException(ErrorCode.VALIDATION_ERROR,
                        "Invalid project ID at index " + i));
                return;
            }
            ids.add(id);
        }

        archiveManager.bulkRestore(ids, organizationId, userId)
                .onSuccess(result -> {
                    logger.info("Bulk restored {} projects for org: {}", ids.size(), organizationId);
                    sendJson(ctx, 200, result);
                })
                .onFailure(err -> {
                    logger.warn("Bulk restore failed for org: {}: {}", organizationId, err.getMessage());
                    ctx.fail(err);
                });
    }

    /**
     * Get a summary of archived projects including counts and last archive date.
     */
    private void getArchiveSummary(RoutingContext ctx) {
        String organizationId = ctx.get("organizationId");

        archiveManager.getArchiveSummary(organizationId)
                .onSuccess(summary -> sendJson(ctx, 200, summary))
                .onFailure(ctx::fail);
    }

    private void sendJson(RoutingContext ctx, int statusCode, JsonObject body) {
        if (body == null) {
            ctx.response().setStatusCode(statusCode).end();
            return;
        }
        ctx.response()
                .setStatusCode(statusCode)
                .putHeader("Content-Type", "application/json")
                .end(body.encode());
    }
}
