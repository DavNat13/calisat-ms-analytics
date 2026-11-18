package com.califorge.msanalytics;

import com.califorge.msanalytics.dto.InteraccionEvent;
import com.califorge.msanalytics.model.Recomendacion;
import com.califorge.msanalytics.repository.RecomendacionRepository;
import com.califorge.msanalytics.service.RecomendacionService;
import com.califorge.msanalytics.store.InteraccionStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RecomendacionServiceTest {

    /** Almacen en memoria con idempotencia por eventId. */
    static class MemoriaStore implements InteraccionStore {
        final List<InteraccionEvent> datos = new ArrayList<>();
        final List<ItemScore> populares = new ArrayList<>();

        @Override public boolean guardar(InteraccionEvent e) {
            if (datos.stream().anyMatch(d -> d.eventId().equals(e.eventId()))) return false;
            datos.add(e);
            return true;
        }
        @Override public List<InteraccionEvent> historialUsuario(String userId) {
            return datos.stream().filter(d -> d.userId().equals(userId)).toList();
        }
        @Override public List<ItemScore> popularesPorCategoria(String category, int limite) {
            return populares;
        }
    }

    private MemoriaStore store;
    private RecomendacionRepository repo;
    private RecomendacionService service;

    @BeforeEach
    void setUp() {
        store = new MemoriaStore();
        repo = mock(RecomendacionRepository.class);
        service = new RecomendacionService(store, repo, Clock.systemUTC());
    }

    private InteraccionEvent ev(String user, String item, String action, String cat) {
        return new InteraccionEvent(UUID.randomUUID().toString(), user, item, action, cat, "std", Instant.now());
    }

    @SuppressWarnings("unchecked")
    private List<Recomendacion> guardadas() {
        ArgumentCaptor<List<Recomendacion>> c = ArgumentCaptor.forClass(List.class);
        verify(repo).saveAll(c.capture());
        return c.getValue();
    }

    @Test
    void pesosViewCartPurchaseOrdenanElRanking() {
        service.procesar(ev("u1", "A", "view", "barras"));          // 1
        service.procesar(ev("u1", "B", "cart", "barras"));          // 2
        service.procesar(ev("u1", "C", "purchase", "barras"));      // 3
        service.procesar(ev("u1", "A", "view", "barras"));          // A = 2
        List<Recomendacion> top = service.recalcularTop5("u1");
        assertEquals("C", top.get(0).getItemId());
        assertEquals(3.0, top.get(0).getScore());
        assertEquals(1, top.get(0).getRankPosition());
        assertEquals(3, top.size());
    }

    @Test
    void eventoDuplicadoSeIgnora() {
        InteraccionEvent e = ev("u1", "A", "view", "barras");
        assertTrue(service.procesar(e));
        assertFalse(service.procesar(e));
    }

    @Test
    void completaConProductosParecidosDeLaMismaCategoria() {
        store.populares.add(new InteraccionStore.ItemScore("X", 50));
        store.populares.add(new InteraccionStore.ItemScore("A", 40));   // ya visto: se excluye
        service.procesar(ev("u1", "A", "view", "barras"));
        List<Recomendacion> top = guardadasUltima();
        assertEquals(List.of("A", "X"), top.stream().map(Recomendacion::getItemId).toList());
    }

    @SuppressWarnings("unchecked")
    private List<Recomendacion> guardadasUltima() {
        ArgumentCaptor<List<Recomendacion>> c = ArgumentCaptor.forClass(List.class);
        verify(repo, org.mockito.Mockito.atLeastOnce()).saveAll(c.capture());
        return c.getValue();
    }

    @Test
    void maximoCincoRecomendaciones() {
        for (int i = 0; i < 8; i++) {
            service.procesar(ev("u1", "P" + i, "view", "barras"));
        }
        assertEquals(5, service.recalcularTop5("u1").size());
    }

    @Test
    void payloadInvalidoLanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> service.procesar(null));
        assertThrows(IllegalArgumentException.class,
                () -> service.procesar(new InteraccionEvent("", "u", "i", "view", null, null, Instant.now())));
    }
}
