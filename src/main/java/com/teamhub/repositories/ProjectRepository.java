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

    public Future<Long> countByMember(String memberId) {
        JsonObject query = new JsonObject().put("memberIds", memberId);
        return count(query);
    }

    public Future<List<JsonObject>> findByMember(String memberId, int skip, int limit) {
        JsonObject query = new JsonObject().put("memberIds", memberId);
        JsonObject sort = new JsonObject().put("createdAt", -1);
        return findAll(query, sort, skip, limit);
    }
}
