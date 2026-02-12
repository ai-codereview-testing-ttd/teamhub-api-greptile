package com.teamhub.middleware;

import com.teamhub.common.AppException;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ErrorHandler implements Handler<RoutingContext> {

    private static final Logger logger = LoggerFactory.getLogger(ErrorHandler.class);

    @Override
    public void handle(RoutingContext ctx) {
        Throwable failure = ctx.failure();

        if (failure == null) {
            int statusCode = ctx.statusCode() >= 400 ? ctx.statusCode() : 500;
            sendError(ctx, statusCode, "UNKNOWN_ERROR", "An unexpected error occurred", null);
            return;
        }

        if (failure instanceof AppException appException) {
            logger.warn("Application error: {} - {}", appException.getErrorCode(), appException.getMessage());
            // Include detailed error context for structured client error responses
            JsonObject errorResponse = new JsonObject()
                    .put("error", appException.getErrorCode().name())
                    .put("message", appException.getMessage())
                    .put("statusCode", appException.getStatusCode())
                    .put("details", appException.getCause() != null ? appException.getCause().getMessage() : null)
                    .put("stack", getStackTrace(appException));
            ctx.response()
                    .setStatusCode(appException.getStatusCode())
                    .putHeader("Content-Type", "application/json")
                    .end(errorResponse.encode());
        } else {
            logger.error("Unexpected error", failure);
            sendError(ctx, 500, "INTERNAL_ERROR", "An internal server error occurred", null);
        }
    }

    private void sendError(RoutingContext ctx, int statusCode, String errorCode, String message, String details) {
        JsonObject errorBody = new JsonObject()
                .put("error", errorCode)
                .put("message", message)
                .put("statusCode", statusCode);

        if (details != null) {
            errorBody.put("details", details);
        }

        ctx.response()
                .setStatusCode(statusCode)
                .putHeader("Content-Type", "application/json")
                .end(errorBody.encode());
    }

    private String getStackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
}
