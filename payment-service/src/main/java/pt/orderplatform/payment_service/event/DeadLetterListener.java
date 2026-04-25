package pt.orderplatform.payment_service.event;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class DeadLetterListener {

    @KafkaListener(
            topics = {"inventory.reserved.DLT"},
            groupId = "payment-service-dlt"
    )
    public void onDeadLetter(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String originalTopic = header(record, "kafka_dlt-original-topic");
        String exceptionClass = header(record, "kafka_dlt-exception-fqcn");
        String exceptionMessage = header(record, "kafka_dlt-exception-message");

        log.error("DLT: message dead-lettered — dltTopic={} originalTopic={} key={} offset={} exceptionClass={} exceptionMessage={}",
                record.topic(), originalTopic, record.key(), record.offset(),
                exceptionClass, exceptionMessage);

        ack.acknowledge();
    }

    private String header(ConsumerRecord<?, ?> record, String name) {
        Header h = record.headers().lastHeader(name);
        return h != null ? new String(h.value(), StandardCharsets.UTF_8) : "unknown";
    }
}
