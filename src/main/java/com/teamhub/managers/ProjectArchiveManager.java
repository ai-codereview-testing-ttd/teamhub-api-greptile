package com.teamhub.managers;

import com.teamhub.common.AppException;
import com.teamhub.common.ErrorCode;
import com.teamhub.models.Project;
import com.teamhub.repositories.ProjectRepository;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Business logic for project archival operations. Supports single and bulk
 * archive/restore with proper validation and audit logging.
 */
public class ProjectArchiveManager {

    private static final Logger logger = LoggerFactory.getLogger(ProjectArchiveManager.class);

    private final ProjectRepository projectRepository;

    public ProjectArchiveManager(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    /**
     * List archived projects for an organization.
     */
    public Future<List<Project>> listArchivedProjects(String organizationId, int skip, int limit) {
        return projectRepository.findArchived(organizationId, skip, limit)
                .map(docs -> docs.stream()
                        .map(Project::fromJson)
                        .toList());
    }

    /**
     * Count archived projects for pagination.
     */
    public Future<Long> countArchivedProjects(String organizationId) {
        return projectRepository.countArchived(organizationId);
    }

    /**
     * Check if a project belongs to the given organization.
     * Returns true if the project exists and belongs to the org, false otherwise.
     */
    public Future<Boolean> isProjectOwnedByOrg(String projectId, String organizationId) {
        return projectRepository.findById(projectId)
                .map(doc -> {
                    if (doc == null) {
                        return false;
                    }
                    return organizationId.equals(doc.getString("organizationId"));
                });
    }

    /**
     * Archive multiple projects in a single operation.
     * Each project is validated individually; the operation reports per-project results.
     */
    public Future<JsonObject> bulkArchive(List<String> projectIds, String organizationId, String userId) {
        List<Future<JsonObject>> archiveFutures = projectIds.stream()
                .map(id -> archiveSingleProject(id, organizationId, userId))
                .toList();

        return CompositeFuture.all(new ArrayList<>(archiveFutures))
                .map(cf -> {
                    JsonArray results = new JsonArray();
                    int successCount = 0;
                    int failureCount = 0;

                    for (int i = 0; i < projectIds.size(); i++) {
                        JsonObject result = cf.resultAt(i);
                        results.add(result);
                        if ("archived".equals(result.getString("status"))) {
                            successCount++;
                        } else {
                            failureCount++;
                        }
                    }

                    return new JsonObject()
                            .put("results", results)
                            .put("summary", new JsonObject()
                                    .put("total", projectIds.size())
                                    .put("archived", successCount)
                                    .put("failed", failureCount));
                });
    }

    /**
     * Restore multiple archived projects.
     */
    public Future<JsonObject> bulkRestore(List<String> projectIds, String organizationId, String userId) {
        List<Future<JsonObject>> restoreFutures = projectIds.stream()
                .map(id -> restoreSingleProject(id, organizationId, userId))
                .toList();

        return CompositeFuture.all(new ArrayList<>(restoreFutures))
                .map(cf -> {
                    JsonArray results = new JsonArray();
                    int successCount = 0;
                    int failureCount = 0;

                    for (int i = 0; i < projectIds.size(); i++) {
                        JsonObject result = cf.resultAt(i);
                        results.add(result);
                        if ("restored".equals(result.getString("status"))) {
                            successCount++;
                        } else {
                            failureCount++;
                        }
                    }

                    return new JsonObject()
                            .put("results", results)
                            .put("summary", new JsonObject()
                                    .put("total", projectIds.size())
                                    .put("restored", successCount)
                                    .put("failed", failureCount));
                });
    }

    /**
     * Get archive summary statistics for the organization.
     */
    public Future<JsonObject> getArchiveSummary(String organizationId) {
        return countArchivedProjects(organizationId)
                .compose(archivedCount -> projectRepository.countByOrganization(organizationId)
                        .map(totalCount -> new JsonObject()
                                .put("archivedProjects", archivedCount)
                                .put("activeProjects", totalCount)
                                .put("totalProjects", archivedCount + totalCount)));
    }

    /**
     * Archive a single project with proper status validation.
     * Returns a result object indicating success or failure with reason.
     */
    private Future<JsonObject> archiveSingleProject(String projectId, String organizationId, String userId) {
        return projectRepository.findById(projectId)
                .compose(doc -> {
                    if (doc == null) {
                        return Future.succeededFuture(buildResult(projectId, "failed", "Project not found"));
                    }

                    if (!organizationId.equals(doc.getString("organizationId"))) {
                        return Future.succeededFuture(buildResult(projectId, "failed", "Access denied"));
                    }

                    Project project = Project.fromJson(doc);
                    if (project.getStatus() == Project.Status.ARCHIVED) {
                        return Future.succeededFuture(buildResult(projectId, "skipped", "Already archived"));
                    }

                    JsonObject update = new JsonObject()
                            .put("status", Project.Status.ARCHIVED.name())
                            .put("archivedBy", userId)
                            .put("archivedAt", Instant.now().toString());

                    return projectRepository.update(projectId, update)
                            .map(v -> {
                                logger.info("Archived project: {} by user: {}", projectId, userId);
                                return buildResult(projectId, "archived", null);
                            });
                })
                .recover(err -> {
                    logger.error("Failed to archive project {}: {}", projectId, err.getMessage(), err);
                    return Future.succeededFuture(buildResult(projectId, "failed", err.getMessage()));
                });
    }

    /**
     * Restore a single archived project.
     */
    private Future<JsonObject> restoreSingleProject(String projectId, String organizationId, String userId) {
        return projectRepository.findById(projectId)
                .compose(doc -> {
                    if (doc == null) {
                        return Future.succeededFuture(buildResult(projectId, "failed", "Project not found"));
                    }

                    if (!organizationId.equals(doc.getString("organizationId"))) {
                        return Future.succeededFuture(buildResult(projectId, "failed", "Access denied"));
                    }

                    Project project = Project.fromJson(doc);
                    if (project.getStatus() != Project.Status.ARCHIVED) {
                        return Future.succeededFuture(buildResult(projectId, "skipped", "Not archived"));
                    }

                    JsonObject update = new JsonObject()
                            .put("status", Project.Status.ACTIVE.name())
                            .put("restoredBy", userId)
                            .put("restoredAt", Instant.now().toString());

                    return projectRepository.update(projectId, update)
                            .map(v -> {
                                logger.info("Restored project: {} by user: {}", projectId, userId);
                                return buildResult(projectId, "restored", null);
                            });
                })
                .recover(err -> {
                    logger.error("Failed to restore project {}: {}", projectId, err.getMessage(), err);
                    return Future.succeededFuture(buildResult(projectId, "failed", err.getMessage()));
                });
    }

    /**
     * Build a standardized result object for bulk operations.
     */
    private JsonObject buildResult(String projectId, String status, String reason) {
        JsonObject result = new JsonObject()
                .put("projectId", projectId)
                .put("status", status);
        if (reason != null) {
            result.put("reason", reason);
        }
        return result;
    }
}
