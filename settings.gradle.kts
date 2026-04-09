rootProject.name = "order-platform"

// Cada linha inclui um módulo do monorepo
include(
    "order-service",
    "inventory-service",
    "payment-service",
    "notification-service",
    "api-gateway"
)
