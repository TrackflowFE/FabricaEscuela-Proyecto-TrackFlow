package com.trackflow.modules.reports.api.dto;

import com.trackflow.modules.reports.application.EnviosRetrasados;
import java.time.Instant;
import java.util.List;

public record EnviosRetrasadosResponse(Instant corte, int totalRetrasados, String mensaje, List<Item> envios) {

    public record Item(String trackingNumber, String estado, String ultimoPunto, String ciudadOrigen,
            String ciudadDestino, long horasDetenido, long horasEsperadas, long horasDeExceso, String severidad) {
    }

    public static EnviosRetrasadosResponse from(EnviosRetrasados reporte) {
        List<Item> items = reporte.envios().stream()
                .map(e -> new Item(e.trackingNumber(), e.estado(),
                        e.ultimoPunto() == null ? "Sin movimientos" : e.ultimoPunto(),
                        e.ciudadOrigen(), e.ciudadDestino(),
                        e.detenido().toHours(), e.maximoEsperado().toHours(), e.exceso().toHours(),
                        e.severidad().name()))
                .toList();

        String mensaje = reporte.hayDatos() ? null : "No hay envíos retrasados en este momento.";
        return new EnviosRetrasadosResponse(reporte.corte(), items.size(), mensaje, items);
    }
}
