package com.califorge.msanalytics.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Topicos de este servicio (definicion explicita de particiones, replicas, retencion y cleanup).
 * {@code interacciones-user} tambien se declara aqui para que exista aunque arranque primero el consumidor;
 * KafkaAdmin ignora la creacion si ya existe con la misma configuracion.
 */
@Configuration
public class KafkaTopicsConfig {

    private static final String SIETE_DIAS_MS = "604800000";

    @Bean
    public NewTopic topicInteracciones(@Value("${calisat.analytics.kafka.topic-interacciones}") String nombre) {
        return TopicBuilder.name(nombre)
                .partitions(3).replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG, SIETE_DIAS_MS)
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
                .build();
    }

    @Bean
    public NewTopic topicInteraccionesDlt(@Value("${calisat.analytics.kafka.topic-interacciones}") String nombre) {
        return TopicBuilder.name(nombre + ".DLT").partitions(3).replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG, SIETE_DIAS_MS).build();
    }

    @Bean
    public NewTopic topicAlertasDlt(@Value("${calisat.analytics.kafka.topic-alertas}") String nombre) {
        return TopicBuilder.name(nombre + ".DLT").partitions(1).replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG, SIETE_DIAS_MS).build();
    }
}
