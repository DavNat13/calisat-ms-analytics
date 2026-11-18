package com.califorge.msanalytics.repository;

import com.califorge.msanalytics.model.Recomendacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RecomendacionRepository extends JpaRepository<Recomendacion, Long> {

    List<Recomendacion> findByUserIdOrderByRankPositionAsc(String userId);

    @Modifying
    @Query("delete from Recomendacion r where r.userId = :userId")
    void deleteByUserId(String userId);
}
