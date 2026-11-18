package com.califorge.msanalytics.service;

import com.califorge.msanalytics.dto.InteraccionEvent;
import com.califorge.msanalytics.model.Recomendacion;
import com.califorge.msanalytics.repository.RecomendacionRepository;
import com.califorge.msanalytics.store.InteraccionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Motor de recomendaciones: puntua los productos con los que el usuario interactuo (view=1, cart=2,
 * purchase=3) y, si el Top-5 queda incompleto, lo completa con productos populares de las mismas
 * categorias ("productos parecidos") que el usuario aun no ha visto.
 */
@Service
public class RecomendacionService {

    static final int TOP = 5;
    private static final Logger log = LoggerFactory.getLogger(RecomendacionService.class);

    private final InteraccionStore store;
    private final RecomendacionRepository recomendaciones;
    private final Clock clock;

    public RecomendacionService(InteraccionStore store, RecomendacionRepository recomendaciones, Clock clock) {
        this.store = store;
        this.recomendaciones = recomendaciones;
        this.clock = clock;
    }

    /** Procesa un evento: lo persiste (idempotente) y recalcula el Top-5 del usuario. */
    @Transactional
    public boolean procesar(InteraccionEvent evento) {
        validar(evento);
        boolean nuevo = store.guardar(evento);
        if (!nuevo) {
            log.debug("Interaccion {} ya procesada, se ignora", evento.eventId());
            return false;
        }
        recalcularTop5(evento.userId());
        return true;
    }

    @Transactional
    public List<Recomendacion> recalcularTop5(String userId) {
        List<InteraccionEvent> historial = store.historialUsuario(userId);
        if (historial.isEmpty()) {
            return List.of();
        }
        Map<String, Double> puntaje = new HashMap<>();
        Map<String, String> categoriaDe = new HashMap<>();
        Map<String, String> tipoDe = new HashMap<>();
        for (InteraccionEvent e : historial) {
            puntaje.merge(e.itemId(), (double) e.weight(), Double::sum);
            if (e.category() != null) {
                categoriaDe.put(e.itemId(), e.category());
            }
            if (e.productType() != null) {
                tipoDe.put(e.itemId(), e.productType());
            }
        }

        List<Map.Entry<String, Double>> ranking = puntaje.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(TOP)
                .collect(Collectors.toCollection(ArrayList::new));

        // Completar con productos parecidos (misma categoria) que el usuario aun no ha consultado
        if (ranking.size() < TOP) {
            Map<String, Double> extra = new LinkedHashMap<>();
            for (String categoria : categoriaDe.values().stream().distinct().toList()) {
                for (InteraccionStore.ItemScore p : store.popularesPorCategoria(categoria, TOP * 2)) {
                    if (!puntaje.containsKey(p.itemId())) {
                        extra.merge(p.itemId(), p.score() / 10.0, Double::sum);   // menor peso que lo propio
                        categoriaDe.putIfAbsent(p.itemId(), categoria);
                    }
                }
            }
            extra.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed()
                            .thenComparing(Map.Entry.comparingByKey()))
                    .limit(TOP - ranking.size())
                    .forEach(ranking::add);
        }

        recomendaciones.deleteByUserId(userId);
        List<Recomendacion> nuevas = new ArrayList<>();
        int posicion = 1;
        for (Map.Entry<String, Double> en : ranking) {
            nuevas.add(new Recomendacion(userId, en.getKey(), en.getValue(), posicion++,
                    categoriaDe.get(en.getKey()), tipoDe.get(en.getKey()), clock.instant()));
        }
        recomendaciones.saveAll(nuevas);
        log.info("Top-{} recalculado para usuario {}", nuevas.size(), userId);
        return nuevas;
    }

    @Transactional(readOnly = true)
    public List<Recomendacion> obtenerTop5(String userId) {
        return recomendaciones.findByUserIdOrderByRankPositionAsc(userId);
    }

    private static void validar(InteraccionEvent e) {
        if (e == null || isBlank(e.eventId()) || isBlank(e.userId()) || isBlank(e.itemId())
                || isBlank(e.action()) || e.timestamp() == null) {
            throw new IllegalArgumentException("InteraccionEvent invalido (campos obligatorios ausentes)");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
