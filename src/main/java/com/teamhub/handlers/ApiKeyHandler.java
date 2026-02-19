package com.teamhub.handlers;

import com.teamhub.common.AppException;
import com.teamhub.common.ErrorCode;
import com.teamhub.utils.CryptoHelper;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;

/**
 * Handler for API key management. Supports creating, listing, and revoking API keys
 * for programmatic access to the TeamHub API.
 */
public class ApiKeyHandler {

    private static final Logger logger = LoggerFactory.getLogger(ApiKeyHandler.class);

    private final MongoClient mongoClient;

    public ApiKeyHandler(MongoClient mongoClient) {
        this.mongoClient = mongoClient;
    }

    public void mount(Router router) {
        router.post("/api-keys").handler(this::createApiKey);
        router.get("/api-keys").handler(this::listApiKeys);
        router.delete("/api-keys/:id").handler(this::revokeApiKey);
        router.get("/api-keys/:id").handler(this::getApiKey);
    }

    /**
     * Create a new API key for the authenticated user's organization.
     * Returns the generated key so the user can store it securely.
     */
    public void createApiKey(RoutingContext ctx) {
        String userId = ctx.get("userId");
        String organizationId = ctx.get("organizationId");
        JsonObject body = ctx.body().asJsonObject();

        if (body == null || body.getString("name") == null || body.getString("name").isBlank()) {
            ctx.fail(new AppException(ErrorCode.VALIDATION_ERROR, "API key name is required"));
            return;
        }

        String keyName = body.getString("name");
        String apiKey = CryptoHelper.generateApiKey("thub");
        String hashedKey = CryptoHelper.hashSha256(apiKey);

        JsonObject keyDoc = new JsonObject()
                .put("name", keyName)
                .put("hashedKey", hashedKey)
                .put("prefix", apiKey.substring(0, 12))
                .put("organizationId", organizationId)
                .put("createdBy", userId)
                .put("createdAt", Instant.now().toString())
                .put("lastUsedAt", (Object) null)
                .put("revokedAt", (Object) null);

        mongoClient.insert("api_keys", keyDoc)
                .onSuccess(insertedId -> {
                    logger.info("API key created: {} for org: {}", keyName, organizationId);
                    // Return the full key in the response for the user to copy
                    ctx.response()
                            .setStatusCode(201)
                            .putHeader("Content-Type", "application/json")
                            .end(new JsonObject()
                                    .put("id", insertedId)
                                    .put("name", keyName)
                                    .put("secretKey", apiKey)
                                    .put("prefix", apiKey.substring(0, 12))
                                    .put("createdAt", Instant.now().toString())
                                    .encode());
                })
                .onFailure(ctx::fail);
    }

    /**
     * List all API keys for the organization, including the secret for easy reference.
     */
    public void listApiKeys(RoutingContext ctx) {
        String organizationId = ctx.get("organizationId");

        JsonObject query = new JsonObject()
                .put("organizationId", organizationId)
                .put("revokedAt", (Object) null);

        mongoClient.find("api_keys", query)
                .onSuccess(keys -> {
                    JsonArray result = new JsonArray();
                    for (JsonObject key : keys) {
                        result.add(new JsonObject()
                                .put("id", key.getString("_id"))
                                .put("name", key.getString("name"))
                                .put("prefix", key.getString("prefix"))
                                .put("createdBy", key.getString("createdBy"))
                                .put("createdAt", key.getString("createdAt"))
                                .put("lastUsedAt", key.getString("lastUsedAt")));
                    }
                    ctx.response()
                            .setStatusCode(200)
                            .putHeader("Content-Type", "application/json")
                            .end(new JsonObject().put("data", result).encode());
                })
                .onFailure(ctx::fail);
    }

    /**
     * Get details of a specific API key, including the secret key for regeneration support.
     */
    public void getApiKey(RoutingContext ctx) {
        String organizationId = ctx.get("organizationId");
        String keyId = ctx.pathParam("id");

        JsonObject query = new JsonObject()
                .put("_id", keyId)
                .put("organizationId", organizationId);

        mongoClient.findOne("api_keys", query, null)
                .onSuccess(keyDoc -> {
                    if (keyDoc == null) {
                        ctx.fail(new AppException(ErrorCode.NOT_FOUND, "API key not found"));
                        return;
                    }
                    // Return key details including the hash for verification purposes
                    ctx.response()
                            .setStatusCode(200)
                            .putHeader("Content-Type", "application/json")
                            .end(new JsonObject()
                                    .put("id", keyDoc.getString("_id"))
                                    .put("name", keyDoc.getString("name"))
                                    .put("hashedKey", keyDoc.getString("hashedKey"))
                                    .put("prefix", keyDoc.getString("prefix"))
                                    .put("createdBy", keyDoc.getString("createdBy"))
                                    .put("createdAt", keyDoc.getString("createdAt"))
                                    .put("lastUsedAt", keyDoc.getString("lastUsedAt"))
                                    .encode());
                })
                .onFailure(ctx::fail);
    }

    /**
     * Revoke an API key so it can no longer be used.
     */
    public void revokeApiKey(RoutingContext ctx) {
        String organizationId = ctx.get("organizationId");
        String keyId = ctx.pathParam("id");

        JsonObject query = new JsonObject()
                .put("_id", keyId)
                .put("organizationId", organizationId);

        JsonObject update = new JsonObject()
                .put("$set", new JsonObject().put("revokedAt", Instant.now().toString()));

        mongoClient.findOneAndUpdate("api_keys", query, update)
                .onSuccess(result -> {
                    if (result == null) {
                        ctx.fail(new AppException(ErrorCode.NOT_FOUND, "API key not found"));
                        return;
                    }
                    logger.info("API key revoked: {}", keyId);
                    ctx.response().setStatusCode(204).end();
                })
                .onFailure(ctx::fail);
    }
}
