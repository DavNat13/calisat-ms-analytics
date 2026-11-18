package com.califorge.msanalytics.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

/** Recomendacion calculada (Top-5 por usuario). */
@Entity
@Table(name = "recomendaciones", indexes = @Index(name = "idx_reco_user_rank", columnList = "user_id, rank_position"))
public class Recomendacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 150)
    private String userId;

    @Column(name = "item_id", nullable = false, length = 80)
    private String itemId;

    @Column(nullable = false)
    private double score;

    @Column(name = "rank_position", nullable = false)
    private int rankPosition;

    @Column(length = 80)
    private String category;

    @Column(name = "product_type", length = 80)
    private String productType;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Recomendacion() {
    }

    public Recomendacion(String userId, String itemId, double score, int rankPosition,
                         String category, String productType, Instant updatedAt) {
        this.userId = userId;
        this.itemId = itemId;
        this.score = score;
        this.rankPosition = rankPosition;
        this.category = category;
        this.productType = productType;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public String getItemId() { return itemId; }
    public double getScore() { return score; }
    public int getRankPosition() { return rankPosition; }
    public String getCategory() { return category; }
    public String getProductType() { return productType; }
    public Instant getUpdatedAt() { return updatedAt; }
}
