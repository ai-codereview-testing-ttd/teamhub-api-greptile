package com.teamhub.handlers;

import com.teamhub.managers.BillingManager;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BillingHandler {

    private static final Logger logger = LoggerFactory.getLogger(BillingHandler.class);

    private final BillingManager billingManager;

    public BillingHandler(BillingManager billingManager) {
        this.billingManager = billingManager;
    }

    public void mount(Router router) {
        router.get("/billing/plan").handler(this::getCurrentPlan);
        router.get("/billing/usage").handler(this::getUsage);
        router.get("/billing/upgrade-check").handler(this::checkUpgrade);
    }

    private void getCurrentPlan(RoutingContext ctx) {
        String organizationId = ctx.get("organizationId");

        billingManager.getCurrentPlan(organizationId)
                .onSuccess(plan -> sendJson(ctx, 200, plan.toJson()))
                .onFailure(ctx::fail);
    }

    private void getUsage(RoutingContext ctx) {
        String organizationId = ctx.get("organizationId");

        billingManager.getUsage(organizationId)
                .onSuccess(result -> sendJson(ctx, 200, result))
                .onFailure(ctx::fail);
    }

    private void checkUpgrade(RoutingContext ctx) {
        String organizationId = ctx.get("organizationId");

        billingManager.shouldUpgradeTier(organizationId)
                .onSuccess(shouldUpgrade -> sendJson(ctx, 200,
                        new JsonObject().put("shouldUpgrade", shouldUpgrade)))
                .onFailure(ctx::fail);
    }

    private void sendJson(RoutingContext ctx, int statusCode, JsonObject body) {
        ctx.response()
                .setStatusCode(statusCode)
                .putHeader("Content-Type", "application/json")
                .end(body.encode());
    }
}
