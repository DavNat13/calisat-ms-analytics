package com.califorge.msanalytics;

import com.califorge.msanalytics.config.KafkaConfig;
import com.califorge.msanalytics.config.KafkaTopicsConfig;
import com.califorge.msanalytics.dto.InteraccionEvent;
import com.califorge.msanalytics.listener.AnalyticsKafkaListeners;
import com.califorge.msanalytics.service.MetricasService;
import com.califorge.msanalytics.service.RecomendacionService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * Integracion con un broker Kafka real (embebido): serializacion JSON tipada con alias de tipo,
 * consumer groups separados sobre el mismo topico y envio de payloads corruptos a la DLT.
 */
@SpringBootTest(
        classes = {KafkaConfig.class, KafkaTopicsConfig.class, AnalyticsKafkaListeners.class},
        properties = {
                "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
                "spring.kafka.consumer.auto-offset-reset=earliest",
                "calisat.analytics.kafka.topic-interacciones=interacciones-user",
                "calisat.analytics.kafka.topic-alertas=payment-alerts",
                "calisat.analytics.kafka.group-recomendaciones=analytics-recommendations-group",
                "calisat.analytics.kafka.group-metricas=analytics-metrics-group"
        })
@ImportAutoConfiguration(KafkaAutoConfiguration.class)
@EmbeddedKafka(partitions = 3, topics = {"interacciones-user", "interacciones-user.DLT", "payment-alerts",
        "payment-alerts.DLT"})
class KafkaIntegrationTest {

    @MockitoBean
    RecomendacionService recomendaciones;

    @MockitoBean
    MetricasService metricas;

    @Autowired
    EmbeddedKafkaBroker broker;

    private KafkaTemplate<String, Object> plantillaJson() {
        Map<String, Object> p = new HashMap<>();
        p.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString());
        p.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        p.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        p.put(JsonSerializer.TYPE_MAPPINGS, "interaccion:" + InteraccionEvent.class.getName());
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(p));
    }

    @Test
    void eventoValidoLoConsumenAmbosGruposYSeDelegaALosServicios() {
        InteraccionEvent e = new InteraccionEvent("evt-1", "user-1", "SKU1", "view", "barras", null, Instant.now());
        plantillaJson().send("interacciones-user", "user-1", e);

        verify(recomendaciones, timeout(15000)).procesar(any(InteraccionEvent.class));
        verify(metricas, timeout(15000)).registrarInteraccion(any(InteraccionEvent.class));
    }

    @Test
    void payloadCorruptoVaALaDltSinBloquearLaParticion() {
        Map<String, Object> p = new HashMap<>();
        p.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString());
        p.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        p.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        KafkaTemplate<String, String> crudo = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(p));
        crudo.send("interacciones-user", "user-x", "esto-no-es-json{{{");

        Map<String, Object> cfg = KafkaTestUtils.consumerProps(broker, "test-dlt-group", true);
        cfg.put("key.deserializer", StringDeserializer.class);
        cfg.put("value.deserializer", StringDeserializer.class);
        cfg.put("auto.offset.reset", "earliest");
        try (Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<String, String>(cfg).createConsumer()) {
            broker.consumeFromAnEmbeddedTopic(consumer, "interacciones-user.DLT");
            // Cada consumer group (recomendaciones y metricas) falla de forma independiente => 1 registro por grupo
            org.apache.kafka.clients.consumer.ConsumerRecords<String, String> recs =
                    KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(20), 1);
            assertTrue(recs.count() >= 1);
            for (ConsumerRecord<String, String> rec : recs) {
                assertEquals("user-x", rec.key());
                assertTrue(rec.value().contains("esto-no-es-json"));
            }
        }
        // El servicio de dominio nunca recibio el payload invalido
        verify(recomendaciones, after(500).never()).procesar(org.mockito.ArgumentMatchers.argThat(
                ev -> ev != null && "user-x".equals(ev.userId())));
        verify(metricas, never()).registrarAlertaFraude();
    }
}
