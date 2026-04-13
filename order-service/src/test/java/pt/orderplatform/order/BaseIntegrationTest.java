package pt.orderplatform.order;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

// =============================================================================
// BASE INTEGRATION TEST — classe base para todos os testes de integração
// =============================================================================
// PORQUÊ TESTCONTAINERS?
//   Os testes de integração precisam de uma base de dados REAL e de um Kafka REAL.
//   Mocks (H2, EmbeddedKafka) são úteis mas escondem problemas:
//     - H2 não suporta JSONB, tipos enum PostgreSQL, algumas funções SQL
//     - EmbeddedKafka tem comportamento diferente do Kafka real
//
//   Testcontainers resolve isto: sobe containers Docker reais durante os testes
//   e destrói-os no fim. É mais lento mas muito mais fiável.
//
// @Testcontainers → activa a integração Testcontainers com JUnit 5
// @Container      → o container é partilhado entre todos os testes da classe
//                   (static = sobe uma vez, não por cada teste)
//
// @SpringBootTest → sobe o contexto Spring completo (como se fosse a app a correr)
//   webEnvironment = RANDOM_PORT → usa uma porta aleatória (evita conflitos)
//
// @DynamicPropertySource → sobrescreve as propriedades do application.yml
//   com os valores dinâmicos dos containers (porta, URL, credenciais)
//   O Testcontainers atribui portas aleatórias — precisamos de dizer ao Spring
//   onde estão os serviços.
// =============================================================================
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseIntegrationTest {

    // -------------------------------------------------------------------------
    // POSTGRESQL CONTAINER
    // -------------------------------------------------------------------------
    // Sobe um PostgreSQL 16 real num container Docker.
    // O Flyway vai aplicar as migrações (V1__create_orders_schema.sql) neste container.
    // static → partilhado entre todos os testes (não sobe/desce por cada teste)
    // -------------------------------------------------------------------------
    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("orders_db_test")
                    .withUsername("test")
                    .withPassword("test");

    // -------------------------------------------------------------------------
    // KAFKA CONTAINER
    // -------------------------------------------------------------------------
    // Sobe um Kafka real. O Testcontainers usa a imagem do Confluent Platform.
    // -------------------------------------------------------------------------
    @Container
    static final KafkaContainer kafka =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    // -------------------------------------------------------------------------
    // SOBRESCREVER PROPRIEDADES — ligar o Spring aos containers
    // -------------------------------------------------------------------------
    // Após os containers arrancarem, o Testcontainers atribui portas aleatórias.
    // Aqui dizemos ao Spring: "usa estes URLs em vez dos do application.yml"
    // -------------------------------------------------------------------------
    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        // Base de dados — URL dinâmica do container PostgreSQL
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // Kafka — URL dinâmica do container Kafka
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);

        // Desactivar Keycloak nos testes — usamos mocks de autenticação
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                () -> "http://localhost:9999/realms/test");
    }
}
