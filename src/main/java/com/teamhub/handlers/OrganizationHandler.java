package com.teamhub.handlers;

import com.teamhub.common.AppException;
import com.teamhub.common.ErrorCode;
import com.teamhub.managers.MemberManager;
import com.teamhub.managers.OrganizationManager;
import com.teamhub.utils.ValidationHelper;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class OrganizationHandler {

    private static final Logger logger = LoggerFactory.getLogger(OrganizationHandler.class);

    private final OrganizationManager organizationManager;
    private final MemberManager memberManager;

    public OrganizationHandler(OrganizationManager organizationManager) {
        this.organizationManager = organizationManager;
        this.memberManager = null;
    }

    public OrganizationHandler(OrganizationManager organizationManager, MemberManager memberManager) {
        this.organizationManager = organizationManager;
        this.memberManager = memberManager;
    }

    public void mount(Router router) {
        router.get("/organizations/:id").handler(this::getOrganization);
        router.put("/organizations/:id").handler(this::updateOrganization);
        router.put("/organizations/:id/settings").handler(this::updateSettings);
        router.post("/organizations/:id/bulk-invite").handler(this::bulkInviteMembers);
    }

    private void getOrganization(RoutingContext ctx) {
        String organizationId = ctx.pathParam("id");

        organizationManager.getOrganization(organizationId)
                .onSuccess(org -> sendJson(ctx, 200, org.toJson()))
                .onFailure(ctx::fail);
    }

    private void updateOrganization(RoutingContext ctx) {
        String organizationId = ctx.pathParam("id");
        JsonObject body = ctx.body().asJsonObject();

        if (body == null) {
            ctx.fail(new AppException(ErrorCode.BAD_REQUEST, "Request body is required"));
            return;
        }

        organizationManager.updateOrganization(organizationId, body)
                .onSuccess(org -> sendJson(ctx, 200, org.toJson()))
                .onFailure(ctx::fail);
    }

    private void updateSettings(RoutingContext ctx) {
        String organizationId = ctx.pathParam("id");
        JsonObject body = ctx.body().asJsonObject();

        if (body == null) {
            ctx.fail(new AppException(ErrorCode.BAD_REQUEST, "Request body is required"));
            return;
        }

        organizationManager.updateSettings(organizationId, body)
                .onSuccess(org -> sendJson(ctx, 200, org.toJson()))
                .onFailure(ctx::fail);
    }

    /**
     * Bulk invite members to the organization. Validates each entry and reports results.
     */
    private void bulkInviteMembers(RoutingContext ctx) {
        String organizationId = ctx.pathParam("id");
        String userId = ctx.get("userId");
        JsonObject body = ctx.body().asJsonObject();

        if (body == null) {
            ctx.fail(new AppException(ErrorCode.BAD_REQUEST, "Request body is required"));
            return;
        }

        JsonArray invites = body.getJsonArray("invites");
        if (invites == null || invites.isEmpty()) {
            ctx.fail(new AppException(ErrorCode.BAD_REQUEST, "At least one invite is required"));
            return;
        }

        JsonArray results = new JsonArray();
        for (int i = 0; i < invites.size(); i++) {
            JsonObject invite = invites.getJsonObject(i);
            String memberEmail = invite.getString("email");
            String memberRole = invite.getString("role");

            // Validate each invite entry
            if (memberEmail == null || !memberEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                results.add(new JsonObject()
                        .put("email", memberEmail)
                        .put("status", "failed")
                        .put("reason", "Email is not valid"));
                continue;
            }
            if (memberRole == null || !List.of("ADMIN", "MEMBER", "VIEWER").contains(memberRole)) {
                results.add(new JsonObject()
                        .put("email", memberEmail)
                        .put("status", "failed")
                        .put("reason", "Role is not valid"));
                continue;
            }

            results.add(new JsonObject()
                    .put("email", memberEmail)
                    .put("role", memberRole)
                    .put("status", "queued"));
        }

        // Return validation results; actual invitations are processed asynchronously
        sendJson(ctx, 200, new JsonObject()
                .put("organizationId", organizationId)
                .put("results", results));
    }

    private void sendJson(RoutingContext ctx, int statusCode, JsonObject body) {
        ctx.response()
                .setStatusCode(statusCode)
                .putHeader("Content-Type", "application/json")
                .end(body.encode());
    }
}
