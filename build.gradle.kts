// ============================================================
// ROOT BUILD — configuração partilhada por todos os subprojetos
// ============================================================
// Este ficheiro define versões e plugins uma só vez.
// Cada serviço herda estas definições no seu próprio build.gradle.kts.

plugins {
    // O `apply false` significa: declaro o plugin aqui mas não o aplico na raiz.
    // Cada subprojeto escolhe se quer aplicá-lo.
    id("org.springframework.boot") version "3.5.0" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    kotlin("jvm") version "2.1.0" apply false  // Kotlin DSL nos build scripts
}

// Configuração aplicada a TODOS os subprojetos
subprojects {
    apply(plugin = "io.spring.dependency-management")

    // Repositório onde o Gradle vai buscar as dependências (Maven Central)
    repositories {
        mavenCentral()
    }

    // Versões centralizadas — alterar aqui afeta todos os serviços
    the<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension>().apply {
        imports {
            // Spring Boot BOM — define versões de todas as libs Spring
            mavenBom("org.springframework.boot:spring-boot-dependencies:3.5.0")
        }
        dependencies {
            // Testcontainers — para testes de integração com DB/Kafka reais
            dependency("org.testcontainers:testcontainers-bom:1.20.4")
            // OpenTelemetry — traces distribuídos
            dependency("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom:2.10.0")
        }
    }

    // Configuração Java comum a todos os serviços
    plugins.withId("java") {
        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_21
            targetCompatibility = JavaVersion.VERSION_21
        }

        // Encoding UTF-8 para todos os ficheiros Java
        tasks.withType<JavaCompile>().configureEach {
            options.encoding = "UTF-8"
        }

        // Testes sempre com output detalhado
        tasks.withType<Test>().configureEach {
            useJUnitPlatform() // JUnit 5
            testLogging {
                events("passed", "skipped", "failed")
            }
        }
    }
}
