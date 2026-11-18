package com.califorge.msanalytics.store;

import com.califorge.msanalytics.dto.InteraccionEvent;
import com.califorge.msanalytics.model.Interaccion;
import com.califorge.msanalytics.repository.InteraccionRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Almacen relacional (PostgreSQL) del historial de interacciones. */
@Component
@Profile("!dynamodb")
public class JpaInteraccionStore implements InteraccionStore {

    private final InteraccionRepository repo;

    public JpaInteraccionStore(InteraccionRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional
    public boolean guardar(InteraccionEvent e) {
        if (repo.existsByEventId(e.eventId())) {
            return false;
        }
        Interaccion i = new Interaccion();
        i.setEventId(e.eventId());
        i.setUserId(e.userId());
        i.setItemId(e.itemId());
        i.setAction(e.action().toLowerCase());
        i.setWeight(e.weight());
        i.setCategory(e.category());
        i.setProductType(e.productType());
        i.setMomento(e.timestamp());
        repo.save(i);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InteraccionEvent> historialUsuario(String userId) {
        return repo.findByUserId(userId).stream()
                .map(i -> new InteraccionEvent(i.getEventId(), i.getUserId(), i.getItemId(), i.getAction(),
                        i.getCategory(), i.getProductType(), i.getMomento()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemScore> popularesPorCategoria(String category, int limite) {
        if (category == null) {
            return List.of();
        }
        return repo.popularidadPorCategoria(category).stream()
                .limit(limite)
                .map(p -> new ItemScore(p.getItemId(), p.getScore()))
                .toList();
    }
}
