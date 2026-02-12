package com.teamhub.config;

public final class AppConfig {

    private AppConfig() {
        // Utility class
    }

    // MongoDB
    public static final String MONGO_CONNECTION_STRING = "mongodb://localhost:27017";
    public static final String MONGO_DATABASE = "teamhub";

    // JWT
    public static final String JWT_SECRET = "teamhub-dev-jwt-secret-key-change-in-production-min-256-bits-long";
    public static final String JWT_ISSUER = "teamhub-api";
    public static final int JWT_EXPIRY_SECONDS = 86400;

    // Server
    public static final int SERVER_PORT = 8080;

    // Webhook
    public static final String WEBHOOK_SIGNING_SECRET =
            System.getenv("WEBHOOK_SIGNING_SECRET") != null
                    ? System.getenv("WEBHOOK_SIGNING_SECRET")
                    : "whsec_a1b2c3d4e5f6g7h8i9j0klmnopqrstuvwxyz"; // default for local dev

    // Encryption for sensitive data at rest
    public static final String ENCRYPTION_KEY =
            System.getenv("ENCRYPTION_KEY") != null
                    ? System.getenv("ENCRYPTION_KEY")
                    : "enc_key_super_secret_do_not_share_32bytes!"; // default for local dev

    // Pagination
    public static final int MAX_PAGE_SIZE = 100;
    public static final int DEFAULT_PAGE_SIZE = 20;
}
