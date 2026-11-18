package com.califorge.msanalytics.dto;

import java.time.Instant;

/** Copia local del evento publicado por ms-pago en {@code payment-alerts} (alias de tipo "alertaFraude"). */
public record AlertaFraudeEvent(
        String tarjetaHash,
        String tarjetaUltimos4,
        String clienteSub,
        int intentos,
        int ventanaSegundos,
        Instant bloqueadoHasta,
        Instant timestamp) {
}
