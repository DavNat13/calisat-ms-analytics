package com.califorge.msanalytics;

import com.califorge.msanalytics.dto.InteraccionEvent;
import com.califorge.msanalytics.dto.MetricasSnapshot;
import com.califorge.msanalytics.service.MetricasService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MetricasServiceTest {

    static class Reloj extends Clock {
        Instant ahora = Instant.parse("2026-11-18T12:00:00Z");
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId z) { return this; }
        @Override public Instant instant() { return ahora; }
    }

    private InteraccionEvent ev(String item, String action) {
        return new InteraccionEvent("id" + Math.random(), "u", item, action, "c", "t", Instant.now());
    }

    @Test
    void calculaLasMetricasDelDashboard() {
        Reloj reloj = new Reloj();
        MetricasService m = new MetricasService(reloj);
        m.registrarInteraccion(ev("A", "view"));
        m.registrarInteraccion(ev("A", "view"));
        m.registrarInteraccion(ev("B", "view"));
        m.registrarInteraccion(ev("B", "cart"));
        m.registrarAlertaFraude();

        MetricasSnapshot s = m.snapshot();
        assertEquals(4, s.eventosTotales());
        assertEquals(4, s.eventosUltimoMinuto());
        assertEquals(3L, s.eventosPorAccion().get("view"));
        assertEquals(1L, s.eventosPorAccion().get("cart"));
        assertEquals("A", s.productosMasVistos().get(0).itemId());
        assertEquals(2, s.productosMasVistos().get(0).vistas());
        assertEquals(1, s.alertasFraude());

        reloj.ahora = reloj.ahora.plus(Duration.ofSeconds(90));
        assertEquals(0, m.snapshot().eventosUltimoMinuto());
        assertEquals(4, m.snapshot().eventosTotales());
    }
}
