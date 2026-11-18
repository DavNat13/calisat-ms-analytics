package com.califorge.msanalytics.ws;

import com.califorge.msanalytics.config.CalisatJwtAuthenticationConverter;
import com.califorge.msanalytics.service.MetricasService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/** Endpoint WebSocket nativo {@code /ws/metricas} y difusion periodica del snapshot. */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final MetricasWebSocketHandler handler;
    private final JwtDecoder jwtDecoder;
    private final CalisatJwtAuthenticationConverter roles;
    private final String[] allowedOrigins;

    public WebSocketConfig(MetricasWebSocketHandler handler, JwtDecoder jwtDecoder,
                           CalisatJwtAuthenticationConverter roles,
                           @Value("${calisat.analytics.websocket.allowed-origins}") String[] allowedOrigins) {
        this.handler = handler;
        this.jwtDecoder = jwtDecoder;
        this.roles = roles;
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    static MetricasWebSocketHandler metricasWebSocketHandler(MetricasService metricas) {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new MetricasWebSocketHandler(metricas, mapper);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/metricas")
                .addInterceptors(new JwtHandshakeInterceptor(jwtDecoder, roles))
                .setAllowedOrigins(allowedOrigins);
    }

    @Scheduled(fixedRateString = "${calisat.analytics.websocket.broadcast-ms:2000}")
    public void difundirMetricas() {
        handler.difundir();
    }
}
