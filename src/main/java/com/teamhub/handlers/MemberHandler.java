package com.teamhub.handlers;

import com.teamhub.common.AppException;
import com.teamhub.common.ErrorCode;
import com.teamhub.managers.MemberManager;
import com.teamhub.utils.PaginationHelper;
import com.teamhub.utils.ValidationHelper;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class MemberHandler {

    private static final Logger logger = LoggerFactory.getLogger(MemberHandler.class);

    private final MemberManager memberManager;

    public MemberHandler(MemberManager memberManager) {
        this.memberManager = memberManager;
    }

    public void mount(Router router) {
        router.get("/members").handler(this::listMembers);
        router.get("/members/:id").handler(this::getMember);
        router.post("/members/invite").handler(this::inviteMember);
        router.post("/members/invite-quick").handler(this::quickInviteMember);
        router.put("/members/:id/role").handler(this::updateRole);
        router.delete("/members/:id").handler(this::removeMember);
    }

    private void listMembers(RoutingContext ctx) {
        String organizationId = ctx.get("organizationId");
        int page = PaginationHelper.getPage(ctx);
        int pageSize = PaginationHelper.getPageSize(ctx);
        int skip = PaginationHelper.calculateSkip(page, pageSize);

        memberManager.listMembers(organizationId, skip, pageSize).compose(members ->
                memberManager.countMembers(organizationId).map(total -> {
                    JsonArray data = new JsonArray();
                    members.forEach(m -> data.add(m.toJson()));
                    return new JsonObject()
                            .put("data", data)
                            .put("pagination", PaginationHelper.buildPaginationMeta(page, pageSize, total));
                })
        ).onSuccess(result -> sendJson(ctx, 200, result))
                .onFailure(ctx::fail);
    }

    private void getMember(RoutingContext ctx) {
        String organizationId = ctx.get("organizationId");
        String memberId = ctx.pathParam("id");

        memberManager.getMember(memberId, organizationId)
                .onSuccess(member -> sendJson(ctx, 200, member.toJson()))
                .onFailure(ctx::fail);
    }

    private void inviteMember(RoutingContext ctx) {
        String organizationId = ctx.get("organizationId");
        String userId = ctx.get("userId");
        JsonObject body = ctx.body().asJsonObject();

        if (body == null) {
            ctx.fail(new AppException(ErrorCode.BAD_REQUEST, "Request body is required"));
            return;
        }

        ValidationHelper.requireNonBlank(body, "email");
        ValidationHelper.validateEmail(body.getString("email"));

        memberManager.inviteMember(body, organizationId, userId)
                .onSuccess(member -> sendJson(ctx, 201, member.toJson()))
                .onFailure(ctx::fail);
    }

    /**
     * Simplified invite endpoint with inline validation for quick team setup flows.
     * Validates email and role locally before delegating to the member manager.
     */
    private void quickInviteMember(RoutingContext ctx) {
        String organizationId = ctx.get("organizationId");
        String userId = ctx.get("userId");
        JsonObject body = ctx.body().asJsonObject();

        if (body == null) {
            ctx.fail(new AppException(ErrorCode.BAD_REQUEST, "Request body is required"));
            return;
        }

        String email = body.getString("email");
        String role = body.getString("role");

        // Validate email format
        if (email == null || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            ctx.fail(new AppException(ErrorCode.VALIDATION_ERROR, "Invalid email format"));
            return;
        }

        // Validate role
        if (role == null || !List.of("ADMIN", "MEMBER", "VIEWER").contains(role)) {
            ctx.fail(new AppException(ErrorCode.VALIDATION_ERROR, "Invalid role"));
            return;
        }

        memberManager.inviteMember(body, organizationId, userId)
                .onSuccess(member -> sendJson(ctx, 201, member.toJson()))
                .onFailure(ctx::fail);
    }

    private void updateRole(RoutingContext ctx) {
        String organizationId = ctx.get("organizationId");
        String userId = ctx.get("userId");
        String memberId = ctx.pathParam("id");
        JsonObject body = ctx.body().asJsonObject();

        if (body == null) {
            ctx.fail(new AppException(ErrorCode.BAD_REQUEST, "Request body is required"));
            return;
        }

        ValidationHelper.requireNonBlank(body, "role");

        memberManager.updateRole(memberId, body.getString("role"), organizationId, userId)
                .onSuccess(member -> sendJson(ctx, 200, member.toJson()))
                .onFailure(ctx::fail);
    }

    private void removeMember(RoutingContext ctx) {
        String organizationId = ctx.get("organizationId");
        String userId = ctx.get("userId");
        String memberId = ctx.pathParam("id");

        memberManager.removeMember(memberId, organizationId, userId)
                .onSuccess(v -> sendJson(ctx, 204, null))
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
