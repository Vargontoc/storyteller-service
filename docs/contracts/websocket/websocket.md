# Contrato WebSocket: eventos de generación de portada, página y narración

<!-- Origen: specs/agent-writer (REQ-014, REQ-020, REQ-022, DEC-003), specs/agent-narrator (REQ-027, REQ-028, REQ-029, DEC-004) -->
<!-- Estado del contrato: contrastado con el código -->

## Canales

- **Endpoint WebSocket:** `/storyteller` (registrado en `WebsocketConfig`, manejador `WebsocketAdapter` con `TextWebSocketHandler` y difusión en texto plano JSON a todas las sesiones conectadas).
- **Dirección de los eventos:** `storyteller-server` -> `storyteller-app`.
- **Nota de convivencia:** El mismo canal `/storyteller` difunde también `PortraitGenerationEvent` (retratos) y `WebsocketNotificationEvent` (notificaciones generales). Si el cliente envía un mensaje de texto, `WebsocketAdapter` responde `"Echo <payload>"`.

## Mensajes: `StoryPageGenerationEvent`

Publicados por `StoryPageNotifier` a través de `WebsocketStoryPageEventPublisher` y `WebsocketAdapter.sendStoryPageNotification`. Identifican la portada con `pageNumber = 0` y las páginas interiores con `pageNumber >= 1`.

### Estructura del payload

| Campo | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `type` | `StoryPageEventType` (string) | Sí | Tipo de evento: `STARTED`, `VERSION_PRESENTED`, `HINT_REJECTED` o `FAILED` |
| `storyId` | `integer` (`int64`) | Sí | Identificador del cuento afectado |
| `pageNumber` | `integer` (`int32`) | Sí | `0` para la portada; `>= 1` para página interior |
| `reason` | `string` | Solo en `HINT_REJECTED` | Motivo dado por el escritor cuando rechaza el hint. Se omite en el JSON cuando es `null` (`@JsonInclude(NON_NULL)`) |

### Tipos de evento (`StoryPageEventType`)

1. **`STARTED`** (`storyteller-server` -> `storyteller-app`)
   Se emite al concederse el turno en `generation-gate` y lanzarse la tarea asíncrona (generación inicial de portada o página, cambio con hint, reapertura o regeneración sin hint).
   ```json
   {
     "type": "STARTED",
     "storyId": 12,
     "pageNumber": 0
   }
   ```

2. **`VERSION_PRESENTED`** (`storyteller-server` -> `storyteller-app`)
   Se emite tras liberar `generation-gate` cuando la nueva versión de portada o página queda persistida (incluye el caso en que se agotan los 3 intentos por longitud y se guardan las 3 versiones con aviso). La app recarga el estado desde la API REST.
   ```json
   {
     "type": "VERSION_PRESENTED",
     "storyId": 12,
     "pageNumber": 3
   }
   ```

3. **`HINT_REJECTED`** (`storyteller-server` -> `storyteller-app`)
   Se emite tras liberar `generation-gate` cuando el escritor rechaza el hint por salirse del beat o incumplir las reglas de edad. No añade versión ni altera la vigente.
   ```json
   {
     "type": "HINT_REJECTED",
     "storyId": 12,
     "pageNumber": 3,
     "reason": "El lobo no aparece en el beat de esta página."
   }
   ```

4. **`FAILED`** (`storyteller-server` -> `storyteller-app`)
   Se emite tras liberar `generation-gate` si falla el modelo del escritor o devuelve una respuesta incompleta (también si ocurre en una regeneración automática por longitud). No guarda ninguna versión ni altera el estado previo.
   ```json
   {
     "type": "FAILED",
     "storyId": 12,
     "pageNumber": 3
   }
   ```

## Errores

- El rechazo de solicitudes por puerta ocupada (`REQ-032`), hint vacío (`REQ-013`), secuencia inválida (`REQ-002`, `REQ-010`) o estado inválido no se notifica por WebSocket: se devuelve síncronamente en la respuesta HTTP (`400` / `409`).
- Si falla la difusión por WebSocket (`IOException` al escribir en la sesión), `WebsocketAdapter` lanza `AppException` con estado `500 INTERNAL_SERVER_ERROR` (`"Could not sent notification"`).
- Si el servidor se reinicia a mitad de una generación de portada o página, no se emite `FAILED`; la app recarga el estado desde la API REST al reconectar el WebSocket.

## Mensajes: `NarrationEvent`

Publicados por `NarrationNotifier` a través de `WebsocketNarrationEventPublisher` y `WebsocketAdapter.sendNarrationNotification`. Informan del ciclo de vida de la narración asíncrona de un cuento con escaleta.

### Estructura del payload

| Campo | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `type` | `NarrationEventType` (string) | Sí | Tipo de evento: `STARTED`, `COMPLETED` o `FAILED` |
| `storyId` | `integer` (`int64`) | Sí | Identificador del cuento afectado |
| `voiceName` | `string` | Solo en `COMPLETED` | Copia del nombre de la voz usada al publicar la narración. Se omite en el JSON cuando es `null` (`@JsonInclude(NON_NULL)`) |

### Tipos de evento (`NarrationEventType`)

1. **`STARTED`** (`storyteller-server` -> `storyteller-app`)
   Se emite tras concederse el turno en `generation-gate`, descartar el audio y la voz anteriores (si existían) y marcar la narración `IN_PROGRESS`, justo antes de lanzar la ejecución en segundo plano.
   ```json
   {
     "type": "STARTED",
     "storyId": 12
   }
   ```

2. **`COMPLETED`** (`storyteller-server` -> `storyteller-app`)
   Se emite tras publicar atómicamente los audios de todas las páginas interiores y la copia de `voiceName`, y marcar la narración `COMPLETED` (antes de liberar `generation-gate`, en bloque `finally`).
   ```json
   {
     "type": "COMPLETED",
     "storyId": 12,
     "voiceName": "Abuela"
   }
   ```

3. **`FAILED`** (`storyteller-server` -> `storyteller-app`)
   Se emite cuando la narración falla (error del narrador, guion que altera las palabras detectado por `AudioScriptValidator`, fallo en Chatterbox en cualquier página o fallo al lanzar la tarea) tras dejar el cuento sin audio ni voz (`NarrationStatus.ERROR`). También lo emite `NarrationReconciliationService` al arrancar el servidor para las narraciones que quedaron en curso (`IN_PROGRESS`) antes de un reinicio.
   ```json
   {
     "type": "FAILED",
     "storyId": 12
   }
   ```

### Errores de narración

- Los rechazos síncronos de inicio de narración (sin escaleta, páginas sin aceptar, narración en curso, sin voces en español, voz no disponible o puerta ocupada) no se notifican por WebSocket: se devuelven por HTTP (`400` / `409` con `NarrationRejectionReason` en `errors[0]`).
- Durante la ejecución asíncrona o la reconciliación al arrancar, los fallos al difundir por WebSocket se registran como aviso en el log sin impedir que se marque el error ni que se libere el turno de `generation-gate`.
