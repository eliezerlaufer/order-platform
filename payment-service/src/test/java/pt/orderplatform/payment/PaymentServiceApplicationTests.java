package pt.orderplatform.payment;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "spring.kafka.listener.auto-startup=false")
class PaymentServiceApplicationTests extends BaseIntegrationTest {

    @Test
    void contextLoads() {
    }
}
