# Contrato de datos — Caso 4: Central / Repartidor (MQTT)

La información que viaja entre las aplicaciones es **JSON UTF-8** publicado con **QoS 1**.
El broker Mosquitto intermedia; ninguna app conoce a la otra directamente.

## 1. Tópicos

| Tópico | Publicador | Suscriptor | QoS |
|---|---|---|---|
| `despachos/puerto-montt/centro/nuevos` | Central | Repartidor de zona Centro | 1 |
| `despachos/puerto-montt/alerce/nuevos` | Central | Repartidor de zona Alerce | 1 |
| `despachos/puerto-montt/tepual/nuevos` | Central | Repartidor de zona Tepual | 1 |
| `despachos/{idDespacho}/estado` | Repartidor | Central (patrón `despachos/+/estado`) y otros repartidores | 1 |

Suscripciones usadas en el código:

- **Central:** `despachos/+/estado` — un `+` por cada id de despacho (RF-06).
- **Repartidor:** `despachos/puerto-montt/{zona}/nuevos` (RF-08) **y** `despachos/+/estado`
  para sincronizar estados aceptados por otros repartidores (soporte ERR-06 / FA-03).
  Esta segunda suscripción es una elección del equipo y está documentada aquí.

## 2. Payload: nuevo despacho

Tópico `despachos/puerto-montt/{zona}/nuevos` · Publicador: Central · Suscriptor: repartidor de esa zona.

```json
{
  "id": "DES-20260902-1030-412",
  "destino": "Av. Colón 123, Puerto Montt",
  "descripcion": "Paquete frágil, timbre 4B",
  "zona": "centro",
  "estado": "PENDIENTE",
  "publicadoEn": "2026-09-02T16:30:00-04:00"
}
```

| Campo | Tipo | Obligatorio |
|---|---|---|
| id | string (DES-…, generado por Central, editable) | Sí |
| destino | string | Sí |
| descripcion | string | Sí |
| zona | string (`centro` \| `alerce` \| `tepual`) | Sí |
| estado | string (inicial `PENDIENTE`) | Sí |
| publicadoEn | string ISO 8601 con zona horaria | Sí |

## 3. Payload: actualización de estado

Tópico `despachos/{idDespacho}/estado` · Publicador: Repartidor · Suscriptor: Central (y repartidores de la misma zona).

```json
{
  "id": "DES-20260902-1030-412",
  "estado": "ACEPTADO",
  "estadoAnterior": "PENDIENTE",
  "zona": "centro",
  "repartidorId": "REP-7F3A",
  "actualizadoEn": "2026-09-02T16:35:00-04:00"
}
```

| Campo | Tipo | Obligatorio |
|---|---|---|
| id | string | Sí (igual al despacho publicado) |
| estado | string (`ACEPTADO` \| `EN_CAMINO` \| `ENTREGADO`) | Sí |
| estadoAnterior | string | Sí |
| zona | string | Sí |
| repartidorId | string (`REP-xxxx`, estable por instalación) | Sí |
| actualizadoEn | string ISO 8601 | Sí |

## 4. Estados y transiciones

| Valor | Quién lo asigna | Siguiente permitido |
|---|---|---|
| PENDIENTE | Central (al publicar) | ACEPTADO |
| ACEPTADO | Repartidor | EN_CAMINO |
| EN_CAMINO | Repartidor | ENTREGADO |
| ENTREGADO | Repartidor | (final) |

La secuencia es obligatoria (RF-12): `Estado.siguiente()` valida la transición antes de
publicar; cualquier salto inválido se rechaza con un mensaje en la app.

## 5. Identificación de mensajes y reglas

- **Por tópico:** los mensajes que terminan en `/nuevos` son despachos nuevos; los que
  terminan en `/estado` son actualizaciones de estado (`GestorMqtt.procesarMensaje`).
- **Idempotencia:** `Repositorio.actualizarEstado` ignora el mensaje si el estado local ya
  es el mismo (el eco del broker o los reintentos de QoS 1 no duplican el historial).
  `Repositorio.agregar` ignora ids ya existentes.
- **Timestamps:** formato `yyyy-MM-dd'T'HH:mm:ssXXX` con zona horaria local
  (`Despacho.ahora()`); se muestran como `yyyy-MM-dd HH:mm`.
- **ERR-05:** payload JSON inválido se ignora y se registra en el log.

## 6. Ejemplo real de ida y vuelta (FP-01)

1. Central publica (`despachos/puerto-montt/centro/nuevos`):

```json
{"id":"DES-001","destino":"O'Higgins 456","descripcion":"Sobre documentos","zona":"centro","estado":"PENDIENTE","publicadoEn":"2026-09-02T16:30:00-04:00"}
```

2. Repartidor acepta (`despachos/DES-001/estado`):

```json
{"id":"DES-001","estado":"ACEPTADO","estadoAnterior":"PENDIENTE","zona":"centro","repartidorId":"REP-001","actualizadoEn":"2026-09-02T16:36:00-04:00"}
```

3. Repartidor confirma entrega (`despachos/DES-001/estado`):

```json
{"id":"DES-001","estado":"ENTREGADO","estadoAnterior":"EN_CAMINO","zona":"centro","repartidorId":"REP-001","actualizadoEn":"2026-09-02T17:00:00-04:00"}
```

4. La Central recibe cada mensaje en `despachos/+/estado` y refleja el color y el
   historial en el listado y el detalle.
