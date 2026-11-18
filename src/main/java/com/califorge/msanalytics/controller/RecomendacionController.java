package com.califorge.msanalytics.controller;

import com.califorge.msanalytics.dto.MetricasSnapshot;
import com.califorge.msanalytics.dto.RecomendacionResponse;
import com.califorge.msanalytics.service.MetricasService;
import com.califorge.msanalytics.service.RecomendacionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class RecomendacionController {

    private final RecomendacionService recomendaciones;
    private final MetricasService metricas;

    public RecomendacionController(RecomendacionService recomendaciones, MetricasService metricas) {
        this.recomendaciones = recomendaciones;
        this.metricas = metricas;
    }

    /** Top-5 del usuario autenticado. */
    @GetMapping("/api/v1/recomendaciones/me")
    public List<RecomendacionResponse> mias(@AuthenticationPrincipal Jwt jwt) {
        return recomendaciones.obtenerTop5(jwt.getSubject()).stream().map(RecomendacionResponse::de).toList();
    }

    /** Top-5 de otro usuario: solo ADMINISTRADOR; un cliente solo puede consultar el suyo. */
    @GetMapping("/api/v1/recomendaciones/{userId}")
    public ResponseEntity<List<RecomendacionResponse>> deUsuario(@PathVariable String userId,
                                                                 @AuthenticationPrincipal Jwt jwt,
                                                                 Authentication auth) {
        boolean admin = auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMINISTRADOR".equals(a.getAuthority()));
        if (!admin && !jwt.getSubject().equals(userId)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(recomendaciones.obtenerTop5(userId).stream()
                .map(RecomendacionResponse::de).toList());
    }

    /** Snapshot REST de las metricas (respaldo cuando no hay WebSocket). */
    @GetMapping("/api/v1/analytics/metricas")
    public MetricasSnapshot metricas() {
        return metricas.snapshot();
    }
}
