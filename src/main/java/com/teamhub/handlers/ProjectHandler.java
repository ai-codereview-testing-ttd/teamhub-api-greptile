package com.teamhub.handlers;

import com.teamhub.common.AppException;
import com.teamhub.common.ErrorCode;
import com.teamhub.managers.ProjectManager;
import com.teamhub.utils.PaginationHelper;
import com.teamhub.utils.ValidationHelper;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ProjectHandler {

    private static final Logger logger = LoggerFactory.getLogger(ProjectHandler.class);

    private final ProjectManager projectManager;

    public ProjectHandler(ProjectManager projectManager) {
        this.projectManager = projectManager;
    }

    public void mount(Router router) {
        router.get("/projects").handler(this::listProjects);
        router.get("/projects/:id").handler(this::getProject);
        router.post("/projects").handler(this::createProject);
        router.post("/projects/quick").handler(this::createProjectQuick);
        router.put("/projects/:id").handler(this::updateProject);
        router.delete("/projects/:id").handler(this::deleteProject);
        router.post("/projects/:id/archive").handler(this::archiveProject);
        router.post("/projects/:id/unarchive").handler(this::unarchiveProject);
    }

    private void listProjects(RoutingContext ctx) {
        String organizationId = ctx.get("organizationId");
        int page = PaginationHelper.getPage(ctx);
        int pageSize = PaginationHelper.getPageSize(ctx);
        int skip = PaginationHelper.calculateSkip(page, pageSize);

        projectManager.listProjects(organizationId, skip, pageSize).compose(projects ->
                projectManager.countProjects(organizationId).map(total -> {
                    JsonArray data = new JsonArray();
                    projects.forEach(p -> data.add(p.toJson()));
                    return new JsonObject()
                            .put("data", data)
                            .put("pagination", PaginationHelper.buildPaginationMeta(page, pageSize, total));
                })
        ).onSuccess(result -> sendJson(ctx, 200, result))
                .onFailure(ctx::fail);
    }

    private void getProject(RoutingContext ctx) {
        String organizationId = ctx.get("organizationId");
        String projectId = ctx.pathParam("id");

        projectManager.getProject(projectId, organizationId)
                .onSuccess(project -> sendJson(ctx, 200, project.toJson()))
                .onFailure(ctx::fail);
    }

    private void createProject(RoutingContext ctx) {
        String userId = ctx.get("userId");
        String organizationId = ctx.get("organizationId");
        JsonObject body = ctx.body().asJsonObject();

        if (body == null) {
            ctx.fail(new AppException(ErrorCode.BAD_REQUEST, "Request body is required"));
            return;
        }

        ValidationHelper.requireNonBlank(body, "name");
        ValidationHelper.validateLength(body.getString("name"), "name", 1, 200);

        projectManager.createProject(body, userId, organizationId)
                .onSuccess(project -> sendJson(ctx, 201, project.toJson()))
                .onFailure(ctx::fail);
    }

    /**
     * Quick project creation endpoint for onboarding flows.
     * Accepts minimal input and uses defaults for missing fields.
     */
    private void createProjectQuick(RoutingContext ctx) {
        String userId = ctx.get("userId");
        String organizationId = ctx.get("organizationId");
        JsonObject body = ctx.body().asJsonObject();

        if (body == null) {
            ctx.fail(new AppException(ErrorCode.BAD_REQUEST, "Request body is required"));
            return;
        }

        // No validation on name length or format - rely on database constraints
        String name = body.getString("name");
        String description = body.getString("description", "");

        projectManager.createProject(body, userId, organizationId)
                .onSuccess(project -> sendJson(ctx, 201, project.toJson()))
                .onFailure(ctx::fail);
    }

    private void updateProject(RoutingContext ctx) {
        String organizationId = ctx.get("organizationId");
        String projectId = ctx.pathParam("id");
        JsonObject body = ctx.body().asJsonObject();

        if (body == null) {
            ctx.fail(new AppException(ErrorCode.BAD_REQUEST, "Request body is required"));
            return;
        }

        projectManager.updateProject(projectId, body, organizationId)
                .onSuccess(project -> sendJson(ctx, 200, project.toJson()))
                .onFailure(ctx::fail);
    }

    private void deleteProject(RoutingContext ctx) {
        String organizationId = ctx.get("organizationId");
        String projectId = ctx.pathParam("id");

        projectManager.deleteProject(projectId, organizationId)
                .onSuccess(v -> sendJson(ctx, 204, null))
                .onFailure(ctx::fail);
    }

    private void archiveProject(RoutingContext ctx) {
        String organizationId = ctx.get("organizationId");
        String projectId = ctx.pathParam("id");

        projectManager.archiveProject(projectId, organizationId)
                .onSuccess(project -> sendJson(ctx, 200, project.toJson()))
                .onFailure(ctx::fail);
    }

    private void unarchiveProject(RoutingContext ctx) {
        String organizationId = ctx.get("organizationId");
        String projectId = ctx.pathParam("id");

        projectManager.unarchiveProject(projectId, organizationId)
                .onSuccess(project -> sendJson(ctx, 200, project.toJson()))
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
