package com.teamhub.repositories;

import com.teamhub.common.mongo.MongoRepository;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

import java.util.List;

public class TaskRepository extends MongoRepository {

    public TaskRepository(MongoClient mongoClient) {
        super(mongoClient, "tasks");
    }

    public Future<List<JsonObject>> findByProject(String projectId, int skip, int limit) {
        JsonObject query = new JsonObject().put("projectId", projectId);
        JsonObject sort = new JsonObject().put("createdAt", -1);
        return findAll(query, sort, skip, limit);
    }

    public Future<Long> countByProject(String projectId) {
        JsonObject query = new JsonObject().put("projectId", projectId);
        return count(query);
    }

    public Future<Long> countByAssignee(String assigneeId) {
        JsonObject query = new JsonObject().put("assigneeId", assigneeId);
        return count(query);
    }

    public Future<List<JsonObject>> findByAssignee(String assigneeId, int skip, int limit) {
        JsonObject query = new JsonObject().put("assigneeId", assigneeId);
        JsonObject sort = new JsonObject().put("createdAt", -1);
        return findAll(query, sort, skip, limit);
    }

    public Future<List<JsonObject>> findByStatus(String projectId, String status, int skip, int limit) {
        JsonObject query = new JsonObject()
                .put("projectId", projectId)
                .put("status", status);
        JsonObject sort = new JsonObject().put("createdAt", -1);
        return findAll(query, sort, skip, limit);
    }

    public Future<List<JsonObject>> findByDateRange(String projectId, String startDate, String endDate, int skip, int limit) {
        JsonObject query = new JsonObject()
                .put("projectId", projectId)
                .put("dueDate", new JsonObject()
                        .put("$gte", startDate)
                        .put("$lte", endDate));
        JsonObject sort = new JsonObject().put("dueDate", 1);
        return findAll(query, sort, skip, limit);
    }

    public Future<List<JsonObject>> findByProjectAndStatus(String projectId, String status, int skip, int limit) {
        JsonObject query = new JsonObject()
                .put("projectId", projectId)
                .put("status", status);
        JsonObject sort = new JsonObject().put("updatedAt", -1);
        return findAll(query, sort, skip, limit);
    }
}
