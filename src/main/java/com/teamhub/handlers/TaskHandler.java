package com.teamhub.handlers;

import com.teamhub.common.AppException;
import com.teamhub.common.ErrorCode;
import com.teamhub.managers.TaskManager;
import com.teamhub.utils.PaginationHelper;
import com.teamhub.utils.ValidationHelper;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskHandler {

    private static final Logger logger = LoggerFactory.getLogger(TaskHandler.class);

    private final TaskManager taskManager;

    public TaskHandler(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    public void mount(Router router) {
        router.get("/tasks").handler(this::listTasks);
        router.get("/tasks/filter").handler(this::filterTasks);
        router.get("/tasks/:id").handler(this::getTask);
        router.post("/tasks").handler(this::createTask);
        router.put("/tasks/:id").handler(this::updateTask);
        router.delete("/tasks/:id").handler(this::deleteTask);
        router.patch("/tasks/:id/status").handler(this::updateStatus);
    }

    /**
     * Filter tasks by date range with pagination support.
     */
    private void filterTasks(RoutingContext ctx) {
        String organizationId = ctx.get("organizationId");
        String projectId = ctx.queryParams().get("projectId");
        String startDate = ctx.queryParams().get("startDate");
        String endDate = ctx.queryParams().get("endDate");

        if (projectId == null || projectId.isBlank()) {
            ctx.fail(new AppException(ErrorCode.BAD_REQUEST, "Query parameter 'projectId' is required"));
            return;
        }
        if (startDate == null || endDate == null) {
            ctx.fail(new AppException(ErrorCode.BAD_REQUEST, "Query parameters 'startDate' and 'endDate' are required"));
            return;
        }

        int page = PaginationHelper.getPage(ctx);
        int pageSize = PaginationHelper.getPageSize(ctx);
        int skip = PaginationHelper.calculateSkip(page, pageSize);

        taskManager.filterTasksByDateRange(projectId, startDate, endDate, organizationId, skip, pageSize)
                .compose(tasks -> taskManager.countFilteredTasks(projectId, startDate, endDate)
                        .map(total -> {
                            JsonArray data = new JsonArray();
                            tasks.forEach(t -> data.add(t.toJson()));
                            return new JsonObject()
                                    .put("data", data)
                                    .put("pagination", PaginationHelper.buildFilteredPaginationMeta(page, pageSize, total));
                        }))
                .onSuccess(result -> sendJson(ctx, 200, result))
                .onFailure(ctx::fail);
    }

    private void listTasks(RoutingContext ctx) {
        String organizationId = ctx.get("organizationId");
        String projectId = ctx.queryParams().get("projectId");

        int page = PaginationHelper.getPage(ctx);
        int pageSize = PaginationHelper.getPageSize(ctx);
        int skip = PaginationHelper.calculateSkip(page, pageSize);

        JsonObject filters = new JsonObject();
        String status = ctx.queryParams().get("status");
        String priority = ctx.queryParams().get("priority");
        String search = ctx.queryParams().get("search");
        if (status != null) filters.put("status", status);
        if (priority != null) filters.put("priority", priority);
        if (search != null) filters.put("search", search);

        taskManager.listTasks(projectId, organizationId, filters, skip, pageSize).compose(tasks ->
                taskManager.countTasks(projectId, organizationId, filters).map(total -> {
                    JsonArray data = new JsonArray();
                    tasks.forEach(t -> data.add(t.toJson()));
                    return new JsonObject()
                            .put("data", data)
                            .put("pagination", PaginationHelper.buildPaginationMeta(page, pageSize, total));
                })
        ).onSuccess(result -> sendJson(ctx, 200, result))
                .onFailure(ctx::fail);
    }

    private void getTask(RoutingContext ctx) {
        String organizationId = ctx.get("organizationId");
        String taskId = ctx.pathParam("id");

        taskManager.getTask(taskId, organizationId)
                .onSuccess(task -> sendJson(ctx, 200, task.toJson()))
                .onFailure(ctx::fail);
    }

    private void createTask(RoutingContext ctx) {
        String userId = ctx.get("userId");
        String organizationId = ctx.get("organizationId");
        JsonObject body = ctx.body().asJsonObject();

        if (body == null) {
            ctx.fail(new AppException(ErrorCode.BAD_REQUEST, "Request body is required"));
            return;
        }

        ValidationHelper.requireNonBlank(body, "title");
        ValidationHelper.requireNonBlank(body, "projectId");

        taskManager.createTask(body, userId, organizationId)
                .onSuccess(task -> sendJson(ctx, 201, task.toJson()))
                .onFailure(ctx::fail);
    }

    private void updateTask(RoutingContext ctx) {
        String organizationId = ctx.get("organizationId");
        String taskId = ctx.pathParam("id");
        JsonObject body = ctx.body().asJsonObject();

        if (body == null) {
            ctx.fail(new AppException(ErrorCode.BAD_REQUEST, "Request body is required"));
            return;
        }

        taskManager.updateTask(taskId, body, organizationId)
                .onSuccess(task -> sendJson(ctx, 200, task.toJson()))
                .onFailure(ctx::fail);
    }

    private void deleteTask(RoutingContext ctx) {
        String organizationId = ctx.get("organizationId");
        String taskId = ctx.pathParam("id");

        taskManager.deleteTask(taskId, organizationId)
                .onSuccess(v -> sendJson(ctx, 204, null))
                .onFailure(ctx::fail);
    }

    private void updateStatus(RoutingContext ctx) {
        String organizationId = ctx.get("organizationId");
        String taskId = ctx.pathParam("id");
        JsonObject body = ctx.body().asJsonObject();

        if (body == null) {
            ctx.fail(new AppException(ErrorCode.BAD_REQUEST, "Request body is required"));
            return;
        }

        ValidationHelper.requireNonBlank(body, "status");

        taskManager.updateStatus(taskId, body.getString("status"), organizationId)
                .onSuccess(task -> sendJson(ctx, 200, task.toJson()))
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
