package com.califorge.msanalytics.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

/** Historial de interacciones (almacen JPA por defecto). */
@Entity
@Table(name = "interacciones", indexes = {
        @Index(name = "idx_interacciones_user", columnList = "user_id"),
        @Index(name = "idx_interacciones_item", columnList = "item_id")})
public class Interaccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 60)
    private String eventId;

    @Column(name = "user_id", nullable = false, length = 150)
    private String userId;

    @Column(name = "item_id", nullable = false, length = 80)
    private String itemId;

    @Column(nullable = false, length = 20)
    private String action;

    @Column(nullable = false)
    private int weight;

    @Column(length = 80)
    private String category;

    @Column(name = "product_type", length = 80)
    private String productType;

    @Column(nullable = false)
    private Instant momento;

    public Long getId() { return id; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public int getWeight() { return weight; }
    public void setWeight(int weight) { this.weight = weight; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }
    public Instant getMomento() { return momento; }
    public void setMomento(Instant momento) { this.momento = momento; }
}
