package com.trackflow.modules.logistics.application;

import com.trackflow.modules.logistics.application.ValidadorDeRecorrido.LugarValidado;
import com.trackflow.modules.logistics.domain.FechaDeMovimientoInvalidaException;
import com.trackflow.modules.logistics.domain.LogisticsEvent;
import com.trackflow.modules.logistics.domain.TrackedShipment;
import com.trackflow.modules.logistics.domain.UnknownShipmentException;
import com.trackflow.shared.events.EventPublisher;
import com.trackflow.shared.events.EventoLogisticoRegistradoEvent;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registra el evento que llega por la cola. Es el único que escribe el historial, así
 * que es la última palabra sobre las reglas del recorrido: las vuelve a aplicar aunque
 * la admisión ya lo haya hecho, porque entre admitir y consumir el envío pudo avanzar, y
 * porque a la cola también publican puntos de la cadena que no pasan por la API.
 */
@Service
public class RegistrarEventoLogistico {

    private static final Logger log = LoggerFactory.getLogger(RegistrarEventoLogistico.class);

    private final LogisticsEventRepository logisticsEvents;
    private final TrackedShipmentRepository trackedShipments;
    private final ConsultarEstadoDelEnvio estadoDelEnvio;
    private final ValidadorDeRecorrido validador;
    private final EventPublisher events;
    private final Clock clock;

    public RegistrarEventoLogistico(LogisticsEventRepository logisticsEvents,
            TrackedShipmentRepository trackedShipments, ConsultarEstadoDelEnvio estadoDelEnvio,
            ValidadorDeRecorrido validador, EventPublisher events, Clock clock) {
        this.logisticsEvents = logisticsEvents;
        this.trackedShipments = trackedShipments;
        this.estadoDelEnvio = estadoDelEnvio;
        this.validador = validador;
        this.events = events;
        this.clock = clock;
    }

    /**
     * El broker entrega at-least-once, así que un mismo eventId puede llegar más de una vez;
     * la segunda se descarta en lugar de duplicar el movimiento en el historial.
     *
     * El envío se bloquea mientras dura la transacción: dos movimientos del mismo envío
     * se registran uno detrás del otro, y el segundo se valida contra un historial que
     * ya incluye al primero. Sin el bloqueo, los dos se validaban contra el mismo estado
     * y podían quedar, por ejemplo, dos despachos seguidos.
     *
     * El nombre del punto y la ciudad se toman del catálogo a partir del centro, no del
     * mensaje: un productor externo podría mandar un texto que no corresponde al centro.
     */
    @Transactional
    public Optional<LogisticsEvent> ejecutar(EventoLogisticoEntrante entrante) {
        if (logisticsEvents.existePorEventId(entrante.eventId())) {
            log.info("Evento {} ya registrado, se descarta la reentrega", entrante.eventId());
            return Optional.empty();
        }

        TrackedShipment envio = trackedShipments.porTrackingNumberParaActualizar(entrante.trackingNumber())
                .orElseThrow(() -> new UnknownShipmentException(entrante.trackingNumber()));

        Instant now = clock.instant();
        if (entrante.ocurridoEn() == null || entrante.ocurridoEn().isAfter(now)) {
            throw new FechaDeMovimientoInvalidaException(entrante.ocurridoEn(), now);
        }

        EstadoDelEnvio estado = estadoDelEnvio.desdeHistorial(envio,
                logisticsEvents.findHistorial(entrante.trackingNumber()));
        LugarValidado lugar = validador.validar(estado, entrante.tipo(), entrante.centroId(), entrante.ocurridoEn());

        LogisticsEvent saved = logisticsEvents.save(LogisticsEvent.registrar(
                new LogisticsEvent.Reporte(
                        entrante.eventId(),
                        entrante.trackingNumber(),
                        entrante.tipo(),
                        new LogisticsEvent.Lugar(lugar.centro().getName(), lugar.centro().getId(),
                                lugar.ciudad().id(), lugar.ciudad().etiqueta()),
                        entrante.observaciones(),
                        entrante.repartidorNombre(),
                        entrante.ocurridoEn()),
                now));

        events.publish(new EventoLogisticoRegistradoEvent(
                saved.getId(),
                saved.getTrackingNumber(),
                saved.getType().name(),
                saved.getType().resultingStatus(),
                saved.getPoint(),
                saved.getCenterId(),
                saved.getCityId(),
                saved.getCityName(),
                saved.getOccurredAt(),
                saved.getRegisteredAt(),
                now));

        return Optional.of(saved);
    }
}
