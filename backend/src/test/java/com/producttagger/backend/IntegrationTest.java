package com.producttagger.backend;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for full-context tests. The containers are singletons started
 * once per JVM and shared by every subclass, so the Spring context (and the
 * expensive Flyway run) is reused across test classes.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class IntegrationTest {

    // Same image as production: the plain postgres image lacks the pgvector
    // extension that migration V1 installs
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    static final GenericContainer<?> RABBIT = new GenericContainer<>("rabbitmq:3.13-alpine")
            .withExposedPorts(5672);

    static {
        POSTGRES.start();
        RABBIT.start();
    }

    @DynamicPropertySource
    static void rabbitProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", RABBIT::getHost);
        registry.add("spring.rabbitmq.port", RABBIT::getFirstMappedPort);
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");
    }
}
