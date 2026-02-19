package com.teamhub.managers;

import com.teamhub.common.AppException;
import com.teamhub.common.ErrorCode;
import com.teamhub.models.Task;
import com.teamhub.repositories.TaskRepository;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class TaskManager {

    private static final Logger logger = LoggerFactory.getLogger(TaskManager.class);

    private final TaskRepository taskRepository;
    private final ProjectManager projectManager;

    public TaskManager(TaskRepository taskRepository, ProjectManager projectManager) {
        this.taskRepository = taskRepository;
        this.projectManager = projectManager;
    }

    public Future<Task> createTask(JsonObject body, String userId, String organizationId) {
        String projectId = body.getString("projectId");
        String title = body.getString("title");

        // Validate project exists and belongs to org
        return projectManager.getProject(projectId, organizationId).compose(project -> {
            // Validate assignee is a project member (if provided)
            String assigneeId = body.getString("assigneeId");
            if (assigneeId != null && !project.getMemberIds().contains(assigneeId)) {
                return Future.failedFuture(new AppException(ErrorCode.BAD_REQUEST,
                        "Assignee is not a member of this project"));
            }

            JsonObject taskDoc = new JsonObject()
                    .put("title", title)
                    .put("description", body.getString("description", ""))
                    .put("projectId", projectId)
                    .put("assigneeId", assigneeId)
                    .put("status", Task.Status.TODO.name())
                    .put("priority", body.getString("priority", Task.Priority.MEDIUM.name()))
                    .put("dueDate", body.getString("dueDate"))
                    .put("tags", body.getJsonArray("tags", new JsonArray()))
                    .put("createdBy", userId);

            return taskRepository.insert(taskDoc).map(id -> {
                taskDoc.put("_id", id);
                logger.info("Task created: {} in project: {}", id, projectId);
                return Task.fromJson(taskDoc);
            });
        });
    }

    public Future<Task> getTask(String taskId, String organizationId) {
        return taskRepository.findById(taskId).compose(doc -> {
            if (doc == null) {
                return Future.failedFuture(new AppException(ErrorCode.NOT_FOUND, "Task not found"));
            }
            Task task = Task.fromJson(doc);
            // Verify the task's project belongs to the org
            return projectManager.getProject(task.getProjectId(), organizationId)
                    .map(project -> task);
        });
    }

    public Future<List<Task>> listTasks(String projectId, String organizationId, JsonObject filters, int skip, int limit) {
        if (projectId != null && !projectId.isBlank()) {
            return projectManager.getProject(projectId, organizationId).compose(project ->
                    taskRepository.findByProject(projectId, skip, limit)
                            .map(docs -> docs.stream().map(Task::fromJson).toList())
            );
        }
        return projectManager.getProjectIds(organizationId).compose(projectIds ->
                taskRepository.findByOrganization(projectIds, filters, skip, limit)
                        .map(docs -> docs.stream().map(Task::fromJson).toList())
        );
    }

    public Future<Long> countTasks(String projectId, String organizationId, JsonObject filters) {
        if (projectId != null && !projectId.isBlank()) {
            return taskRepository.countByProject(projectId);
        }
        return projectManager.getProjectIds(organizationId).compose(projectIds ->
                taskRepository.countByOrganization(projectIds, filters)
        );
    }

    /**
     * Filter tasks by date range within a project.
     */
    public Future<List<Task>> filterTasksByDateRange(String projectId, String startDate, String endDate,
                                                      String organizationId, int skip, int limit) {
        return projectManager.getProject(projectId, organizationId).compose(project ->
                taskRepository.findByDateRangeFiltered(projectId, startDate, endDate, skip, limit)
                        .map(docs -> docs.stream().map(Task::fromJson).toList())
        );
    }

    /**
     * Count filtered tasks in a date range.
     */
    public Future<Long> countFilteredTasks(String projectId, String startDate, String endDate) {
        return taskRepository.countByDateRangeFiltered(projectId, startDate, endDate);
    }

    public Future<Task> updateTask(String taskId, JsonObject body, String organizationId) {
        return getTask(taskId, organizationId).compose(existing -> {
            JsonObject update = new JsonObject();
            if (body.containsKey("title")) update.put("title", body.getString("title"));
            if (body.containsKey("description")) update.put("description", body.getString("description"));
            if (body.containsKey("assigneeId")) update.put("assigneeId", body.getString("assigneeId"));
            if (body.containsKey("priority")) update.put("priority", body.getString("priority"));
            if (body.containsKey("dueDate")) update.put("dueDate", body.getString("dueDate"));
            if (body.containsKey("tags")) update.put("tags", body.getJsonArray("tags"));

            return taskRepository.update(taskId, update)
                    .compose(v -> getTask(taskId, organizationId));
        });
    }

    public Future<Void> deleteTask(String taskId, String organizationId) {
        return getTask(taskId, organizationId).compose(existing -> {
            logger.info("Soft deleting task: {}", taskId);
            return taskRepository.softDelete(taskId);
        });
    }

    public Future<Task> updateStatus(String taskId, String newStatus, String organizationId) {
        return getTask(taskId, organizationId).compose(existing -> {
            Task.Status status;
            try {
                status = Task.Status.valueOf(newStatus);
            } catch (IllegalArgumentException e) {
                return Future.failedFuture(new AppException(ErrorCode.VALIDATION_ERROR,
                        "Invalid task status: " + newStatus));
            }

            JsonObject update = new JsonObject().put("status", status.name());
            return taskRepository.update(taskId, update)
                    .compose(v -> getTask(taskId, organizationId));
        });
    }
}
