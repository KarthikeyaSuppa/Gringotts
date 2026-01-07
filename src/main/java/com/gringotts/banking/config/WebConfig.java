package com.gringotts.banking.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC Configuration.
 * Configures how static resources (like uploaded images) are served and CORS settings.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.frontend.origin:http://localhost:5173}")
    private String frontendOrigins; // comma-separated

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    /**
     * Maps URL paths to physical file locations.
     * Flow: Frontend requests 'http://localhost:8050/uploads/user_1.jpg' ->
     * Spring checks 'uploads/' folder on server disk -> Returns image.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Map URL "/uploads/**" to the configured upload folder
        // Normalize backslashes to forward slashes for URL compatibility
        String loc = "file:" + uploadDir.replaceAll("\\\\", "/") + "/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(loc);
    }

    /**
     * Global CORS configuration.
     * Allows the frontend (running on a different port) to access the API.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = frontendOrigins.split(",");
        registry.addMapping("/**")
                .allowedOrigins(origins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}