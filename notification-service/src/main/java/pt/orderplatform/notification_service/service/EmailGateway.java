package pt.orderplatform.notification_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pt.orderplatform.notification_service.domain.NotificationType;

import java.util.UUID;

// =============================================================================
// EMAIL GATEWAY — mock do envio de email
// =============================================================================
// Em produção, integraria com SendGrid / AWS SES / JavaMailSender.
// Aqui apenas regista o envio em log para fins de estudo.
// =============================================================================
@Slf4j
@Service
public class EmailGateway {

    public void send(UUID customerId, NotificationType type, String message) {
        log.info("[EMAIL MOCK] To=customer:{} Type={} Message=\"{}\"", customerId, type, message);
    }
}
