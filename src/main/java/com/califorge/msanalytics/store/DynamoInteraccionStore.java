package com.califorge.msanalytics.store;

import com.califorge.msanalytics.dto.InteraccionEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Almacen NoSQL en Amazon DynamoDB (perfil "dynamodb"). Tabla {@code UserInteractions}:
 * PK userId, SK "timestamp#eventId". Crear la tabla con aws-lambdas/README.md.
 */
@Component
@Profile("dynamodb")
public class DynamoInteraccionStore implements InteraccionStore {

    private final DynamoDbTable<InteraccionDynamoItem> table;

    public DynamoInteraccionStore(DynamoDbEnhancedClient client,
                                  @Value("${calisat.analytics.dynamodb.table}") String tableName) {
        this.table = client.table(tableName, TableSchema.fromBean(InteraccionDynamoItem.class));
    }

    @Override
    public boolean guardar(InteraccionEvent e) {
        InteraccionDynamoItem item = new InteraccionDynamoItem();
        item.setUserId(e.userId());
        item.setSk(e.timestamp().toEpochMilli() + "#" + e.eventId());
        item.setEventId(e.eventId());
        item.setItemId(e.itemId());
        item.setAction(e.action().toLowerCase());
        item.setWeight(e.weight());
        item.setCategory(e.category());
        item.setProductType(e.productType());
        item.setTimestamp(e.timestamp().toEpochMilli());
        try {
            table.putItem(PutItemEnhancedRequest.builder(InteraccionDynamoItem.class)
                    .item(item)
                    .conditionExpression(Expression.builder().expression("attribute_not_exists(sk)").build())
                    .build());
            return true;
        } catch (ConditionalCheckFailedException dup) {
            return false;
        }
    }

    @Override
    public List<InteraccionEvent> historialUsuario(String userId) {
        return table.query(QueryConditional.keyEqualTo(Key.builder().partitionValue(userId).build()))
                .items().stream()
                .map(i -> new InteraccionEvent(i.getEventId(), i.getUserId(), i.getItemId(), i.getAction(),
                        i.getCategory(), i.getProductType(), Instant.ofEpochMilli(i.getTimestamp())))
                .toList();
    }

    /** Scan con agregacion en memoria: aceptable para el volumen academico del proyecto. */
    @Override
    public List<ItemScore> popularesPorCategoria(String category, int limite) {
        if (category == null) {
            return List.of();
        }
        Map<String, Double> acumulado = new HashMap<>();
        table.scan().items().stream()
                .filter(i -> category.equals(i.getCategory()))
                .forEach(i -> acumulado.merge(i.getItemId(), (double) i.getWeight(), Double::sum));
        return acumulado.entrySet().stream()
                .map(en -> new ItemScore(en.getKey(), en.getValue()))
                .sorted(Comparator.comparingDouble(ItemScore::score).reversed())
                .limit(limite)
                .toList();
    }
}
