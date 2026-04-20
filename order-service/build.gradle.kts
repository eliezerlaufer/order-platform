// ============================================================
// ORDER-SERVICE — responsável pelo ciclo de vida dos pedidos
// ============================================================
plugins {
    java
    id("org.springframework.boot") version "3.5.0"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "pt.orderplatform"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        // Java 21 — LTS atual, suporte a Virtual Threads (Project Loom)
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // --- WEB ---
    // Spring MVC: expõe REST endpoints (@RestController, @GetMapping, etc.)
    implementation("org.springframework.boot:spring-boot-starter-web")

    // --- VALIDAÇÃO ---
    // Anotações @Valid, @NotNull, @Size nos DTOs de entrada
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // --- BASE DE DADOS ---
    // Spring Data JPA: abstração sobre Hibernate (@Entity, @Repository)
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    // Driver JDBC para PostgreSQL
    runtimeOnly("org.postgresql:postgresql")
    // Flyway: migrações de schema SQL versionadas (V1__create_orders.sql, etc.)
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // --- KAFKA ---
    // Spring Kafka: @KafkaListener para consumir, KafkaTemplate para produzir eventos
    implementation("org.springframework.kafka:spring-kafka")

    // --- SEGURANÇA ---
    // Spring Security: filtros de autenticação/autorização
    implementation("org.springframework.boot:spring-boot-starter-security")
    // OAuth2 Resource Server: valida JWT tokens emitidos pelo Keycloak
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

    // --- OBSERVABILIDADE ---
    // Actuator: /actuator/health (K8s liveness/readiness), /actuator/metrics
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    // Micrometer Tracing com bridge OpenTelemetry: traces distribuídos entre serviços
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.opentelemetry.instrumentation:opentelemetry-spring-boot-starter:2.26.1")

    // --- DOCUMENTAÇÃO ---
    // SpringDoc OpenAPI: gera Swagger UI automaticamente a partir das anotações
    // Aceder em: http://localhost:8081/swagger-ui.html
    // JSON da spec: http://localhost:8081/v3/api-docs
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3")

    // --- PRODUTIVIDADE ---
    // Lombok: @Data, @Builder, @RequiredArgsConstructor — elimina getters/setters boilerplate
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // ============================================================
    // DEPENDÊNCIAS DE TESTE (não entram no JAR de produção)
    // ============================================================
    // Spring Boot Test: @SpringBootTest, MockMvc, @DataJpaTest
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    // Spring Security Test: @WithMockUser para testes de endpoints protegidos
    testImplementation("org.springframework.security:spring-security-test")
    // Spring Kafka Test: EmbeddedKafkaBroker — Kafka em memória para testes unitários
    testImplementation("org.springframework.kafka:spring-kafka-test")
    // Testcontainers: sobe um PostgreSQL REAL num container Docker durante os testes
    // Garante que os testes testam contra a DB real, não mocks
    testImplementation("org.testcontainers:testcontainers:2.0.4")
    testImplementation("org.testcontainers:junit-jupiter:1.21.4")
    testImplementation("org.testcontainers:postgresql:1.21.4")
    testImplementation("org.testcontainers:kafka:1.21.4")
    // Lombok nos testes também
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform() // JUnit 5
    testLogging {
        events("passed", "skipped", "failed")
    }
}
