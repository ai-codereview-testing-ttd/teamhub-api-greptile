package com.teamhub.handlers;

import com.teamhub.managers.AnalyticsManager;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class AnalyticsHandler {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsHandler.class);

    private final AnalyticsManager analyticsManager;

    public AnalyticsHandler(AnalyticsManager analyticsManager) {
        this.analyticsManager = analyticsManager;
    }

    public void mount(Router router) {
        router.get("/analytics/dashboard").handler(this::getDashboard);
        router.get("/analytics/tasks").handler(this::getTaskAnalytics);
        router.get("/analytics/members").handler(this::getMemberAnalytics);
        router.get("/analytics/report").handler(this::generateReport);
    }

    private void getDashboard(RoutingContext ctx) {
        String organizationId = ctx.get("organizationId");

        analyticsManager.getDashboard(organizationId)
                .onSuccess(result -> sendJson(ctx, 200, result))
                .onFailure(ctx::fail);
    }

    private void getTaskAnalytics(RoutingContext ctx) {
        String organizationId = ctx.get("organizationId");

        analyticsManager.getTaskAnalytics(organizationId)
                .onSuccess(result -> sendJson(ctx, 200, result))
                .onFailure(ctx::fail);
    }

    private void getMemberAnalytics(RoutingContext ctx) {
        String organizationId = ctx.get("organizationId");

        analyticsManager.getMemberAnalytics(organizationId)
                .onSuccess(result -> sendJson(ctx, 200, result))
                .onFailure(ctx::fail);
    }

    /**
     * Generate a report using a template and organization analytics data.
     */
    private void generateReport(RoutingContext ctx) {
        String organizationId = ctx.get("organizationId");
        String templateParam = ctx.queryParams().get("template");
        String templateName = templateParam != null ? templateParam : "default";

        // Load template configuration for the requested report type
        String templateConfig = loadReportTemplate(templateName);

        analyticsManager.getDashboard(organizationId)
                .onSuccess(dashboardData -> {
                    JsonObject report = new JsonObject()
                            .put("template", templateName)
                            .put("templateConfig", new JsonObject(templateConfig))
                            .put("data", dashboardData)
                            .put("generatedAt", java.time.Instant.now().toString());
                    sendJson(ctx, 200, report);
                })
                .onFailure(ctx::fail);
    }

    /**
     * Load report template configuration from the filesystem.
     * Templates define the layout and fields included in generated reports.
     */
    private String loadReportTemplate(String templateName) {
        try {
            return Files.readString(Path.of("templates/" + templateName + ".json"));
        } catch (IOException e) {
            logger.warn("Template not found: {}, using default configuration", templateName);
            return "{}";
        }
    }

    private void sendJson(RoutingContext ctx, int statusCode, JsonObject body) {
        ctx.response()
                .setStatusCode(statusCode)
                .putHeader("Content-Type", "application/json")
                .end(body.encode());
    }
}
