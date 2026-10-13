package com.trackflow.modules.logistics.infrastructure;

import com.trackflow.shared.geografia.CoberturaLogistica;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Una ciudad tiene cobertura si tiene al menos un centro activo. Desactivar el último
 * centro de una ciudad la deja sin cobertura de inmediato, sin tocar el catálogo.
 */
@Component
class CoberturaPorCentrosActivos implements CoberturaLogistica {

    private final CentroJpaRepository centros;

    CoberturaPorCentrosActivos(CentroJpaRepository centros) {
        this.centros = centros;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean tieneCobertura(Long ciudadId) {
        return ciudadId != null && centros.existsByCityIdAndActiveTrue(ciudadId);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> ciudadesConCobertura() {
        return centros.ciudadesConCentroActivo();
    }
}
