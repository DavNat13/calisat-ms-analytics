package com.califorge.msanalytics.ws;

import com.califorge.msanalytics.service.MetricasService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/** Mantiene las sesiones del dashboard y les envia el snapshot de metricas. */
public class MetricasWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(MetricasWebSocketHandler.class);

    private final Set<WebSocketSession> sesiones = new CopyOnWriteArraySet<>();
    private final MetricasService metricas;
    private final ObjectMapper mapper;

    public MetricasWebSocketHandler(MetricasService metricas, ObjectMapper mapper) {
        this.metricas = metricas;
        this.mapper = mapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        sesiones.add(session);
        log.info("Dashboard conectado ({} sesiones)", sesiones.size());
        enviar(session, serializarSnapshot());   // estado inicial inmediato
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sesiones.remove(session);
        log.info("Dashboard desconectado ({} sesiones)", sesiones.size());
    }

    public int sesionesActivas() {
        return sesiones.size();
    }

    public void difundir() {
        if (sesiones.isEmpty()) {
            return;
        }
        String json = serializarSnapshot();
        for (WebSocketSession s : sesiones) {
            try {
                enviar(s, json);
            } catch (IOException | RuntimeException e) {
                log.warn("No se pudo enviar metricas a la sesion {}: {}", s.getId(), e.getMessage());
                sesiones.remove(s);
            }
        }
    }

    private String serializarSnapshot() {
        try {
            return mapper.writeValueAsString(metricas.snapshot());
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo serializar el snapshot de metricas", e);
        }
    }

    private void enviar(WebSocketSession s, String json) throws IOException {
        synchronized (s) {
            if (s.isOpen()) {
                s.sendMessage(new TextMessage(json));
            }
        }
    }
}
