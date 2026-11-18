package com.califorge.msanalytics.repository;

import com.califorge.msanalytics.model.Interaccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface InteraccionRepository extends JpaRepository<Interaccion, Long> {

    boolean existsByEventId(String eventId);

    List<Interaccion> findByUserId(String userId);

    /** Popularidad global por producto dentro de una categoria (para completar el Top-5). */
    @Query("select i.itemId as itemId, sum(i.weight) as score from Interaccion i "
            + "where i.category = :category group by i.itemId order by sum(i.weight) desc")
    List<PopularidadItem> popularidadPorCategoria(String category);

    interface PopularidadItem {
        String getItemId();
        Long getScore();
    }
}
