package pt.orderplatform.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pt.orderplatform.inventory.config.SecurityConfig;
import pt.orderplatform.inventory.dto.CreateProductRequest;
import pt.orderplatform.inventory.dto.ProductResponse;
import pt.orderplatform.inventory.dto.RestockRequest;
import pt.orderplatform.inventory.exception.GlobalExceptionHandler;
import pt.orderplatform.inventory.exception.ProductNotFoundException;
import pt.orderplatform.inventory.service.ProductService;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import pt.orderplatform.inventory.exception.InsufficientStockException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private final UUID adminId = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    @DisplayName("POST /api/products → 201 com produto criado")
    void create_shouldReturn201() throws Exception {
        CreateProductRequest request = new CreateProductRequest("Teclado", "KBD-001", 100);
        ProductResponse response = sampleResponse();

        when(productService.create(any(CreateProductRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/products")
                        .with(jwt().jwt(j -> j.subject(adminId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("KBD-001"))
                .andExpect(jsonPath("$.availableQuantity").value(100));
    }

    @Test
    @DisplayName("POST /api/products → 400 quando sku em falta")
    void create_shouldReturn400WhenSkuMissing() throws Exception {
        CreateProductRequest invalid = new CreateProductRequest("Teclado", "", 10);

        mockMvc.perform(post("/api/products")
                        .with(jwt().jwt(j -> j.subject(adminId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/products → 4xx sem autenticação")
    void create_shouldBlockUnauthenticated() throws Exception {
        CreateProductRequest request = new CreateProductRequest("Teclado", "KBD-001", 100);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("GET /api/products → 200 com lista")
    void list_shouldReturn200() throws Exception {
        when(productService.findAll()).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/products")
                        .with(jwt().jwt(j -> j.subject(adminId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("GET /api/products/{id} → 200 quando existe")
    void findById_shouldReturn200() throws Exception {
        ProductResponse response = sampleResponse();
        when(productService.findById(response.id())).thenReturn(response);

        mockMvc.perform(get("/api/products/{id}", response.id())
                        .with(jwt().jwt(j -> j.subject(adminId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(response.id().toString()));
    }

    @Test
    @DisplayName("GET /api/products/{id} → 404 quando inexistente")
    void findById_shouldReturn404() throws Exception {
        UUID unknown = UUID.randomUUID();
        when(productService.findById(unknown)).thenThrow(new ProductNotFoundException(unknown));

        mockMvc.perform(get("/api/products/{id}", unknown)
                        .with(jwt().jwt(j -> j.subject(adminId.toString()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Product Not Found"));
    }

    @Test
    @DisplayName("PATCH /api/products/{id}/restock → 200 com novo available")
    void restock_shouldReturn200() throws Exception {
        ProductResponse response = sampleResponse();
        when(productService.restock(eq(response.id()), any(RestockRequest.class)))
                .thenReturn(response);

        mockMvc.perform(patch("/api/products/{id}/restock", response.id())
                        .with(jwt().jwt(j -> j.subject(adminId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RestockRequest(50))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(100));
    }

    @Test
    @DisplayName("PATCH /api/products/{id}/restock → 400 quando quantity <= 0")
    void restock_shouldReturn400OnInvalidQuantity() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(patch("/api/products/{id}/restock", id)
                        .with(jwt().jwt(j -> j.subject(adminId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RestockRequest(0))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/products → 409 quando SKU duplicado (DataIntegrityViolationException)")
    void create_shouldReturn409WhenDuplicateSku() throws Exception {
        CreateProductRequest request = new CreateProductRequest("Teclado", "KBD-001", 100);
        when(productService.create(any(CreateProductRequest.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        mockMvc.perform(post("/api/products")
                        .with(jwt().jwt(j -> j.subject(adminId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflict"));
    }

    @Test
    @DisplayName("PATCH /api/products/{id}/restock → 400 quando argumento inválido")
    void restock_shouldReturn400WhenIllegalArgument() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.restock(eq(id), any(RestockRequest.class)))
                .thenThrow(new IllegalArgumentException("quantity must be positive"));

        mockMvc.perform(patch("/api/products/{id}/restock", id)
                        .with(jwt().jwt(j -> j.subject(adminId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RestockRequest(10))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"));
    }

    @Test
    @DisplayName("GET /api/products/{id} → 409 quando stock insuficiente simulado")
    void findById_shouldReturn409WhenInsufficientStock() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.findById(id))
                .thenThrow(new InsufficientStockException(id, 10, 5));

        mockMvc.perform(get("/api/products/{id}", id)
                        .with(jwt().jwt(j -> j.subject(adminId.toString()))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Insufficient Stock"));
    }

    private ProductResponse sampleResponse() {
        return new ProductResponse(
                UUID.randomUUID(),
                "Teclado",
                "KBD-001",
                100,
                0,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
    }
}
