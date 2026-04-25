package pt.orderplatform.notification_service.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import pt.orderplatform.notification_service.BaseIntegrationTest;
import pt.orderplatform.notification_service.domain.Notification;
import pt.orderplatform.notification_service.domain.NotificationType;
import pt.orderplatform.notification_service.repository.NotificationRepository;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// =============================================================================
// NOTIFICATION CONTROLLER TEST
// =============================================================================
@AutoConfigureMockMvc
class NotificationControllerTest extends BaseIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired NotificationRepository notificationRepository;

    @Test
    void shouldReturn404WhenNoNotificationsForOrder() throws Exception {
        mockMvc.perform(get("/api/notifications/order/{orderId}", UUID.randomUUID())
                        .with(jwt())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnNotificationsByOrderId() throws Exception {
        UUID orderId    = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        notificationRepository.save(Notification.of(orderId, customerId, NotificationType.ORDER_CREATED,
                "Your order has been placed."));

        mockMvc.perform(get("/api/notifications/order/{orderId}", orderId)
                        .with(jwt())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").value(orderId.toString()))
                .andExpect(jsonPath("$[0].type").value("ORDER_CREATED"));
    }

    @Test
    void shouldReturn401WhenNoToken() throws Exception {
        mockMvc.perform(get("/api/notifications/order/{orderId}", UUID.randomUUID())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnNotificationsByCustomerId() throws Exception {
        UUID orderId    = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        notificationRepository.save(Notification.of(orderId, customerId, NotificationType.ORDER_CREATED,
                "Your order has been placed."));
        notificationRepository.save(Notification.of(UUID.randomUUID(), customerId, NotificationType.PAYMENT_PROCESSED,
                "Payment approved."));

        mockMvc.perform(get("/api/notifications/customer/{customerId}", customerId)
                        .with(jwt())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].customerId").value(customerId.toString()));
    }

    @Test
    void shouldReturnEmptyListWhenNoNotificationsForCustomer() throws Exception {
        mockMvc.perform(get("/api/notifications/customer/{customerId}", UUID.randomUUID())
                        .with(jwt())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
