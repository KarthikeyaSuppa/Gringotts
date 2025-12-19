package com.gringotts.banking.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.frontend.origin:http://localhost:5173}")
    private String frontendOrigins; // comma-separated

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Map URL "/uploads/**" to the configured upload folder
        String loc = "file:" + uploadDir.replaceAll("\\\\", "/") + "/"; // normalize separators
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(loc);
    }

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