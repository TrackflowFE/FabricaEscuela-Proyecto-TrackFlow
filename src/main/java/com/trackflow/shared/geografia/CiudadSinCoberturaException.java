package com.trackflow.shared.geografia;

/**
 * La ciudad existe en el catálogo pero la red no tiene ningún centro activo en ella.
 */
public class CiudadSinCoberturaException extends RuntimeException {

    private final Long id;

    public CiudadSinCoberturaException(Ciudad ciudad) {
        super(("No hay cobertura en %s: no tiene ningún centro activo donde recibir o entregar el envío. "
                + "Consulte GET /api/cities?q=&onlyCovered=true para ver las ciudades con servicio")
                .formatted(ciudad.etiqueta()));
        this.id = ciudad.id();
    }

    public Long getId() {
        return id;
    }
}
