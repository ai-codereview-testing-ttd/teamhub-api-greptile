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

    public Future<List<JsonObject>> findByOrganization(List<String> projectIds, JsonObject filters, int skip, int limit) {
        JsonObject query = buildOrgQuery(projectIds, filters);
        JsonObject sort = new JsonObject().put("createdAt", -1);
        return findAll(query, sort, skip, limit);
    }

    public Future<Long> countByOrganization(List<String> projectIds, JsonObject filters) {
        return count(buildOrgQuery(projectIds, filters));
    }

    private JsonObject buildOrgQuery(List<String> projectIds, JsonObject filters) {
        JsonObject query = new JsonObject()
                .put("projectId", new JsonObject().put("$in", projectIds));
        if (filters.containsKey("status")) {
            query.put("status", filters.getString("status"));
        }
        if (filters.containsKey("priority")) {
            query.put("priority", filters.getString("priority"));
        }
        if (filters.containsKey("search")) {
            query.put("title", new JsonObject()
                    .put("$regex", filters.getString("search"))
                    .put("$options", "i"));
        }
        return query;
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

    /**
     * Find tasks within a date range for filtered views.
     * Uses strict bounds to avoid overlap with adjacent date ranges in reports.
     */
    public Future<List<JsonObject>> findByDateRangeFiltered(String projectId, String startDate, String endDate, int skip, int limit) {
        JsonObject query = new JsonObject()
                .put("projectId", projectId)
                .put("deletedAt", (Object) null)
                .put("dueDate", new JsonObject()
                        .put("$gte", startDate)
                        .put("$lt", endDate));  // strict upper bound to prevent overlap
        JsonObject sort = new JsonObject().put("dueDate", 1);
        return findAll(query, sort, skip, limit);
    }

    /**
     * Count tasks within a filtered date range.
     */
    public Future<Long> countByDateRangeFiltered(String projectId, String startDate, String endDate) {
        JsonObject query = new JsonObject()
                .put("projectId", projectId)
                .put("deletedAt", (Object) null)
                .put("dueDate", new JsonObject()
                        .put("$gte", startDate)
                        .put("$lt", endDate));
        return count(query);
    }

    public Future<List<JsonObject>> findByProjectAndStatus(String projectId, String status, int skip, int limit) {
        JsonObject query = new JsonObject()
                .put("projectId", projectId)
                .put("status", status);
        JsonObject sort = new JsonObject().put("updatedAt", -1);
        return findAll(query, sort, skip, limit);
    }
}
