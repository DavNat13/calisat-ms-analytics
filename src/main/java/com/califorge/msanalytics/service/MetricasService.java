package com.califorge.msanalytics.service;

import com.califorge.msanalytics.dto.InteraccionEvent;
import com.califorge.msanalytics.dto.MetricasSnapshot;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** Metricas en memoria alimentadas por Kafka y publicadas por WebSocket al dashboard. */
@Service
public class MetricasService {

    private final Clock clock;
    private final AtomicLong totales = new AtomicLong();
    private final AtomicLong alertasFraude = new AtomicLong();
    private final Map<String, AtomicLong> porAccion = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> vistasPorProducto = new ConcurrentHashMap<>();
    private final Deque<Instant> ultimoMinuto = new ArrayDeque<>();

    public MetricasService(Clock clock) {
        this.clock = clock;
    }

    public void registrarInteraccion(InteraccionEvent e) {
        totales.incrementAndGet();
        porAccion.computeIfAbsent(e.action().toLowerCase(), k -> new AtomicLong()).incrementAndGet();
        if (InteraccionEvent.VIEW.equalsIgnoreCase(e.action())) {
            vistasPorProducto.computeIfAbsent(e.itemId(), k -> new AtomicLong()).incrementAndGet();
        }
        synchronized (ultimoMinuto) {
            ultimoMinuto.addLast(clock.instant());
            podar();
        }
    }

    public void registrarAlertaFraude() {
        alertasFraude.incrementAndGet();
    }

    public MetricasSnapshot snapshot() {
        long ultimoMin;
        synchronized (ultimoMinuto) {
            podar();
            ultimoMin = ultimoMinuto.size();
        }
        Map<String, Long> acciones = new TreeMap<>();
        porAccion.forEach((k, v) -> acciones.put(k, v.get()));
        List<MetricasSnapshot.ProductoVisto> top = vistasPorProducto.entrySet().stream()
                .map(en -> new MetricasSnapshot.ProductoVisto(en.getKey(), en.getValue().get()))
                .sorted(Comparator.comparingLong(MetricasSnapshot.ProductoVisto::vistas).reversed()
                        .thenComparing(MetricasSnapshot.ProductoVisto::itemId))
                .limit(5)
                .toList();
        return new MetricasSnapshot(clock.instant(), ultimoMin, totales.get(), acciones, top,
                alertasFraude.get());
    }

    private void podar() {
        Instant limite = clock.instant().minusSeconds(60);
        while (!ultimoMinuto.isEmpty() && ultimoMinuto.peekFirst().isBefore(limite)) {
            ultimoMinuto.pollFirst();
        }
    }
}
