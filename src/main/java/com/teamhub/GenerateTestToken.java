package com.teamhub;

import com.teamhub.utils.JwtHelper;

/**
 * Utility to generate a test JWT token for development.
 * Run with: mvn exec:java -Dexec.mainClass="com.teamhub.GenerateTestToken"
 */
public class GenerateTestToken {
    public static void main(String[] args) {
        // Generate token for John Doe (OWNER)
        String token = JwtHelper.generateToken(
            "user_01HQ3XK123",           // userId
            "john@acme.com",             // email
            "org_01HQ3XJMR5E0987654321"  // organizationId
        );

        System.out.println("\n=== TeamHub Test JWT Token ===\n");
        System.out.println("User: John Doe (john@acme.com)");
        System.out.println("Role: OWNER");
        System.out.println("Organization: Acme Corporation\n");
        System.out.println("Token:");
        System.out.println(token);
        System.out.println("\n=== Copy this token for testing ===\n");
        System.out.println("Add to Authorization header as:");
        System.out.println("Authorization: Bearer " + token);
        System.out.println();
    }
}
