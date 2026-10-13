package com.trackflow.modules.logistics.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;

/**
 * Calcula la ruta más corta entre dos ciudades sobre los tramos de la red (algoritmo de
 * Dijkstra, ponderado por horas).
 *
 * Es lo que le da sentido geográfico a los hubs intermedios. Antes, un envío Medellín →
 * Bogotá podía "pasar" por Cartagena porque la única regla era que el hub no fuera la
 * ciudad de destino; ahora el hub debe ser la siguiente ciudad de esta ruta.
 *
 * No conoce la base de datos: recibe los tramos y devuelve la ruta, así que la regla
 * se puede razonar y probar sin infraestructura.
 */
public final class PlanificadorDeRuta {

    /** Ciudad por visitar y las horas acumuladas para llegar a ella. */
    private record Pendiente(Long ciudad, int horas) {
    }

    /** Ante dos caminos igual de largos gana la ciudad de id menor, para que el resultado sea estable. */
    private static final Comparator<Pendiente> MAS_CERCANA_PRIMERO = Comparator
            .comparingInt(Pendiente::horas)
            .thenComparing(Pendiente::ciudad);

    private PlanificadorDeRuta() {
    }

    /**
     * @return la ruta de menor duración, o vacío si la red no conecta las dos ciudades
     */
    public static Optional<RutaPlaneada> planificar(Collection<Tramo> tramos, Long desde, Long hasta) {
        if (desde == null || hasta == null) {
            return Optional.empty();
        }
        if (desde.equals(hasta)) {
            return Optional.of(RutaPlaneada.enDestino(desde));
        }

        Map<Long, List<Tramo>> salidas = new HashMap<>();
        for (Tramo tramo : tramos) {
            salidas.computeIfAbsent(tramo.desdeCiudadId(), id -> new ArrayList<>()).add(tramo);
        }

        Map<Long, Integer> horasHasta = new HashMap<>();
        Map<Long, Long> anterior = new HashMap<>();
        PriorityQueue<Pendiente> pendientes = new PriorityQueue<>(MAS_CERCANA_PRIMERO);

        horasHasta.put(desde, 0);
        pendientes.add(new Pendiente(desde, 0));

        while (!pendientes.isEmpty()) {
            Pendiente actual = pendientes.poll();

            // Una entrada vieja de la cola: ya se encontró un camino más corto a esta ciudad.
            if (actual.horas() > horasHasta.get(actual.ciudad())) {
                continue;
            }
            if (actual.ciudad().equals(hasta)) {
                return Optional.of(new RutaPlaneada(reconstruir(anterior, desde, hasta), actual.horas()));
            }

            for (Tramo tramo : salidas.getOrDefault(actual.ciudad(), List.of())) {
                int horas = actual.horas() + tramo.horas();
                Integer conocidas = horasHasta.get(tramo.hastaCiudadId());
                if (conocidas == null || horas < conocidas) {
                    horasHasta.put(tramo.hastaCiudadId(), horas);
                    anterior.put(tramo.hastaCiudadId(), actual.ciudad());
                    pendientes.add(new Pendiente(tramo.hastaCiudadId(), horas));
                }
            }
        }

        return Optional.empty();
    }

    private static List<Long> reconstruir(Map<Long, Long> anterior, Long desde, Long hasta) {
        List<Long> ciudades = new ArrayList<>();
        Long ciudad = hasta;
        while (!ciudad.equals(desde)) {
            ciudades.add(ciudad);
            ciudad = anterior.get(ciudad);
        }
        ciudades.add(desde);
        Collections.reverse(ciudades);
        return ciudades;
    }
}
