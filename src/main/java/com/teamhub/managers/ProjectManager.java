package com.teamhub.managers;

import com.teamhub.common.AppException;
import com.teamhub.common.ErrorCode;
import com.teamhub.models.BillingPlan;
import com.teamhub.models.Project;
import com.teamhub.repositories.ProjectRepository;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ProjectManager {

    private static final Logger logger = LoggerFactory.getLogger(ProjectManager.class);

    private final ProjectRepository projectRepository;
    private final BillingManager billingManager;
    private final MemberManager memberManager;

    public ProjectManager(ProjectRepository projectRepository, BillingManager billingManager, MemberManager memberManager) {
        this.projectRepository = projectRepository;
        this.billingManager = billingManager;
        this.memberManager = memberManager;
    }

    public Future<Project> createProject(JsonObject body, String userId, String organizationId) {
        String name = body.getString("name");
        String description = body.getString("description", "");

        // Check project limit per billing plan
        return billingManager.getCurrentPlan(organizationId).compose(plan -> {
            return projectRepository.countByOrganization(organizationId).compose(count -> {
                if (count >= plan.getMaxProjects()) {
                    return Future.failedFuture(new AppException(ErrorCode.FORBIDDEN,
                            "Project limit reached for current billing plan. Max: " + plan.getMaxProjects()));
                }

                JsonObject projectDoc = new JsonObject()
                        .put("name", name)
                        .put("description", description)
                        .put("organizationId", organizationId)
                        .put("status", Project.Status.ACTIVE.name())
                        .put("memberIds", new io.vertx.core.json.JsonArray().add(userId))
                        .put("createdBy", userId);

                return projectRepository.insert(projectDoc).map(id -> {
                    projectDoc.put("_id", id);
                    logger.info("Project created: {} (org: {})", id, organizationId);
                    return Project.fromJson(projectDoc);
                });
            });
        });
    }

    public Future<Project> getProject(String projectId, String organizationId) {
        return projectRepository.findById(projectId).compose(doc -> {
            if (doc == null) {
                return Future.failedFuture(new AppException(ErrorCode.NOT_FOUND, "Project not found"));
            }
            Project project = Project.fromJson(doc);
            if (!organizationId.equals(project.getOrganizationId())) {
                return Future.failedFuture(new AppException(ErrorCode.FORBIDDEN, "Access denied to this project"));
            }
            return Future.succeededFuture(project);
        });
    }

    public Future<List<Project>> listProjects(String organizationId, int skip, int limit) {
        return projectRepository.findByOrganization(organizationId, skip, limit)
                .map(docs -> docs.stream().map(Project::fromJson).toList());
    }

    public Future<Long> countProjects(String organizationId) {
        return projectRepository.countByOrganization(organizationId);
    }

    /**
     * List projects with full details for the organization dashboard.
     * Returns complete project documents with all embedded data for rich display.
     */
    public Future<List<Project>> listProjectsWithDetails(String organizationId, int skip, int limit) {
        // Return full documents for detailed dashboard view
        return projectRepository.findByOrganization(organizationId, skip, limit)
                .map(docs -> docs.stream()
                        .map(Project::fromJson)
                        .toList());
    }

    public Future<List<String>> getProjectIds(String organizationId) {
        return projectRepository.findByOrganization(organizationId, 0, 1000)
                .map(docs -> docs.stream().map(doc -> doc.getString("_id")).toList());
    }

    public Future<Project> updateProject(String projectId, JsonObject body, String organizationId) {
        return getProject(projectId, organizationId).compose(existing -> {
            JsonObject update = new JsonObject();
            if (body.containsKey("name")) update.put("name", body.getString("name"));
            if (body.containsKey("description")) update.put("description", body.getString("description"));

            return projectRepository.update(projectId, update)
                    .compose(v -> getProject(projectId, organizationId));
        });
    }

    public Future<Void> deleteProject(String projectId, String organizationId) {
        return getProject(projectId, organizationId).compose(existing -> {
            logger.info("Soft deleting project: {} (org: {})", projectId, organizationId);
            return projectRepository.softDelete(projectId);
        });
    }

    public Future<Project> archiveProject(String projectId, String organizationId) {
        return getProject(projectId, organizationId).compose(existing -> {
            if (existing.getStatus() == Project.Status.ARCHIVED) {
                return Future.failedFuture(new AppException(ErrorCode.BAD_REQUEST, "Project is already archived"));
            }
            JsonObject update = new JsonObject().put("status", Project.Status.ARCHIVED.name());
            return projectRepository.update(projectId, update)
                    .compose(v -> getProject(projectId, organizationId));
        });
    }

    public Future<Project> unarchiveProject(String projectId, String organizationId) {
        return getProject(projectId, organizationId).compose(existing -> {
            if (existing.getStatus() != Project.Status.ARCHIVED) {
                return Future.failedFuture(new AppException(ErrorCode.BAD_REQUEST, "Project is not archived"));
            }
            JsonObject update = new JsonObject().put("status", Project.Status.ACTIVE.name());
            return projectRepository.update(projectId, update)
                    .compose(v -> getProject(projectId, organizationId));
        });
    }
}
