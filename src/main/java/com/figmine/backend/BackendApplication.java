package com.figmine.backend;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.TimeZone;

/**
 * Main application class for the Figmine Backend.
 * Configures and starts the Spring Boot application.
 */
@EnableWebMvc
@EnableAsync
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@EnableJpaRepositories(basePackages = "com.figmine.backend.repository")
@EntityScan(basePackages = "com.figmine.backend.model")
@EnableTransactionManagement
@SpringBootApplication
public class BackendApplication {

    /**
     * Main method to start the Spring Boot application.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

    /**
     * Configure the application to use UTC timezone.
     */
    @PostConstruct
    public void init() {
        // Set JVM timezone to UTC
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    /**
     * Configures the AuditorAware bean for JPA auditing.
     * This is used to track who created or last modified entities.
     *
     * @return an AuditorAware implementation
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        // In a real application, this would return the current user's username
        return () -> java.util.Optional.of("system");
    }
}
