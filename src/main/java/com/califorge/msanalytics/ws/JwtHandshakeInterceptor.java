package com.califorge.msanalytics.ws;

import com.califorge.msanalytics.config.CalisatJwtAuthenticationConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * Valida el JWT en el handshake WebSocket. Los navegadores no pueden enviar cabeceras en
 * {@code new WebSocket(url)}, por lo que el token viaja en {@code ?token=}. Solo ADMINISTRADOR.
 */
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(JwtHandshakeInterceptor.class);

    private final JwtDecoder decoder;
    private final CalisatJwtAuthenticationConverter roles;

    public JwtHandshakeInterceptor(JwtDecoder decoder, CalisatJwtAuthenticationConverter roles) {
        this.decoder = decoder;
        this.roles = roles;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler handler, Map<String, Object> attributes) {
        String token = request instanceof ServletServerHttpRequest servlet
                ? servlet.getServletRequest().getParameter("token") : null;
        if (token == null || token.isBlank()) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        try {
            Jwt jwt = decoder.decode(token);
            boolean admin = roles.convert(jwt).stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch("ROLE_ADMINISTRADOR"::equals);
            if (!admin) {
                response.setStatusCode(HttpStatus.FORBIDDEN);
                return false;
            }
            attributes.put("sub", jwt.getSubject());
            return true;
        } catch (RuntimeException e) {
            log.warn("Handshake WebSocket rechazado: {}", e.getMessage());
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler handler, Exception exception) {
        // sin accion
    }
}
