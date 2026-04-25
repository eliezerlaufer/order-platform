package pt.orderplatform.order.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

// =============================================================================
// KAFKA DLQ CONFIGURATION — Dead Letter Queue para mensagens não processáveis
// =============================================================================
// Quando um listener lança uma excepção (infra: JSON inválido, DB em baixo),
// o DefaultErrorHandler tenta a mensagem até 3 vezes (intervalo 1s).
// Após esgotar as tentativas, a mensagem é publicada no tópico <original>.DLT
// pelo DeadLetterPublishingRecoverer.
//
// Spring Boot auto-configura este bean na ConcurrentKafkaListenerContainerFactory
// quando encontra um @Bean do tipo CommonErrorHandler no contexto.
//
// Excepções de negócio (IllegalStateException) são marcadas como não-retentáveis
// porque nunca serão resolvidas por retry (ex: pedido em estado terminal).
// =============================================================================
@Slf4j
@Configuration
public class KafkaDlqConfig {

    @Bean
    public CommonErrorHandler kafkaErrorHandler(KafkaOperations<String, String> kafkaTemplate) {
        // DeadLetterPublishingRecoverer envia para <topic>.DLT após esgotar retries
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);

        // 3 tentativas com 1 segundo de intervalo entre cada uma
        FixedBackOff backOff = new FixedBackOff(1000L, 3L);

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);

        // Excepções de negócio — não fazem sentido ser re-tentadas
        handler.addNotRetryableExceptions(IllegalStateException.class);

        handler.setRetryListeners((record, ex, deliveryAttempt) ->
                log.warn("DLQ retry attempt {} for topic={} key={} error={}",
                        deliveryAttempt, record.topic(), record.key(), ex.getMessage()));

        return handler;
    }
}
