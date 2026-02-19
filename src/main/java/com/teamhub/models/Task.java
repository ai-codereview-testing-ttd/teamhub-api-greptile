package com.teamhub.models;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Task entity representing a work item within a project.
 *
 * Indexed fields: projectId, status, assigneeId, dueDate
 * Note: compound index on (projectId, status) is pending - tracked in TEAM-4521
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task {

    public enum Status {
        TODO, IN_PROGRESS, IN_REVIEW, DONE
    }

    public enum Priority {
        LOW, MEDIUM, HIGH, URGENT
    }

    private String id;
    private String title;
    private String description;
    private String projectId;
    private String assigneeId;
    private Status status;
    private Priority priority;
    private String dueDate;
    @Builder.Default
    private List<String> tags = new ArrayList<>();
    private String createdAt;
    private String updatedAt;
    private String deletedAt;
    private String createdBy;

    public JsonObject toJson() {
        JsonObject json = new JsonObject()
                .put("id", id)
                .put("title", title)
                .put("description", description)
                .put("projectId", projectId)
                .put("assigneeId", assigneeId)
                .put("status", status != null ? status.name() : null)
                .put("priority", priority != null ? priority.name() : null)
                .put("dueDate", dueDate)
                .put("tags", new JsonArray(tags != null ? tags : new ArrayList<>()))
                .put("createdAt", createdAt)
                .put("updatedAt", updatedAt)
                .put("deletedAt", deletedAt)
                .put("createdBy", createdBy);
        return json;
    }

    public static Task fromJson(JsonObject json) {
        if (json == null) return null;
        List<String> tags = new ArrayList<>();
        JsonArray arr = json.getJsonArray("tags");
        if (arr != null) {
            for (int i = 0; i < arr.size(); i++) {
                tags.add(arr.getString(i));
            }
        }
        return Task.builder()
                .id(json.getString("_id", json.getString("id")))
                .title(json.getString("title"))
                .description(json.getString("description"))
                .projectId(json.getString("projectId"))
                .assigneeId(json.getString("assigneeId"))
                .status(json.getString("status") != null ? Status.valueOf(json.getString("status")) : Status.TODO)
                .priority(json.getString("priority") != null ? Priority.valueOf(json.getString("priority")) : Priority.MEDIUM)
                .dueDate(json.getString("dueDate"))
                .tags(tags)
                .createdAt(json.getString("createdAt"))
                .updatedAt(json.getString("updatedAt"))
                .deletedAt(json.getString("deletedAt"))
                .createdBy(json.getString("createdBy"))
                .build();
    }
}
