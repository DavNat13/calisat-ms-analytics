package com.califorge.msanalytics.listener;

import com.califorge.msanalytics.dto.AlertaFraudeEvent;
import com.califorge.msanalytics.dto.InteraccionEvent;
import com.califorge.msanalytics.service.MetricasService;
import com.califorge.msanalytics.service.RecomendacionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Consumidores Kafka. El MISMO topico {@code interacciones-user} lo leen dos consumer groups
 * independientes: uno persiste y recalcula recomendaciones; otro alimenta las metricas en tiempo real.
 * Los listeners solo delegan en la capa de servicio.
 */
@Component
public class AnalyticsKafkaListeners {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsKafkaListeners.class);

    private final RecomendacionService recomendaciones;
    private final MetricasService metricas;

    public AnalyticsKafkaListeners(RecomendacionService recomendaciones, MetricasService metricas) {
        this.recomendaciones = recomendaciones;
        this.metricas = metricas;
    }

    @KafkaListener(topics = "${calisat.analytics.kafka.topic-interacciones}",
            groupId = "${calisat.analytics.kafka.group-recomendaciones}",
            containerFactory = "kafkaListenerContainerFactory")
    public void persistirYRecomendar(@Payload(required = false) InteraccionEvent evento,
                                     @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                     @Header(KafkaHeaders.OFFSET) long offset,
                                     @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key) {
        log.debug("interacciones-user (reco) p={} o={} key={}", partition, offset, key);
        recomendaciones.procesar(evento);
    }

    @KafkaListener(topics = "${calisat.analytics.kafka.topic-interacciones}",
            groupId = "${calisat.analytics.kafka.group-metricas}",
            containerFactory = "kafkaListenerContainerFactory")
    public void actualizarMetricas(@Payload(required = false) InteraccionEvent evento) {
        if (evento == null || evento.action() == null || evento.itemId() == null) {
            throw new IllegalArgumentException("InteraccionEvent invalido para metricas");
        }
        metricas.registrarInteraccion(evento);
    }

    @KafkaListener(topics = "${calisat.analytics.kafka.topic-alertas}",
            groupId = "${calisat.analytics.kafka.group-metricas}",
            containerFactory = "kafkaListenerContainerFactory")
    public void alertaFraude(@Payload(required = false) AlertaFraudeEvent evento) {
        if (evento == null) {
            throw new IllegalArgumentException("AlertaFraudeEvent nulo");
        }
        log.warn("Alerta de fraude recibida: tarjeta ****{} intentos={}", evento.tarjetaUltimos4(), evento.intentos());
        metricas.registrarAlertaFraude();
    }
}
