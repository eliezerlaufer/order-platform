package pt.orderplatform.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// =============================================================================
// INVENTORY-SERVICE — ponto de entrada
// =============================================================================
// @EnableScheduling → necessário para o OutboxPublisher (@Scheduled)
// =============================================================================
@SpringBootApplication
@EnableScheduling
public class InventoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }
}
