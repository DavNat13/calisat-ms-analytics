package com.califorge.msanalytics.store;

import com.califorge.msanalytics.dto.InteraccionEvent;

import java.util.List;

/**
 * Almacen del historial de interacciones. La implementacion por defecto usa JPA (PostgreSQL);
 * con el perfil {@code dynamodb} se persiste en Amazon DynamoDB (tabla UserInteractions).
 */
public interface InteraccionStore {

    /** Guarda el evento. Devuelve false si ya existia (idempotencia ante reentregas de Kafka). */
    boolean guardar(InteraccionEvent evento);

    /** Historial completo de un usuario. */
    List<InteraccionEvent> historialUsuario(String userId);

    /** Productos mas populares (suma de pesos) de una categoria. */
    List<ItemScore> popularesPorCategoria(String category, int limite);

    record ItemScore(String itemId, double score) {
    }
}
