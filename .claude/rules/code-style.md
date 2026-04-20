# Code Style Rules

## Java / Spring Boot

### DTOs — always immutable Java records
```java
// CORRECT
public record CreateOrderRequest(
    @NotNull UUID productId,
    @NotBlank String productName,
    @Min(1) Integer quantity,
    @DecimalMin("0.01") BigDecimal unitPrice
) {}

// WRONG — mutable class with setters
public class CreateOrderRequest {
    private UUID productId;
    public void setProductId(UUID id) { this.productId = id; }
}
```

### Entities — use Lombok, never expose JPA entities in API responses
```java
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {
    // Always map to a DTO before returning from controller
}
```

### Services — constructor injection, no @Autowired on fields
```java
// CORRECT
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
}
```

### Error handling
- Use `@ControllerAdvice` + `GlobalExceptionHandler` — already exists in each service
- Never return 500 for business errors — map to 400/404/409
- Never expose stack traces in API responses

### Naming
- Topics: `<domain>.<entity>.<event>` lowercase with dots
- DB tables: `snake_case`
- Java classes: `PascalCase`, methods: `camelCase`

## TypeScript / React

### Type safety — strict mode is on, zero `any`
```typescript
// WRONG
function process(data: any) { }

// CORRECT
function process(data: OrderResponse) { }
```

### Immutability — never mutate state directly
```typescript
// WRONG
items.push(newItem)
item.quantity = 2

// CORRECT
setItems(prev => [...prev, newItem])
setItems(prev => prev.map(i => i.key === key ? { ...i, quantity: 2 } : i))
```

### Components — named exports, never default for components
```typescript
// CORRECT
export function OrderStatusBadge({ status }: Props) { }

// Exception: page-level components and App.tsx use default export
```

### Hooks — one hook per concern, no business logic in components
```typescript
// API state lives in hooks (useOrders, usePayment, etc.)
// UI state lives in components (useState for form fields)
```

### ESLint rules enforced
- `react-refresh/only-export-components` — components and contexts in separate files
- `@typescript-eslint/no-explicit-any`
- `react-hooks/rules-of-hooks`
- `react-hooks/exhaustive-deps`
