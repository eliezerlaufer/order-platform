package pt.orderplatform.notification_service.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Slf4j
@Configuration
public class KafkaDlqConfig {

    @Bean
    public CommonErrorHandler kafkaErrorHandler(KafkaOperations<String, String> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
        FixedBackOff backOff = new FixedBackOff(1000L, 3L);
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);
        handler.addNotRetryableExceptions(IllegalStateException.class, IllegalArgumentException.class);
        handler.setRetryListeners((record, ex, deliveryAttempt) ->
                log.warn("DLQ retry attempt {} for topic={} key={} error={}",
                        deliveryAttempt, record.topic(), record.key(), ex.getMessage()));
        return handler;
    }
}
