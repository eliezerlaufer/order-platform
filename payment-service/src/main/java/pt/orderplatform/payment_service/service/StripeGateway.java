package pt.orderplatform.payment_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Random;
import java.util.UUID;

// =============================================================================
// STRIPE GATEWAY — mock do gateway de pagamento
// =============================================================================
// Em produção, este serviço faria uma chamada HTTP à API real da Stripe.
// Para fins de desenvolvimento/estudo, simula aprovação aleatória:
//   - 80% de sucesso (aprovado)
//   - 20% de falha (recusado)
//
// A injecção de aleatoriedade pode ser controlada num futuro via Spring Profile
// (profile=test → sempre aprovado; profile=default → aleatório).
// =============================================================================
@Slf4j
@Service
public class StripeGateway {

    private static final double SUCCESS_RATE = 0.80;
    private final Random random = new Random();

    /**
     * Processa um pagamento. Retorna true se aprovado, false se recusado.
     *
     * @param orderId  ID do pedido (para logging)
     * @param amount   Valor a debitar
     */
    public boolean charge(UUID orderId, BigDecimal amount) {
        boolean approved = random.nextDouble() < SUCCESS_RATE;
        if (approved) {
            log.info("Stripe mock: APPROVED payment for order {} amount={}", orderId, amount);
        } else {
            log.warn("Stripe mock: DECLINED payment for order {} amount={}", orderId, amount);
        }
        return approved;
    }
}
