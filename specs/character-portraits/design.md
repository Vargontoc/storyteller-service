# Diseño: character-portraits

Estado: aprobado
Revisión: 2
Basado en: requirements@2

## Resumen

Se añade un generador de retratos cartoon que orquesta 4 candidatos por personaje desde ComfyUI, notifica el progreso por WebSocket, bloquea al personaje durante la generación y gestiona la aprobación, invalidación y fork según el uso en cuentos. El puerto `character-image-condition-port` deja de ser una interfaz vacía y se implementa mediante el nuevo adaptador de retratos.

## Supuestos sobre el sistema actual

- El catálogo `CatalogCharacter` y la instantánea `StoryActorSnapshot` existen como resultado de `character-inventory`.
- `character-image-condition-port` está definido como interfaz pero sin implementación; esta feature lo materializa.
- ComfyUI ya es accesible desde el backend (`ComfyUIClient`, `ComfyUIAdapter`) y acepta workflows por HTTP.
- El canal WebSocket `/storyteller` ya difunde notificaciones genéricas; aquí se enriquece con eventos de retrato.
- El almacenamiento de assets sigue el patrón `stories/` para cuentos; los retratos del catálogo residirán en una ruta propia, por ejemplo `characters/{characterId}/`.

## Componentes

| Componente | Responsabilidad | Existente o nuevo |
|---|---|---|
| portrait-generation-service | Orquesta solicitud, seguimiento y finalización de la generación de 4 candidatos | nuevo |
| portrait-comfyui-adapter | Construye y envía el workflow de 4 retratos cartoon a ComfyUI; recibe los 4 assets | nuevo |
| portrait-candidate-store | Guarda temporalmente las 4 propuestas generadas hasta la selección | nuevo |
| portrait-selection-service | Registra el retrato aprobado, descarta las otras propuestas y actualiza el catálogo | nuevo |
| portrait-lock-service | Bloquea y libera un personaje durante la generación de retratos | nuevo |
| portrait-fork-service | Crea un `CatalogCharacter` derivado al confirmar un cambio visual en personaje usado en cuentos | nuevo |
| portrait-condition-adapter | Implementación real de `character-image-condition-port`: consulta retrato aprobado e imágenes generadas | nuevo |
| portrait-websocket-notifier | Emite eventos de inicio, progreso, éxito y error de generación de retratos | nuevo |
| portrait-generation-state-store | Persiste el estado del trabajo de generación de retratos (`RUNNING`, `COMPLETED`, `FAILED`) | nuevo |
| portrait-reconciliation-service | Al arrancar la aplicación, reconcilia los trabajos en vuelo, libera sus locks y emite el evento de error por WebSocket | nuevo |
| character-catalog-service | Añade consulta de candidatos, selección de retrato, estado de generación y fork | existente (modificado) |
| character-review-service | Añade invalidación de retrato y creación de fork ante cambio visual confirmado | existente (modificado) |
| character-repository | Persiste retrato aprobado, candidatos, jobs de generación, locks derivados y relación fork-original | existente (modificado) |
| comfyui-client | Cliente HTTP/WebSocket de ComfyUI reutilizado para encolar workflows y descargar imágenes | existente |
| websocket-adapter | Difusor de notificaciones WebSocket existente; recibe eventos de retrato | existente |
| character-catalog-view | Añade UI de generación, galería de 4 candidatos, indicador de progreso y selección | existente (modificado) |
| character-review-panel | Añade aviso de consistencia visual, aviso de fork y confirmación de fork | existente (modificado) |

## Flujo principal

1. El usuario pulsa "Generar retratos" en la ficha de un `CatalogCharacter` desde `character-catalog-view`.
2. `character-catalog-service` solicita a `portrait-generation-service` la generación de retratos.
3. `portrait-generation-service` crea un trabajo en estado `RUNNING` en `portrait-generation-state-store`.
4. `portrait-lock-service` deriva el bloqueo de la existencia del trabajo en curso y rechaza cualquier otra modificación, revisión o uso en cuento nuevo.
5. `portrait-websocket-notifier` envía un evento de inicio de generación por `/storyteller`.
6. `portrait-generation-service` pide a `portrait-comfyui-adapter` que genere 4 candidatos a partir de la descripción visual del personaje.
7. `portrait-comfyui-adapter` construye el workflow cartoon, lo encola en ComfyUI mediante `comfyui-client` y descarga los 4 resultados.
8. `portrait-candidate-store` persiste las 4 propuestas y las asocia al personaje.
9. `portrait-generation-state-store` marca el trabajo como `COMPLETED`.
10. `portrait-lock-service` libera el lock al dejar de existir un trabajo en curso.
11. `portrait-websocket-notifier` envía un evento de finalización con éxito, identificador del personaje y referencia a los candidatos.
12. El usuario selecciona un candidato en `character-catalog-view`.
13. `portrait-selection-service` marca el elegido como retrato aprobado, elimina las otras 3 propuestas de `portrait-candidate-store` y actualiza `character-repository`.

## Flujos alternativos y de fallo

### ALT-001 Regeneración de propuestas

- **Condición:** El usuario tiene 4 candidatos no seleccionados y solicita regenerar.
- **Comportamiento:** `portrait-generation-service` repite el flujo principal, reemplazando las propuestas previas por las nuevas.
- **Responsable:** portrait-generation-service
- **Resultado:** Las propuestas anteriores se descartan y se generan 4 nuevas.
- **Cubre:** REQ-004

### ALT-002 Fallo en la generación

- **Condición:** ComfyUI devuelve un error, un resultado incompleto o no responde.
- **Comportamiento:** `portrait-generation-service` marca el trabajo como `FAILED` en `portrait-generation-state-store`, `portrait-lock-service` libera el lock al desaparecer el trabajo en curso, `portrait-websocket-notifier` notifica el fallo con motivo y deja el personaje sin propuestas.
- **Responsable:** portrait-generation-service
- **Resultado:** Evento de error con motivo; el usuario puede volver a intentarlo.
- **Cubre:** REQ-005, REQ-007

### ALT-003 Intento de modificación durante generación

- **Condición:** Durante el bloqueo, el usuario intenta editar el nombre, solicitar una revisión o usar el personaje en un cuento nuevo.
- **Comportamiento:** `portrait-lock-service` o `character-catalog-service` rechaza la operación y `character-catalog-view` muestra el indicador de generación en curso.
- **Responsable:** portrait-lock-service
- **Resultado:** Operación rechazada; la ficha sigue consultable.
- **Cubre:** REQ-006

### ALT-004 Cierre de pestaña o caída del servidor durante la generación

- **Condición:** El usuario cierra el navegador o el servidor se interrumpe mientras la generación está en curso.
- **Comportamiento:** El trabajo persistente en `portrait-generation-state-store` sobrevive a la caída. Si el servidor se recupera antes de que termine ComfyUI, la generación continúa y se emite el evento correspondiente. Si el servidor arranca y detecta trabajos `RUNNING` huérfanos (sin tarea viva asociada), `portrait-reconciliation-service` los marca como `FAILED`, libera el lock y emite el evento de error por `/storyteller`.
- **Responsable:** portrait-reconciliation-service
- **Resultado:** El usuario no queda bloqueado indefinidamente; al reconectar recibe el evento de error o de finalización según corresponda.
- **Cubre:** REQ-005, REQ-006, REQ-007

### ALT-005 Selección con candidatos obsoletos

- **Condición:** El usuario tiene una galería de candidatos abiertos y, en paralelo, otra sesión regenera o invalida el retrato.
- **Comportamiento:** `portrait-selection-service` valida que los candidatos existan y no hayan sido invalidados; si no, rechaza la selección.
- **Responsable:** portrait-selection-service
- **Resultado:** Solo se puede aprobar un candidato válido y vigente.
- **Cubre:** REQ-003

### ALT-006 Cambio visual en personaje no usado

- **Condición:** El usuario confirma una revisión que modifica la descripción visual de un `CatalogCharacter` sin `StoryActorSnapshot` asociada.
- **Comportamiento:** `character-review-service` invalida el retrato aprobado y marca las imágenes generadas como obsoletas; el personaje vuelve a estado borrador.
- **Responsable:** character-review-service
- **Resultado:** Retrato aprobado eliminado; ficha sin retrato.
- **Cubre:** REQ-010

### ALT-007 Cambio visual en personaje usado en cuentos

- **Condición:** El usuario confirma una revisión visual de un personaje que tiene al menos una `StoryActorSnapshot`.
- **Comportamiento:** `character-review-service` muestra un aviso de fork. Si el usuario confirma, `portrait-fork-service` crea un nuevo `CatalogCharacter` con nombre sufijado, copia la ficha, aplica el cambio visual e invalida el retrato del fork. El original permanece inalterable.
- **Responsable:** portrait-fork-service
- **Resultado:** Nuevo personaje borrador en el catálogo; original conservado.
- **Cubre:** REQ-011, REQ-012

## Interfaces entre componentes

| Origen | Destino | Qué se comunica | Protocolo |
|---|---|---|---|
| app | character-catalog-service | Solicitud de generación, regeneración, selección de retrato y consulta de estado/candidatos | openapi |
| character-catalog-service | portrait-generation-service | Orden de generar o regenerar 4 candidatos | - |
| character-catalog-service | portrait-selection-service | Orden de aprobar un candidato | - |
| portrait-generation-service | portrait-comfyui-adapter | Petición de workflow de 4 retratos con descripción visual | - |
| portrait-comfyui-adapter | comfyui-client | Encolado de workflow y descarga de imágenes | http |
| portrait-generation-service | portrait-candidate-store | Lectura/escritura de candidatos | - |
| portrait-generation-service | portrait-generation-state-store | Creación y actualización del trabajo de generación | - |
| portrait-generation-service | portrait-lock-service | Bloqueo/liberación del personaje derivado del estado del trabajo | - |
| portrait-generation-service | portrait-websocket-notifier | Eventos de inicio, éxito y error | - |
| portrait-reconciliation-service | portrait-generation-state-store | Lectura de jobs `RUNNING` y cambio a `FAILED` | - |
| portrait-reconciliation-service | portrait-lock-service | Liberación de locks huérfanos | - |
| portrait-reconciliation-service | portrait-websocket-notifier | Emisión de eventos de error tras reconciliación | - |
| portrait-generation-service | character-repository | Lectura/actualización del personaje y retrato aprobado | - |
| character-review-service | portrait-fork-service | Orden de crear fork ante cambio visual en personaje usado | - |
| character-review-service | portrait-condition-adapter | Consulta de retrato aprobado e imágenes generadas | - |
| portrait-fork-service | character-repository | Alta del personaje fork | - |
| app | websocket-adapter | Suscripción a `/storyteller` y reconexión | websocket |
| websocket-adapter | app | Eventos de retrato con personajeId y estado | websocket |

## Modelo de datos lógico

- `CatalogCharacter`: personaje maestro del catálogo. Relaciones: tiene cero o un `ApprovedPortrait`; tiene cero o varias `PortraitCandidate` (solo durante generación no aprobada); es referenciado por cero o varias `StoryActorSnapshot`; puede ser origen de cero o varios `CatalogCharacter` (forks).
- `ApprovedPortrait`: retrato aprobado de un personaje. Relaciones: pertenece a un `CatalogCharacter`.
- `PortraitCandidate`: propuesta generada y pendiente de selección. Relaciones: pertenece a un `CatalogCharacter`; solo una está aprobada o ninguna.
- `PortraitGenerationJob`: trabajo de generación de retratos de un personaje. Estados: `RUNNING`, `COMPLETED`, `FAILED`. Relaciones: pertenece a un `CatalogCharacter`. Es la fuente de verdad del bloqueo.
- `PortraitGenerationLock`: bloqueo temporal de un personaje mientras dura una generación. Relaciones: pertenece a un `CatalogCharacter`; se deriva de la existencia de un `PortraitGenerationJob` en estado `RUNNING`.
- `StoryActorSnapshot`: instantánea inmutable del personaje usada en un cuento (existente). Relaciones: referencia a `CatalogCharacter`; determina si el personaje está "en uso".

## Decisiones

### DEC-001 Implementación real de `character-image-condition-port`

- **Decisión:** Crear `portrait-condition-adapter` como implementación del puerto `character-image-condition-port`, consultando `ApprovedPortrait` e imágenes generadas del personaje.
- **Alternativas descartadas:** Mantener el puerto sin implementar (imposibilita REQ-008, REQ-009 y REQ-010) o hacer que `character-inventory` gestione imágenes (fuera de su alcance).
- **Cubre:** REQ-008, REQ-009, REQ-010

### DEC-002 Workflow de 4 candidatos por lote

- **Decisión:** `portrait-comfyui-adapter` genera los 4 candidatos en una sola orquestación hacia ComfyUI, variando seeds/prompts para obtener diversidad manteniendo la identidad visual.
- **Alternativas descartadas:** 4 llamadas independientes (mayor latencia y carga de VRAM) o un único candidato (no cumple REQ-001).
- **Cubre:** REQ-001, REQ-002

### DEC-003 Bloqueo por personaje con lock explícito

- **Decisión:** `portrait-lock-service` mantiene un lock por `CatalogCharacter` durante la generación; cualquier operación concurrente es rechazada antes de modificar el personaje.
- **Alternativas descartadas:** Bloqueo a nivel de base de datos optimista (más complejo para operaciones de lectura/escritura mixtas) o no bloquear (rompería REQ-006).
- **Cubre:** REQ-006, REQ-007

### DEC-004 Candidatos temporales hasta la selección

- **Decisión:** Las 4 propuestas se almacenan como `PortraitCandidate` hasta que el usuario elige una; entonces las 3 restantes se eliminan.
- **Alternativas descartadas:** Guardar siempre las 4 como retratos alternativos (aumenta almacenamiento y contradice REQ-003) o eliminar al regenerar sin más (aceptable, ya se cubre en ALT-001).
- **Cubre:** REQ-003, REQ-004

### DEC-005 Notificación broadcast con identificador de personaje

- **Decisión:** Los eventos de retrato se publican en `/storyteller` con el `personajeId`; el frontend filtra los que le interesan. No se usa canal privado por sesión.
- **Alternativas descartadas:** Canal privado por usuario (el sistema es local de un solo usuario) o polling REST (menos reactivo).
- **Cubre:** REQ-005

### DEC-006 Fork completo del catálogo al cambiar visual en uso

- **Decisión:** Ante un cambio visual confirmado en un personaje con `StoryActorSnapshot`, se crea un `CatalogCharacter` nuevo copiando la ficha original, se aplica el cambio visual y se invalida su retrato.
- **Alternativas descartadas:** Editar el personaje original (prohibido por decisión cerrada) o crear solo una nueva revisión sin entidad (no permitiría gestionar el nuevo retrato como personaje independiente).
- **Cubre:** REQ-011, REQ-012, REQ-013

### DEC-007 Invalidación del retrato sin eliminar imágenes de cuentos

- **Decisión:** Al invalidar un retrato por cambio visual en personaje no usado, se eliminan el retrato aprobado y las imágenes generadas del catálogo; las instantáneas `StoryActorSnapshot` de cuentos previos no existen en este caso.
- **Alternativas descartadas:** Eliminar también snapshots (no hay, por definición de "no usado") o conservar el retrato aprobado (rompería REQ-010).
- **Cubre:** REQ-010

### DEC-008 Recuperación de locks huérfanos mediante reconciliación al arranque

- **Decisión:** El estado persistente del trabajo de generación (`PortraitGenerationJob`) es la fuente de verdad del bloqueo. El lock se deriva de un job en estado `RUNNING` y se libera cuando el job pasa a `COMPLETED` o `FAILED`. Al arrancar la aplicación, `portrait-reconciliation-service` marca como `FAILED` todos los jobs `RUNNING` huérfanos (aquellos cuya tarea en ejecución no haya sobrevivido al reinicio), libera sus locks y emite el evento de error correspondiente por WebSocket.
- **Alternativas descartadas:** TTL en el lock (rompería el requisito de no timeout de generación), lock en memoria sin persistencia (no soporta caída del servidor) y limpieza periódica en segundo plano (menos determinista y más compleja que la reconciliación al arrancar).
- **Cubre:** REQ-006, REQ-007

## Riesgos

- **Generación lenta o fallo de ComfyUI:** sin timeout, el usuario depende del evento final. Mitigación: notificar progreso intermedio cuando sea posible y permitir cancelación en futuras iteraciones.
- **Bloqueo huérfano:** si el servidor falla durante una generación, el lock podría quedar activo. Mitigación: el estado del trabajo es persistente y `portrait-reconciliation-service` reconcilia los jobs huérfanos al arrancar, liberando locks y notificando el error (DEC-008).
- **Concurrencia de selección y regeneración:** el frontend debe invalidar la galería si llega un evento de regeneración. Mitigación: `portrait-selection-service` valida vigencia de candidatos.
- **Acumulación de forks:** cambios visuales repetidos en personajes usados generan múltiples forks. Mitigación: es el comportamiento esperado; el usuario puede renombrar o eliminar en futuras features.

## Trazabilidad

| Requisito | Componentes | Decisiones |
|---|---|---|
| REQ-001 | portrait-generation-service, portrait-comfyui-adapter, portrait-candidate-store | DEC-002 |
| REQ-002 | portrait-comfyui-adapter | DEC-002 |
| REQ-003 | portrait-selection-service, portrait-candidate-store, character-catalog-view | DEC-004 |
| REQ-004 | portrait-generation-service, portrait-candidate-store | DEC-004 |
| REQ-005 | portrait-websocket-notifier, websocket-adapter, character-catalog-view | DEC-005 |
| REQ-006 | portrait-lock-service, portrait-generation-state-store, portrait-reconciliation-service, character-catalog-service, character-review-service | DEC-003, DEC-008 |
| REQ-007 | portrait-lock-service, portrait-generation-state-store, portrait-reconciliation-service | DEC-003, DEC-008 |
| REQ-008 | portrait-condition-adapter, character-catalog-service | DEC-001 |
| REQ-009 | character-review-service, portrait-condition-adapter, character-review-panel | DEC-001 |
| REQ-010 | character-review-service, portrait-condition-adapter, portrait-selection-service | DEC-001, DEC-007 |
| REQ-011 | character-review-service, portrait-fork-service, character-review-panel | DEC-006 |
| REQ-012 | portrait-fork-service, character-repository | DEC-006 |
| REQ-013 | portrait-fork-service, character-catalog-service | DEC-006 |

## Contratos afectados

| Protocolo | Contrato | Cambio |
|---|---|---|
| openapi | docs/contracts/openapi/paths/character/*.yaml | Añadir endpoints de generación de retratos, regeneración, consulta de candidatos y selección de retrato aprobado |
| openapi | docs/contracts/openapi/schemas/character*.yaml | Añadir esquemas de retrato aprobado, candidato, galería de candidatos, estado del trabajo de generación (`RUNNING`/`COMPLETED`/`FAILED`) y lock derivado |
| openapi | docs/contracts/openapi/paths/actor/*.yaml | Adaptar revisiones de personaje para incluir aviso de consistencia visual, aviso de fork y confirmación de fork |

## Cambios sugeridos al mapa

- Módulo: `portrait-generation | api/src/.../portrait | Genera y gestiona retratos cartoon del catálogo | api, agents, comfyui`
- Módulo: `portrait-comfyui | api/src/.../portrait/external | Adaptador de ComfyUI para retratos de personaje | api, comfyui`
- ~ Módulo: `character-catalog | api/src/.../character | Catálogo maestro de protagonistas reutilizables | api, agents, portrait-generation`
- ~ Contrato: `openapi | docs/contracts/openapi/paths/character/*.yaml | app -> api`
- ~ Contrato: `openapi | docs/contracts/openapi/schemas/character*.yaml | app <-> api`
- Contrato: `openapi | docs/contracts/openapi/paths/actor/*.yaml | app -> api`

## Avisos

- El mapa no incluye contratos WebSocket; los eventos de retrato se asumen sobre el canal `/storyteller` ya existente.
