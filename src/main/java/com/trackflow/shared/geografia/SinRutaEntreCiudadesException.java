package com.trackflow.shared.geografia;

/**
 * Las dos ciudades tienen cobertura, pero la red de rutas no las conecta.
 */
public class SinRutaEntreCiudadesException extends RuntimeException {

    public SinRutaEntreCiudadesException(Ciudad origen, Ciudad destino) {
        super("La red no tiene una ruta de %s a %s, así que el envío no podría salir del origen"
                .formatted(origen.etiqueta(), destino.etiqueta()));
    }
}
