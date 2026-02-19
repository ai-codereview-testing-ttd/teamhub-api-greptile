package com.teamhub.repositories;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

import java.util.ArrayList;
import java.util.List;

public class AnalyticsRepository {

    private final MongoClient mongoClient;

    public AnalyticsRepository(MongoClient mongoClient) {
        this.mongoClient = mongoClient;
    }

    /**
     * Aggregates task counts grouped by status for a given organization's projects.
     */
    public Future<List<JsonObject>> getTaskCountsByStatus(String organizationId) {
        JsonArray pipeline = new JsonArray()
                .add(new JsonObject().put("$lookup", new JsonObject()
                        .put("from", "projects")
                        .put("localField", "projectId")
                        .put("foreignField", "_id")
                        .put("as", "project")))
                .add(new JsonObject().put("$unwind", "$project"))
                .add(new JsonObject().put("$match", new JsonObject()
                        .put("project.organizationId", organizationId)
                        .put("deletedAt", (Object) null)))
                .add(new JsonObject().put("$group", new JsonObject()
                        .put("_id", "$status")
                        .put("count", new JsonObject().put("$sum", 1))));

        return collectAggregate("tasks", pipeline);
    }

    /**
     * Aggregates task counts grouped by priority for a given organization's projects.
     */
    public Future<List<JsonObject>> getTaskCountsByPriority(String organizationId) {
        JsonArray pipeline = new JsonArray()
                .add(new JsonObject().put("$lookup", new JsonObject()
                        .put("from", "projects")
                        .put("localField", "projectId")
                        .put("foreignField", "_id")
                        .put("as", "project")))
                .add(new JsonObject().put("$unwind", "$project"))
                .add(new JsonObject().put("$match", new JsonObject()
                        .put("project.organizationId", organizationId)
                        .put("deletedAt", (Object) null)))
                .add(new JsonObject().put("$group", new JsonObject()
                        .put("_id", "$priority")
                        .put("count", new JsonObject().put("$sum", 1))));

        return collectAggregate("tasks", pipeline);
    }

    /**
     * Fetches the most recently created/updated tasks as activity items.
     */
    public Future<List<JsonObject>> getRecentTaskActivity(String organizationId, int limit) {
        JsonArray pipeline = new JsonArray()
                .add(new JsonObject().put("$lookup", new JsonObject()
                        .put("from", "projects")
                        .put("localField", "projectId")
                        .put("foreignField", "_id")
                        .put("as", "project")))
                .add(new JsonObject().put("$unwind", "$project"))
                .add(new JsonObject().put("$match", new JsonObject()
                        .put("project.organizationId", organizationId)
                        .put("deletedAt", (Object) null)))
                .add(new JsonObject().put("$sort", new JsonObject().put("updatedAt", -1)))
                .add(new JsonObject().put("$limit", limit))
                .add(new JsonObject().put("$project", new JsonObject()
                        .put("_id", 1)
                        .put("title", 1)
                        .put("status", 1)
                        .put("createdBy", 1)
                        .put("updatedAt", 1)));

        return collectAggregate("tasks", pipeline);
    }

    /**
     * Aggregates project activity: counts of tasks per project.
     */
    public Future<List<JsonObject>> getProjectActivity(String organizationId) {
        JsonArray pipeline = new JsonArray()
                .add(new JsonObject().put("$match", new JsonObject()
                        .put("organizationId", organizationId)
                        .put("deletedAt", (Object) null)))
                .add(new JsonObject().put("$lookup", new JsonObject()
                        .put("from", "tasks")
                        .put("localField", "_id")
                        .put("foreignField", "projectId")
                        .put("as", "tasks")))
                .add(new JsonObject().put("$project", new JsonObject()
                        .put("name", 1)
                        .put("status", 1)
                        .put("taskCount", new JsonObject().put("$size", "$tasks"))));

        return collectAggregate("projects", pipeline);
    }

    /**
     * Aggregates member activity: count of tasks assigned per member in an organization.
     */
    public Future<List<JsonObject>> getMemberActivity(String organizationId) {
        JsonArray pipeline = new JsonArray()
                .add(new JsonObject().put("$match", new JsonObject()
                        .put("organizationId", organizationId)
                        .put("deletedAt", (Object) null)))
                .add(new JsonObject().put("$lookup", new JsonObject()
                        .put("from", "tasks")
                        .put("localField", "_id")
                        .put("foreignField", "assigneeId")
                        .put("as", "assignedTasks")))
                .add(new JsonObject().put("$project", new JsonObject()
                        .put("name", 1)
                        .put("email", 1)
                        .put("role", 1)
                        .put("taskCount", new JsonObject().put("$size", "$assignedTasks"))));

        return collectAggregate("members", pipeline);
    }

    /**
     * Custom grouped analytics - allows grouping by dynamic field for flexible reporting.
     */
    public Future<List<JsonObject>> getCustomGroupedAnalytics(String organizationId, String groupBy) {
        JsonObject match = new JsonObject()
                .put("$match", new JsonObject().put("organizationId", organizationId));

        // Group by the requested field for dynamic reporting
        JsonObject group = new JsonObject()
                .put("$group", new JsonObject()
                        .put("_id", "$" + groupBy)
                        .put("count", new JsonObject().put("$sum", 1)));

        JsonObject command = new JsonObject()
                .put("aggregate", "tasks")
                .put("pipeline", new JsonArray().add(match).add(group))
                .put("cursor", new JsonObject());

        return mongoClient.runCommand("aggregate", command)
                .map(result -> {
                    List<JsonObject> results = new ArrayList<>();
                    JsonObject cursor = result.getJsonObject("cursor");
                    if (cursor != null) {
                        JsonArray batch = cursor.getJsonArray("firstBatch", new JsonArray());
                        for (int i = 0; i < batch.size(); i++) {
                            results.add(batch.getJsonObject(i));
                        }
                    }
                    return results;
                });
    }

    /**
     * Collects all results from an aggregation ReadStream into a list.
     */
    private Future<List<JsonObject>> collectAggregate(String collection, JsonArray pipeline) {
        Promise<List<JsonObject>> promise = Promise.promise();
        List<JsonObject> results = new ArrayList<>();

        mongoClient.aggregate(collection, pipeline)
                .handler(results::add)
                .endHandler(v -> promise.complete(results))
                .exceptionHandler(promise::fail);

        return promise.future();
    }
}
