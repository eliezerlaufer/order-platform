package pt.orderplatform.inventory;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

// =============================================================================
// BASE INTEGRATION TEST — PostgreSQL + Kafka reais via Testcontainers
// =============================================================================
// Mesmo padrão do order-service.
// Containers iniciados via static {} — reutilizados por todas as classes de teste.
// =============================================================================
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseIntegrationTest {

    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("inventory_db_test")
                    .withUsername("test")
                    .withPassword("test");

    static final KafkaContainer kafka =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"))
                    .withStartupTimeout(Duration.ofMinutes(3));

    static {
        postgres.start();
        kafka.start();
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);

        // Desactiva autenticação JWT real nos testes
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                () -> "http://localhost:9999/realms/test");

        // Desactivar OpenTelemetry nos testes — sem collector local
        registry.add("otel.sdk.disabled", () -> "true");
    }
}
