package com.trackflow.shared.geografia;

import java.util.Set;

/**
 * Puerto de consulta de la cobertura: en qué ciudades la red puede recibir y entregar
 * un envío.
 *
 * Una ciudad del catálogo no basta para aceptar un envío: si no tiene ningún centro
 * activo, el paquete nunca podrá recibirse en origen ni llegar a destino, y queda
 * muerto desde el registro. Vive en shared, junto al catálogo, porque lo consulta
 * shipments; lo implementa logistics, que es el dueño de los centros. Así ningún módulo
 * importa a otro, que es el mismo criterio de los eventos de integración.
 */
public interface CoberturaLogistica {

    boolean tieneCobertura(Long ciudadId);

    /** Ids de las ciudades con al menos un centro activo. */
    Set<Long> ciudadesConCobertura();

    /** Exige cobertura o falla: es la forma en que los casos de uso la piden. */
    default void exigir(Ciudad ciudad) {
        if (!tieneCobertura(ciudad.id())) {
            throw new CiudadSinCoberturaException(ciudad);
        }
    }
}
