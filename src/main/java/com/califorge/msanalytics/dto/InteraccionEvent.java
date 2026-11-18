package com.califorge.msanalytics.dto;

import java.time.Instant;

/**
 * Evento de interaccion de un usuario con la tienda, publicado en {@code interacciones-user}
 * (key = userId) por ms-catalogo (view), ms-carrito (cart) y ms-orden (purchase).
 */
public record InteraccionEvent(
        String eventId,
        String userId,
        String itemId,
        String action,
        String category,
        String productType,
        Instant timestamp) {

    public static final String VIEW = "view";
    public static final String CART = "cart";
    public static final String PURCHASE = "purchase";

    /** Peso de la accion para el ranking: view=1, cart=2, purchase=3. */
    public int weight() {
        return switch (action == null ? "" : action.toLowerCase()) {
            case PURCHASE -> 3;
            case CART -> 2;
            default -> 1;
        };
    }
}
