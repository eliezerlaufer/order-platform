package pt.orderplatform.api_gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class ApiGatewayApplicationTests {

    // Prevents Spring Security from connecting to Keycloak at startup
    @MockitoBean
    JwtDecoder jwtDecoder;

    @Test
    void contextLoads() {
    }
}
