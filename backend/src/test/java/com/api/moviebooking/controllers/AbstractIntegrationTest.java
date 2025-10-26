package com.api.moviebooking.controllers;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for integration tests using singleton Testcontainers.
 * Containers are started once and reused across all test classes for
 * performance.
 */
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    // Singleton containers - started once and reused across all test classes
    private static final PostgreSQLContainer<?> postgresContainer;
    private static final GenericContainer<?> redisContainer;

    static {
        // Initialize PostgreSQL container as singleton
        postgresContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
                .withReuse(true);
        postgresContainer.start();

        // Initialize Redis container as singleton
        redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379)
                .withReuse(true);
        redisContainer.start();
    }

    @ServiceConnection
    protected static PostgreSQLContainer<?> getPostgresContainer() {
        return postgresContainer;
    }

    protected static GenericContainer<?> getRedisContainer() {
        return redisContainer;
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL configuration
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);

        // Redis configuration
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", redisContainer::getFirstMappedPort);
    }
}
