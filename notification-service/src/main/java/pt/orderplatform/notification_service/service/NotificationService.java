package pt.orderplatform.notification_service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.orderplatform.notification_service.domain.*;
import pt.orderplatform.notification_service.dto.NotificationResponse;
import pt.orderplatform.notification_service.exception.NotificationNotFoundException;
import pt.orderplatform.notification_service.repository.NotificationRepository;
import pt.orderplatform.notification_service.repository.OrderContextRepository;
import pt.orderplatform.notification_service.repository.ProcessedEventRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// =============================================================================
// NOTIFICATION SERVICE — Kafka listeners + lógica de notificações
// =============================================================================
// Consome:
//   orders.order.created        → guarda OrderContext + envia ORDER_CREATED
//   orders.order.cancelled      → envia ORDER_CANCELLED
//   payments.payment.processed  → envia PAYMENT_PROCESSED
//   payments.payment.failed     → envia PAYMENT_FAILED
//   inventory.reservation.failed→ envia STOCK_UNAVAILABLE
//
// Todos os eventos passam por idempotência (ProcessedEvent).
// Eventos sem customerId resolvem pelo OrderContext local.
// =============================================================================
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final OrderContextRepository orderContextRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final EmailGateway emailGateway;
    private final ObjectMapper objectMapper;

    // =========================================================================
    // ORDERS.ORDER.CREATED — guarda contexto + notifica
    // =========================================================================

    @KafkaListener(topics = "orders.order.created", groupId = "notification-service")
    @Transactional
    public void onOrderCreated(ConsumerRecord<String, String> record, Acknowledgment ack) throws Exception {
        JsonNode payload    = objectMapper.readTree(record.value());
        UUID eventId        = UUID.fromString(payload.get("eventId").asText());
        UUID orderId        = UUID.fromString(payload.get("orderId").asText());
        UUID customerId     = UUID.fromString(payload.get("customerId").asText());

        if (processedEventRepository.existsById(eventId)) {
            log.info("Duplicate event {} on orders.order.created — skipping", eventId);
            ack.acknowledge();
            return;
        }
        processedEventRepository.save(ProcessedEvent.of(eventId));

        if (!orderContextRepository.existsById(orderId)) {
            orderContextRepository.save(OrderContext.of(orderId, customerId));
        }

        String message = "Your order %s has been placed successfully.".formatted(orderId);
        saveAndSend(orderId, customerId, NotificationType.ORDER_CREATED, message);
        ack.acknowledge();
    }

    // =========================================================================
    // ORDERS.ORDER.CANCELLED — customerId está no payload
    // =========================================================================

    @KafkaListener(topics = "orders.order.cancelled", groupId = "notification-service")
    @Transactional
    public void onOrderCancelled(ConsumerRecord<String, String> record, Acknowledgment ack) throws Exception {
        JsonNode payload    = objectMapper.readTree(record.value());
        UUID eventId        = UUID.fromString(payload.get("eventId").asText());
        UUID orderId        = UUID.fromString(payload.get("orderId").asText());
        UUID customerId     = UUID.fromString(payload.get("customerId").asText());

        if (processedEventRepository.existsById(eventId)) {
            log.info("Duplicate event {} on orders.order.cancelled — skipping", eventId);
            ack.acknowledge();
            return;
        }
        processedEventRepository.save(ProcessedEvent.of(eventId));

        String message = "Your order %s has been cancelled.".formatted(orderId);
        saveAndSend(orderId, customerId, NotificationType.ORDER_CANCELLED, message);
        ack.acknowledge();
    }

    // =========================================================================
    // PAYMENTS.PAYMENT.PROCESSED — resolve customerId via OrderContext
    // =========================================================================

    @KafkaListener(topics = "payments.payment.processed", groupId = "notification-service")
    @Transactional
    public void onPaymentProcessed(ConsumerRecord<String, String> record, Acknowledgment ack) throws Exception {
        JsonNode payload = objectMapper.readTree(record.value());
        UUID eventId     = UUID.fromString(payload.get("eventId").asText());
        UUID orderId     = UUID.fromString(payload.get("orderId").asText());

        if (processedEventRepository.existsById(eventId)) {
            log.info("Duplicate event {} on payments.payment.processed — skipping", eventId);
            ack.acknowledge();
            return;
        }
        processedEventRepository.save(ProcessedEvent.of(eventId));

        resolveCustomer(orderId).ifPresent(customerId -> {
            String message = "Payment for order %s has been approved.".formatted(orderId);
            saveAndSend(orderId, customerId, NotificationType.PAYMENT_PROCESSED, message);
        });

        ack.acknowledge();
    }

    // =========================================================================
    // PAYMENTS.PAYMENT.FAILED — resolve customerId via OrderContext
    // =========================================================================

    @KafkaListener(topics = "payments.payment.failed", groupId = "notification-service")
    @Transactional
    public void onPaymentFailed(ConsumerRecord<String, String> record, Acknowledgment ack) throws Exception {
        JsonNode payload = objectMapper.readTree(record.value());
        UUID eventId     = UUID.fromString(payload.get("eventId").asText());
        UUID orderId     = UUID.fromString(payload.get("orderId").asText());

        if (processedEventRepository.existsById(eventId)) {
            log.info("Duplicate event {} on payments.payment.failed — skipping", eventId);
            ack.acknowledge();
            return;
        }
        processedEventRepository.save(ProcessedEvent.of(eventId));

        resolveCustomer(orderId).ifPresent(customerId -> {
            String message = "Payment for order %s was declined. Please review your payment details.".formatted(orderId);
            saveAndSend(orderId, customerId, NotificationType.PAYMENT_FAILED, message);
        });

        ack.acknowledge();
    }

    // =========================================================================
    // INVENTORY.RESERVATION.FAILED — resolve customerId via OrderContext
    // =========================================================================

    @KafkaListener(topics = "inventory.reservation.failed", groupId = "notification-service")
    @Transactional
    public void onInventoryReservationFailed(ConsumerRecord<String, String> record, Acknowledgment ack) throws Exception {
        JsonNode payload = objectMapper.readTree(record.value());
        UUID eventId     = UUID.fromString(payload.get("eventId").asText());
        UUID orderId     = UUID.fromString(payload.get("orderId").asText());

        if (processedEventRepository.existsById(eventId)) {
            log.info("Duplicate event {} on inventory.reservation.failed — skipping", eventId);
            ack.acknowledge();
            return;
        }
        processedEventRepository.save(ProcessedEvent.of(eventId));

        resolveCustomer(orderId).ifPresent(customerId -> {
            String message = "Sorry, one or more items in order %s are out of stock. Your order has been cancelled.".formatted(orderId);
            saveAndSend(orderId, customerId, NotificationType.STOCK_UNAVAILABLE, message);
        });

        ack.acknowledge();
    }

    // =========================================================================
    // QUERY — usado pelo controller
    // =========================================================================

    @Transactional(readOnly = true)
    public List<NotificationResponse> getByOrderId(UUID orderId) {
        List<Notification> notifications = notificationRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
        if (notifications.isEmpty()) {
            throw new NotificationNotFoundException("No notifications found for orderId: " + orderId);
        }
        return notifications.stream().map(NotificationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getByCustomerId(UUID customerId) {
        return notificationRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream().map(NotificationResponse::from).toList();
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private void saveAndSend(UUID orderId, UUID customerId, NotificationType type, String message) {
        Notification notification = Notification.of(orderId, customerId, type, message);
        notificationRepository.save(notification);
        emailGateway.send(customerId, type, message);
        log.info("Notification {} saved for order {} customer {}", type, orderId, customerId);
    }

    private Optional<UUID> resolveCustomer(UUID orderId) {
        Optional<OrderContext> ctx = orderContextRepository.findById(orderId);
        if (ctx.isEmpty()) {
            log.warn("No OrderContext found for orderId={} — notification skipped", orderId);
        }
        return ctx.map(OrderContext::getCustomerId);
    }
}
