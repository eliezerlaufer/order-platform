package pt.orderplatform.payment_service.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import pt.orderplatform.payment_service.BaseIntegrationTest;
import pt.orderplatform.payment_service.domain.Payment;
import pt.orderplatform.payment_service.repository.PaymentRepository;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// =============================================================================
// PAYMENT CONTROLLER TEST
// =============================================================================
@AutoConfigureMockMvc
class PaymentControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    void shouldReturn404WhenPaymentNotFound() throws Exception {
        UUID unknownOrderId = UUID.randomUUID();

        mockMvc.perform(get("/api/payments/order/{orderId}", unknownOrderId)
                        .with(jwt())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnPaymentByOrderId() throws Exception {
        UUID orderId = UUID.randomUUID();
        Payment payment = Payment.pending(orderId, new BigDecimal("120.00"));
        payment.markProcessed();
        paymentRepository.save(payment);

        mockMvc.perform(get("/api/payments/order/{orderId}", orderId)
                        .with(jwt())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("PROCESSED"))
                .andExpect(jsonPath("$.amount").value(120.00));
    }

    @Test
    void shouldReturn401WhenNoToken() throws Exception {
        mockMvc.perform(get("/api/payments/order/{orderId}", UUID.randomUUID())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
