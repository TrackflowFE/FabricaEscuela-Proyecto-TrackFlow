package com.trackflow.modules.shipments.api;

import com.trackflow.modules.shipments.domain.DocumentoInvalidoException;
import com.trackflow.shared.geografia.CiudadDesconocidaException;
import com.trackflow.shared.geografia.CiudadSinCoberturaException;
import com.trackflow.shared.geografia.SinRutaEntreCiudadesException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce las excepciones del dominio a respuestas HTTP. Vive en api/ para que el
 * dominio no tenga que conocer el protocolo.
 */
@RestControllerAdvice(assignableTypes = ShipmentController.class)
public class ShipmentsExceptionHandler {

    @ExceptionHandler(DocumentoInvalidoException.class)
    ProblemDetail documentoInvalido(DocumentoInvalidoException e) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        problema.setTitle("Documento de identidad inválido");
        return problema;
    }

    @ExceptionHandler(CiudadDesconocidaException.class)
    ProblemDetail ciudadDesconocida(CiudadDesconocidaException e) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        problema.setTitle("Ciudad no encontrada en el catálogo");
        return problema;
    }

    @ExceptionHandler(CiudadSinCoberturaException.class)
    ProblemDetail ciudadSinCobertura(CiudadSinCoberturaException e) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
        problema.setTitle("Ciudad sin cobertura");
        return problema;
    }

    @ExceptionHandler(SinRutaEntreCiudadesException.class)
    ProblemDetail sinRuta(SinRutaEntreCiudadesException e) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
        problema.setTitle("Sin ruta entre las ciudades");
        return problema;
    }
}
