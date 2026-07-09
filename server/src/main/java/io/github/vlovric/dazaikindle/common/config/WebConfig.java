package io.github.vlovric.dazaikindle.common.config;

import java.io.IOException;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * All endpoints are public per the API spec, and the React dev server runs on
 * a different origin/port than Spring Boot, so CORS must be allowed for it.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:5173")
            .allowedMethods("GET", "POST", "PUT", "DELETE");
    }

    /**
     * Serves the bundled React SPA (see server/pom.xml's frontend build) and
     * falls back to index.html for any path that isn't a real static file, so
     * react-router-dom's client-side routes (e.g. "/library") work on a hard
     * refresh instead of 404ing out of the static resource handler - which
     * GlobalExceptionHandler's catch-all would otherwise turn into a 500.
     * "/api/**" is unaffected: those requests are matched by @RestController
     * mappings before ever reaching this generic resource handler.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
            .addResourceLocations("classpath:/static/")
            .resourceChain(true)
            .addResolver(new PathResourceResolver() {
                @Override
                protected Resource getResource(String resourcePath, Resource location) throws IOException {
                    Resource requested = location.createRelative(resourcePath);
                    return requested.exists() && requested.isReadable()
                        ? requested
                        : new ClassPathResource("/static/index.html");
                }
            });
    }
}
