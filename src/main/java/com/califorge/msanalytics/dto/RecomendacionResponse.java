package com.califorge.msanalytics.dto;

import com.califorge.msanalytics.model.Recomendacion;

public record RecomendacionResponse(String itemId, double score, int posicion, String category, String productType) {

    public static RecomendacionResponse de(Recomendacion r) {
        return new RecomendacionResponse(r.getItemId(), r.getScore(), r.getRankPosition(),
                r.getCategory(), r.getProductType());
    }
}
