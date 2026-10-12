package com.trackflow.modules.reports.domain;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * Cuánto tiempo puede estar un envío sin movimiento, según su estado, antes de
 * considerarse retrasado. Los estados que no aparecen (p. ej. DELIVERED) no se vigilan.
 */
public final class ReglaDeRetraso {

    private final Map<String, Duration> maximoSinMovimiento;

    public ReglaDeRetraso(Map<String, Duration> maximoSinMovimiento) {
        this.maximoSinMovimiento = Map.copyOf(maximoSinMovimiento);
    }

    public static ReglaDeRetraso porDefecto() {
        return new ReglaDeRetraso(Map.of(
                "REGISTERED", Duration.ofHours(24),
                "AT_DISTRIBUTION_CENTER", Duration.ofHours(24),
                "IN_TRANSIT", Duration.ofHours(48),
                "OUT_FOR_DELIVERY", Duration.ofHours(12)));
    }

    public Optional<Duration> maximoPara(String status) {
        return Optional.ofNullable(maximoSinMovimiento.get(status));
    }
}
