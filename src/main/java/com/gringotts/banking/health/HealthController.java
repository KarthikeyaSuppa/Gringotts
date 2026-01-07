package com.gringotts.banking.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Health Check API.
 * Used to verify the application is running without authentication.
 */
@RestController
public class HealthController {

    /**
     * Simple heartbeat endpoint.
     * Endpoint: GET /health
     * Returns: "Gringotts Bank is Up and Running!"
     */
    @GetMapping("/health")
    public String healthCheck() {
        return "Gringotts Bank is Up and Running!";
    }
}