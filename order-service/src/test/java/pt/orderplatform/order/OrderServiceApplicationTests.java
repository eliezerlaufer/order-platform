package pt.orderplatform.order;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// =============================================================================
// CONTEXT LOAD TEST
// =============================================================================
// Verifica que o contexto Spring arranca sem erros.
// Estende BaseIntegrationTest para usar Testcontainers (PostgreSQL + Kafka reais).
// Se este teste falhar, significa que há um bean mal configurado ou uma
// dependência em falta — é o primeiro teste a correr e o mais básico.
// =============================================================================
class OrderServiceApplicationTests extends BaseIntegrationTest {

    @Test
    @DisplayName("contexto Spring deve arrancar com sucesso")
    void contextLoads() {
        // Se chegar aqui sem lançar excepção, o contexto carregou correctamente
    }
}
