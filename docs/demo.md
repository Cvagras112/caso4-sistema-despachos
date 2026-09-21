# Demo y defensa — Caso 4: Central / Repartidor (MQTT)

> **Estado: demo ejecutada y verificada.** Los pasos 1-10 del flujo principal y los
> alternativos FA-01/FA-02/ERR-02 corrieron en este PC (emulador Android 34, broker
> Moquette incluido en `broker/`). Capturas: `docs/evidencia/*.png` · Transcripción
> del tráfico: `docs/evidencia/trafico-mqtt-espia.txt`. Durante la ejecución se detectó
> y corrigió un defecto (la Central no se suscribía a `despachos/+/estado`), que hoy
> está verificado a nivel broker (`SUBSCRIBE ... despachos/+/estado grantedQoses [1]`).
> Además, el botón de retroceso de Android lleva siempre al menú principal en ambas
> apps (verificado en las 4 rutas: listado y detalle de Central y Repartidor,
> capturas `12-atras-menu-principal-repartidor.png` y `12-navegacion-atras-central-menu.png`).

## Preparación (checklist previo)

- [ ] Mosquitto ejecutándose: `mosquitto -c mosquitto.conf -v` (listener 1883, `allow_anonymous true`)
- [ ] Puerto 1883 permitido en el firewall; IP del PC conocida (`ipconfig`)
- [ ] APK de Central instalado en dispositivo/emulador A (host = IP del PC, o `10.0.2.2` si ambos son emuladores)
- [ ] APK de Repartidor instalado en dispositivo/emulador B (mismo host)
- [ ] (Opcional) `mosquitto_sub -h 127.0.0.1 -t "despachos/#" -v` o MQTT Explorer para mostrar los tópicos en vivo
- [ ] Mosquitto detenido a propósito para la demo de ERR-01

## Flujo principal (FP-01 — despacho completado)

| # | Paso | Evidencia esperada |
|---|---|---|
| 1 | Central: Conectar | Indicador verde "Conectado al broker" (RF-17) |
| 2 | Repartidor: zona **Centro** + Conectar | Indicador verde; listado vacío |
| 3 | Central: Nuevo despacho → destino/descripción/zona Centro → Publicar | Toast "Despacho publicado…" y aparece en listado local como **Pendiente** (naranja) |
| 4 | Repartidor: mirar el listado **sin tocar nada** | Aparece el despacho + Toast "Nuevo despacho recibido" (RF-09, RF-16) |
| 5 | Repartidor: abrir detalle → **Aceptar despacho** | Toast "Estado actualizado: Aceptado"; chip azul |
| 6 | Central: observar listado | El despacho pasa a **Aceptado** (azul) sin refrescar |
| 7 | Repartidor: **Iniciar ruta** | Chip violeta; Central refleja **En camino** |
| 8 | Repartidor: **Confirmar entrega** | Chip verde; Central refleja **Entregado** (RF-12 ciclo completo) |
| 9 | Central: abrir detalle | Historial completo con horas y repartidorId |

## Flujos alternativos

- **FA-01 (zona equivocada):** Central publica despacho en zona **Alerce**; el repartidor suscrito a **centro** no lo recibe (mostrar con `mosquitto_sub` que el tópico `.../alerce/nuevos` sí recibió el mensaje).
- **FA-02 (ERR-01):** detener Mosquitto → pulsar Conectar → "No se pudo conectar al broker"; reiniciar broker y reintentar → conecta.
- **FA-03 (ERR-06):** dos repartidores en la misma zona; el repartidor B abre un despacho ya aceptado por A → solo ve "Aceptar" bloqueado/ausente y cualquier intento muestra **"Despacho no disponible (ERR-06)"**.
- **FA-04 (ERR-02):** en Nuevo despacho dejar el destino vacío → "Hay campos obligatorios vacíos"; no se publica nada.
- **FA-05 (ERR-04):** activar/desactivar WiFi/datos del repartidor → indicador "Conexión perdida. Reintentando automáticamente…"; al volver, re-suscripción automática y el listado local se conserva.
- **ERR-03:** en el Repartidor intentar conectar sin elegir zona → "Seleccione una zona de trabajo".

## Preguntas de defensa (con respuesta en una línea)

1. **¿Qué tópico publica la Central y quién se suscribe?** → `despachos/puerto-montt/{zona}/nuevos`; repartidores de esa zona.
2. **¿Por qué el Repartidor no recibe otra zona?** → solo está suscrito al tópico de su zona; el broker filtra.
3. **¿Qué es QoS 1 y por qué se usa?** → entrega al menos una vez (PUBACK); evita perder despachos, los duplicados se filtran por id.
4. **¿Cómo sabe la Central que cambió el estado?** → suscripción al patrón `despachos/+/estado` que le llega en tiempo real.
5. **¿Por qué MQTT y no Socket?** → broker desacoplado, tópicos por zona, QoS y reconexión estándar sin protocolo propio.
6. **¿Qué cambiaría con Firestore?** → sin broker: documentos + listeners en la nube; persistencia e historial en Firebase, no en memoria local.
7. **¿Qué pasa si el Repartidor está cerrado al publicar?** → nada viaja: con clean session no hay mensajes retenidos; al reconectar se re-suscribe y recibe los nuevos despachos posteriores.
8. **¿Dónde queda almacenada la información?** → en el repositorio en memoria de cada app (el broker no guarda mensajes); la config del broker en SharedPreferences.

## Criterio de éxito

Despacho publicado, recibido por la zona correcta, ciclo de estados completo visible en la
Central con colores y explicación de tópicos/payloads JSON durante la demostración.
