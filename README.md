# TrackFlow — Sistema de tracking logístico

OnRender: https://trackflow-mxll.onrender.com/swagger-ui/index.html

## Reporte de volumen de envíos

`GET /api/reports/shipment-volume?from=AAAA-MM-DD&to=AAAA-MM-DD`

Para el supervisor de operaciones: cuántos envíos se registraron en un periodo y por qué
punto de la red entraron. Requiere token de operador cuando la protección está activa.

- `from` y `to` son opcionales y se interpretan como días en hora de Colombia, ambos
  inclusive. Sin ellos, el periodo es el día en curso. Si el periodo incluye hoy, se
  cuenta hasta el momento de la consulta (`inProgress = true`).
- `totalShipments` son los envíos **registrados** en el periodo. `byPoint` los desglosa por
  el centro donde ingresaron a la red (el primer "Recibido en centro") y siempre suma el
  total. Los que aún no se reciben en ningún centro aparecen como pendientes de recepción
  en su ciudad de origen.
- `totalDelivered` es una cifra aparte: los envíos entregados en el periodo, aunque se
  hayan registrado antes.
- Un periodo sin envíos responde 200 con total 0 y un `message`. Un periodo invertido, que
  empieza en el futuro o con una fecha mal escrita responde 400.

## Reporte de tiempos de tránsito

`GET /api/reports/transit-times?from=AAAA-MM-DD&to=AAAA-MM-DD`

Para el supervisor de operaciones: cuánto tardan en recorrer la red los envíos
**entregados** en un periodo y en qué tramos se va el tiempo. Requiere token de operador
cuando la protección está activa.

- El periodo se interpreta igual que en el reporte de volumen (días en hora de Colombia,
  ambos inclusive, por defecto el día en curso, 400 si está invertido, en el futuro o mal
  escrito) y se filtra por la fecha de **entrega**.
- Solo cuentan los envíos ya entregados; los que siguen en tránsito se ignoran.
- `averageTotal` va desde que el envío entra a la red (primer "Recibido en centro") hasta
  que se entrega. `segments` lo desglosa en cuatro tramos que suman el total:
  `ORIGIN_PROCESSING` (recibido, despachado), `TRANSPORT` (despachado, llegada al centro
  destino), `DESTINATION_PROCESSING` (llegada, salida a reparto) y `LAST_MILE` (salida a
  reparto, entregado). Un transbordo por un hub intermedio queda dentro de `TRANSPORT`.
- Se usa la hora en que ocurrió cada movimiento, no la de su registro.
- Un periodo sin entregas responde 200 con `deliveredShipments = 0`, `segments` vacío y un
  `message` ("No hay datos para el periodo consultado...").
