# calisat-ms-analytics

Analítica y recomendaciones en tiempo real — puerto **8090** · Java 21 · Spring Boot 4.1.0

## Endpoints (JWT Entra/Cognito)
```
GET /api/v1/recomendaciones/me · GET /api/v1/recomendaciones/{userId} · GET /api/v1/analytics/metricas (ADMIN)
WebSocket wss://host/ws/metricas?token=<JWT> (ADMIN): eventos/min, productos más vistos, eventos por acción, alertas de fraude
Pesos: view=1, cart=2, purchase=3; Top-5 por usuario, completado con productos populares de la misma categoría.
```

## Variables de entorno
SPRING_DATASOURCE_*, SPRING_KAFKA_BOOTSTRAP_SERVERS, SPRING_PROFILES_ACTIVE=dynamodb (historial en DynamoDB `UserInteractions`, +2), WS_ALLOWED_ORIGINS

## Ejecutar
```
./mvnw clean test
docker compose up -d --build   # requiere calisat-infra arriba (red calisat-net)
```
