package pt.orderplatform.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;

// =============================================================================
// SECURITY CONFIG — configuração de segurança com JWT/Keycloak
// =============================================================================
// REST APIs são stateless: não usam sessões HTTP (sem cookies de sessão).
// Cada pedido inclui o token JWT no header: Authorization: Bearer <token>
// O Spring valida o token contra o Keycloak (issuer-uri no application.yml).
//
// @Configuration → Spring lê esta classe na inicialização
// @EnableWebSecurity → activa o sistema de segurança do Spring Security
// =============================================================================
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF desactivado — REST APIs com JWT não precisam de protecção CSRF
            // (CSRF é um ataque contra sessões com cookies, não contra tokens JWT)
            .csrf(csrf -> csrf.disable())

            // Sessão stateless — sem HttpSession, sem cookies de sessão
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Regras de autorização por endpoint
            .authorizeHttpRequests(auth -> auth
                // Actuator endpoints (health, metrics) — acessíveis sem autenticação
                // O Kubernetes usa /actuator/health para liveness/readiness probes
                .requestMatchers("/actuator/**").permitAll()

                // Swagger UI — acessível sem autenticação (só em dev; em prod proteger)
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()

                // Todos os endpoints /api/** exigem autenticação
                .requestMatchers("/api/**").authenticated()

                // Qualquer outro endpoint → negar por defeito
                .anyRequest().denyAll()
            )

            // Resource Server com JWT — o Spring vai validar o token em cada pedido
            // A configuração do Keycloak (issuer-uri) está no application.yml
            .oauth2ResourceServer(oauth2 ->
                oauth2.jwt(jwt -> {})  // usa configuração do application.yml
            )

            // Tratamento de excepções de segurança:
            //   BearerTokenAuthenticationEntryPoint → 401 quando não há token (sem autenticação)
            //   BearerTokenAccessDeniedHandler      → 403 quando há token mas sem permissão
            // Sem isto, o Spring Security devolve 403 para ambos os casos em REST APIs.
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
                .accessDeniedHandler(new BearerTokenAccessDeniedHandler())
            );

        return http.build();
    }
}
