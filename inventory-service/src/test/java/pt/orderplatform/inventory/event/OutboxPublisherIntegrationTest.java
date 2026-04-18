package pt.orderplatform.inventory.event;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import pt.orderplatform.inventory.BaseIntegrationTest;
import pt.orderplatform.inventory.domain.OutboxEvent;
import pt.orderplatform.inventory.repository.OutboxEventRepository;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class OutboxPublisherIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Test
    @DisplayName("publica evento pendente no tópico correcto e marca como published")
    void publishesPendingEventAndMarksAsPublished() {
        UUID orderId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        String payload = "{\"eventId\":\"" + eventId + "\",\"orderId\":\"" + orderId + "\"}";

        OutboxEvent event = OutboxEvent.of("Order", orderId, "InventoryReserved", payload);
        OutboxEvent saved = outboxEventRepository.save(event);

        // O scheduler corre a cada 5s — aguardamos até 15s para processar
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            OutboxEvent reloaded = outboxEventRepository.findById(saved.getId()).orElseThrow();
            assertThat(reloaded.isPublished()).isTrue();
            assertThat(reloaded.getPublishedAt()).isNotNull();
        });

        // Consumir o tópico e verificar que a mensagem chegou
        try (KafkaConsumer<String, String> consumer = newConsumer("inventory.reserved")) {
            List<ConsumerRecord<String, String>> records = pollAll(consumer, Duration.ofSeconds(10));

            assertThat(records)
                    .extracting(ConsumerRecord::key)
                    .contains(orderId.toString());

            assertThat(records)
                    .extracting(ConsumerRecord::value)
                    .anyMatch(v -> v.contains(eventId.toString()));
        }
    }

    @Test
    @DisplayName("eventType desconhecido vai para tópico fallback inventory.unknown")
    void unknownEventTypeGoesToFallbackTopic() {
        UUID orderId = UUID.randomUUID();
        String payload = "{\"orderId\":\"" + orderId + "\"}";
        OutboxEvent event = OutboxEvent.of("Order", orderId, "SomethingMadeUp", payload);
        OutboxEvent saved = outboxEventRepository.save(event);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(outboxEventRepository.findById(saved.getId()).orElseThrow().isPublished())
                        .isTrue()
        );

        try (KafkaConsumer<String, String> consumer = newConsumer("inventory.unknown")) {
            List<ConsumerRecord<String, String>> records = pollAll(consumer, Duration.ofSeconds(10));
            assertThat(records).anyMatch(r -> r.key().equals(orderId.toString()));
        }
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------
    private KafkaConsumer<String, String> newConsumer(String topic) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(List.of(topic));
        return consumer;
    }

    private List<ConsumerRecord<String, String>> pollAll(KafkaConsumer<String, String> consumer,
                                                         Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        java.util.ArrayList<ConsumerRecord<String, String>> all = new java.util.ArrayList<>();
        while (System.currentTimeMillis() < deadline) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
            records.forEach(all::add);
            if (!all.isEmpty()) {
                break;
            }
        }
        return all;
    }
}
