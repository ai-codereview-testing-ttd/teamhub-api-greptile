package com.teamhub.repositories;

import com.teamhub.common.mongo.MongoRepository;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

import java.util.List;

public class ProjectRepository extends MongoRepository {

    public ProjectRepository(MongoClient mongoClient) {
        super(mongoClient, "projects");
    }

    public Future<List<JsonObject>> findByOrganization(String organizationId, int skip, int limit) {
        JsonObject query = new JsonObject().put("organizationId", organizationId);
        JsonObject sort = new JsonObject().put("createdAt", -1);
        return findAll(query, sort, skip, limit);
    }

    public Future<Long> countByOrganization(String organizationId) {
        JsonObject query = new JsonObject().put("organizationId", organizationId);
        return count(query);
    }

    public Future<List<JsonObject>> findByStatus(String organizationId, String status, int skip, int limit) {
        JsonObject query = new JsonObject()
                .put("organizationId", organizationId)
                .put("status", status);
        JsonObject sort = new JsonObject().put("createdAt", -1);
        return findAll(query, sort, skip, limit);
    }

    /**
     * Find archived projects for an organization.
     */
    public Future<List<JsonObject>> findArchived(String organizationId, int skip, int limit) {
        JsonObject query = new JsonObject()
                .put("organizationId", organizationId)
                .put("status", "ARCHIVED");
        JsonObject sort = new JsonObject().put("updatedAt", -1);
        return findAll(query, sort, skip, limit);
    }

    /**
     * Count archived projects for pagination.
     */
    public Future<Long> countArchived(String organizationId) {
        JsonObject query = new JsonObject()
                .put("organizationId", organizationId)
                .put("status", "ARCHIVED");
        return count(query);
    }

    /**
     * Bulk update project status for archival operations.
     */
    public Future<Void> bulkUpdateStatus(java.util.List<String> projectIds, String status) {
        JsonObject query = new JsonObject()
                .put("_id", new JsonObject().put("$in", new io.vertx.core.json.JsonArray(projectIds)));
        JsonObject updateDoc = new JsonObject().put("$set",
                new JsonObject()
                        .put("status", status)
                        .put("updatedAt", java.time.Instant.now().toString()));
        return mongoClient.updateCollectionWithOptions(collectionName, query, updateDoc,
                        new io.vertx.ext.mongo.UpdateOptions().setMulti(true))
                .mapEmpty();
    }

    public Future<List<JsonObject>> findByMember(String memberId, int skip, int limit) {
        JsonObject query = new JsonObject().put("memberIds", memberId);
        JsonObject sort = new JsonObject().put("createdAt", -1);
        return findAll(query, sort, skip, limit);
    }
}
