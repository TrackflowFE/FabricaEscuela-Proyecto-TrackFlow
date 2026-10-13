package com.trackflow.bootstrap;

import com.trackflow.shared.geografia.CatalogoDeCiudades;
import com.trackflow.shared.geografia.Ciudad;
import com.trackflow.shared.geografia.CiudadDesconocidaException;
import com.trackflow.shared.geografia.CoberturaLogistica;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Alimenta el autocompletado de ciudad al registrar un envío: se escribe parte del
 * nombre y se escoge de la lista, en lugar de teclearlo y que "Bogotá" y "Bogota
 * D.C." acaben siendo dos ciudades.
 *
 * Cada ciudad dice si tiene cobertura (algún centro activo). Con onlyCovered=true la
 * lista ofrece solo las ciudades que el registro de envíos va a aceptar, así la
 * interfaz no propone una opción que el backend rechace.
 *
 * Vive en bootstrap por la misma razón que la reconstrucción de proyecciones: el
 * catálogo es dato de referencia transversal y ningún módulo de negocio es su dueño.
 */
@RestController
@RequestMapping("/api/cities")
public class CatalogoCiudadesController {

    private static final int LIMITE = 10;

    /**
     * Con el filtro de cobertura se piden más candidatos de los que se devuelven, para
     * que descartar los que no tienen cobertura no deje la lista corta. El catálogo
     * tiene unas decenas de municipios, así que filtrar en memoria es suficiente.
     */
    private static final int CANDIDATOS_CON_FILTRO = 100;

    private final CatalogoDeCiudades ciudades;
    private final CoberturaLogistica cobertura;

    public CatalogoCiudadesController(CatalogoDeCiudades ciudades, CoberturaLogistica cobertura) {
        this.ciudades = ciudades;
        this.cobertura = cobertura;
    }

    public record CiudadResponse(Long id, String name, String department, String label, boolean covered) {

        static CiudadResponse from(Ciudad ciudad, boolean cubierta) {
            return new CiudadResponse(ciudad.id(), ciudad.nombre(), ciudad.departamento(), ciudad.etiqueta(),
                    cubierta);
        }
    }

    @GetMapping
    public List<CiudadResponse> buscar(
            @RequestParam("q") @NotBlank(message = "indique el texto a buscar") String q,
            @RequestParam(value = "onlyCovered", defaultValue = "false") boolean soloConCobertura) {
        Set<Long> cubiertas = cobertura.ciudadesConCobertura();
        return ciudades.buscar(q, soloConCobertura ? CANDIDATOS_CON_FILTRO : LIMITE).stream()
                .filter(ciudad -> !soloConCobertura || cubiertas.contains(ciudad.id()))
                .limit(LIMITE)
                .map(ciudad -> CiudadResponse.from(ciudad, cubiertas.contains(ciudad.id())))
                .toList();
    }

    @GetMapping("/{id}")
    public CiudadResponse detalle(@PathVariable Long id) {
        Ciudad ciudad = ciudades.exigir(id);
        return CiudadResponse.from(ciudad, cobertura.tieneCobertura(ciudad.id()));
    }

    @ExceptionHandler(CiudadDesconocidaException.class)
    ProblemDetail ciudadDesconocida(CiudadDesconocidaException e) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        problema.setTitle("Ciudad no encontrada en el catálogo");
        return problema;
    }
}
