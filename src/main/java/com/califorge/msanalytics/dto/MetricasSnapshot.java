package com.califorge.msanalytics.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Instantanea de metricas en tiempo real enviada por WebSocket al dashboard:
 * (1) clics/eventos por minuto, (2) productos mas vistos, (3) eventos por tipo de accion,
 * (4) alertas de fraude recibidas.
 */
public record MetricasSnapshot(
        Instant timestamp,
        long eventosUltimoMinuto,
        long eventosTotales,
        Map<String, Long> eventosPorAccion,
        List<ProductoVisto> productosMasVistos,
        long alertasFraude) {

    public record ProductoVisto(String itemId, long vistas) {
    }
}
