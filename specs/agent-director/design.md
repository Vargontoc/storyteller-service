# Diseño: agent-director

Estado: aprobado
Revisión: 1
Basado en: requirements@2

## Resumen

Se reescribe el agente director para que, a partir de tema, tamaño y protagonista del catálogo, genere un guion completo con título, sinopsis, reparto episódico ligero y una escaleta contractual página a página. La escaleta se aprueba antes de escribir, admite revisión global por hint conservando los beats inalterados y marcando los cambiados, y permite la edición directa de un beat con validación de edad. El Modelfile del director pasa a gestionarse desde el backend, como el agente de personaje.

## Supuestos sobre el sistema actual

- El catálogo maestro `CatalogCharacter`, las instantáneas de cuento y el flujo de retratos aprobados ya están implementados (`character-inventory` y `character-portraits`).
- El patrón de revisiones con estados `PENDING`, `CONFIRMED`, `DISCARDED` ya existe para guion, personaje y página.
- El escritor no se modifica en esta feature; solo se amplía el contexto que recibe al generar cada página.
- Los cuentos antiguos carecen de escaleta estructurada y no se migrarán (REQ-016).
- El agente de personaje es el único responsable de generar y revisar fichas de personajes del catálogo (REQ-014).

## Componentes

| Componente | Responsabilidad | Existente o nuevo |
|---|---|---|
| director-agent | Agente Ollama que genera guiones, revisa la escaleta por hint global y valida ediciones directas de beat según reglas de edad | existente (modificado) |
| director-model-provisioner | Aprovisiona el modelo del director en Ollama al arrancar desde recursos del backend | nuevo |
| story-service | Casos de uso del guion: creación con protagonista obligatorio, generación, aprobación, revisión global y coordinación de ediciones directas | existente (modificado) |
| story-outline-repository | Persistencia del guion aprobado: título, sinopsis, reparto ligero y beats estructurados | nuevo |
| story-review-repository | Reutilizado para guardar la propuesta de revisión global con la escaleta candidata | existente (modificado) |
| story-page-service | Generación de portada/páginas; exige guion aprobado y entrega el beat correspondiente al escritor | existente (modificado) |
| beat-editor | Caso de uso de edición directa de un beat: valida edad, invalida aprobación y gestiona páginas generadas | nuevo |
| topic-selection-modal | Pantalla de creación de cuento: muestra protagonistas con retrato aprobado y opción de elección al azar | existente (modificado) |
| story-script-panel | Nueva vista del guion: escaleta, aprobación, hint global, revisión con diff y edición directa de beats | nuevo |
| story-modal | Orquesta las pestañas del cuento; redirige a la pantalla de guion mientras esté pendiente | existente (modificado) |
| character-catalog-service | Casos de uso del catálogo: listar personajes con retrato aprobado y resolver selección al azar | existente |
| writer-agent | Agente Ollama que genera el texto de cada página a partir del beat aprobado | existente |
| character-agent | Agente Ollama responsable de generar y revisar fichas de personajes del catálogo | existente |

## Flujo principal

1. El usuario inicia la creación de un cuento en `topic-selection-modal`, elige tema, tamaño y protagonista del catálogo (o la opción de elegir al azar).
2. `story-service` comprueba que el protagonista tenga retrato aprobado; si se eligió al azar, selecciona uno aleatorio de entre los disponibles.
3. `story-service` toma la instantánea textual y visual del protagonista y la envía al `director-agent` junto con tema y tamaño.
4. El `director-agent` devuelve título, sinopsis, reparto episódico ligero y una escaleta con un beat estructurado por página.
5. `story-service` valida que cada beat tenga los cinco campos obligatorios y persiste el cuento con estado `SCRIPT_PENDING`.
6. El usuario abre el cuento y `story-script-panel` muestra la escaleta completa con las acciones de aprobar o solicitar cambios.
7. Si el usuario aprueba, `story-service` pasa el guion a `SCRIPT_ACCEPTED`.
8. Si el usuario envía un hint global, `story-service` pide al `director-agent` una escaleta completa revisada, preservando los beats que no cambian y marcando los que sí.
9. La propuesta se guarda como revisión `PENDING`; al confirmarla se aplica la escaleta candidata como nueva versión actual, manteniendo el estado `SCRIPT_PENDING` hasta que el usuario apruebe explícitamente.
10. Al generar una página, `story-page-service` rechaza la operación si el guion no está `SCRIPT_ACCEPTED`; si lo está, entrega el beat de la página al escritor como contrato.

## Flujos alternativos y de fallo

### ALT-001 Catálogo sin protagonista apto

- **Condición:** No existe ningún personaje del catálogo con retrato aprobado cuando el usuario intenta crear un cuento.
- **Comportamiento:** `story-service` rechaza la creación e informa al usuario de que debe crear y aprobar al menos un protagonista.
- **Responsable:** story-service
- **Resultado:** El cuento no se crea; la UI muestra el aviso correspondiente.
- **Cubre:** REQ-001, REQ-020

### ALT-002 Beat incompleto devuelto por el director

- **Condición:** El `director-agent` devuelve un beat sin alguno de los cinco campos obligatorios.
- **Comportamiento:** `story-service` rechaza la respuesta, solicita una nueva generación (hasta el límite de reintentos del proyecto) y no persiste el cuento.
- **Responsable:** story-service
- **Resultado:** No se crea el cuento o se informa del error de validación.
- **Cubre:** REQ-004

### ALT-003 Hint global rechazado por el director

- **Condición:** El hint viola las reglas de edad o la coherencia del cuento.
- **Comportamiento:** El `director-agent` responde con `hintAccepted=false` y un motivo de rechazo; `story-service` guarda la revisión como rechazada y conserva la escaleta actual.
- **Responsable:** director-agent
- **Resultado:** El usuario ve el motivo del rechazo; la escaleta vigente no cambia.
- **Cubre:** REQ-009

### ALT-004 Edición directa de beat que incumple reglas de edad

- **Condición:** El usuario edita el texto de un beat y el cambio viola las reglas de edad.
- **Comportamiento:** `beat-editor` solicita validación al `director-agent` y rechaza el cambio con el motivo indicado.
- **Responsable:** beat-editor
- **Resultado:** El beat no se modifica; la UI muestra el motivo del rechazo.
- **Cubre:** REQ-010

### ALT-005 Edición directa en guion aprobado con páginas generadas

- **Condición:** El guion está `SCRIPT_ACCEPTED` y ya existen `StoryPage` generadas cuando el usuario inicia la edición directa de un beat.
- **Comportamiento:** `beat-editor` avisa de que las páginas existentes quedarán invalidadas y se eliminarán; si el usuario confirma, elimina las páginas del cuento, aplica las reglas de gestión de assets del producto y guarda el beat editado, pasando el guion a `SCRIPT_PENDING`.
- **Responsable:** beat-editor
- **Resultado:** Las páginas desaparecen del cuento, el beat se actualiza y el guion queda pendiente de aprobación.
- **Cubre:** REQ-011, REQ-012

### ALT-006 Generación de página con guion pendiente

- **Condición:** Se solicita generar la portada o una página y el guion no está `SCRIPT_ACCEPTED`.
- **Comportamiento:** `story-page-service` rechaza la operación y solicita la aprobación del guion.
- **Responsable:** story-page-service
- **Resultado:** No se genera la página; la UI muestra el aviso de aprobación requerida.
- **Cubre:** REQ-006

## Interfaces entre componentes

| Origen | Destino | Qué se comunica | Protocolo |
|---|---|---|---|
| app | story-service | Crear cuento, generar guion, aprobar, solicitar revisión global, confirmar/descartar revisión, editar beat | openapi |
| app | character-catalog-service | Listado de protagonistas con retrato aprobado | openapi |
| story-service | director-agent | Prompt de generación, revisión global o validación de edición con tema, tamaño, protagonista y escaleta actual | ollama |
| story-service | story-outline-repository | Lectura/escritura del guion y sus beats | - |
| story-service | story-review-repository | Lectura/escritura de la revisión global con escaleta candidata | - |
| story-service | character-catalog-service | Consulta de protagonistas disponibles y resolución de elegido al azar | - |
| beat-editor | director-agent | Texto editado de un beat para validación de reglas de edad | ollama |
| beat-editor | story-outline-repository | Guardado del beat validado | - |
| beat-editor | story-page-service | Orden de eliminación de páginas invalidadas por edición | - |
| story-page-service | story-outline-repository | Lectura del beat correspondiente a la página que se va a generar | - |
| story-page-service | writer-agent | Beat aprobado como contrato para generar el texto de la página | ollama |
| director-model-provisioner | Ollama | Creación/registro del modelo `storyteller-director` al arranque | http |

## Modelo de datos lógico

- `Story`: cuento. Relaciones: tiene cero o una `StoryOutline`; tiene cero o varias `StoryPage`; referencia al protagonista mediante `StoryActorSnapshot`. Se añade un marcador de versión de guion para distinguir cuentos antiguos (sin escaleta) de nuevos.
- `StoryOutline`: guion aprobado o pendiente de un cuento. Contiene título, sinopsis, reparto episódico ligero y la lista de `StoryBeat`. Relaciones: pertenece a un `Story`; puede tener asociada una `StoryReview` candidata.
- `StoryBeat`: beat estructurado de una página. Campos lógicos: qué ocurre, función narrativa, quiénes aparecen, emoción/tono y nota visual. Relaciones: pertenece a una `StoryOutline`; se vincula lógicamente con la `StoryPage` del mismo número de página.
- `EpisodicCastMember`: personaje secundario del reparto ligero. Contiene nombre y función narrativa; pertenece a una `StoryOutline` y no se persiste en el catálogo de personajes.
- `StoryReview`: revisión global del guion. Se reutiliza la entidad existente; almacena el hint, la escaleta candidata, el indicador de aceptación, el motivo de rechazo y el estado (`PENDING`, `CONFIRMED`, `DISCARDED`). Relaciones: pertenece a un `Story`.
- `StoryActorSnapshot`: instantánea del protagonista en el cuento (existente). Conserva la ficha y el retrato aprobado en el momento de la generación.
- `StoryPage`: página o portada del cuento (existente). Relaciones: pertenece a un `Story`; se vincula lógicamente con el `StoryBeat` del mismo número de página.

## Decisiones

### DEC-001 Reutilizar `StoryReview` para las revisiones globales del guion

- **Decisión:** Reutilizar la entidad `StoryReview` existente y extenderla para que almacene la escaleta candidata completa en lugar de (o además de) los datos antiguos de guion.
- **Alternativas descartadas:** Crear una entidad `StoryOutlineReview` separada. Se descarta porque duplicaría el mismo ciclo de vida PENDING/CONFIRMED/DISCARDED y la lógica de confirmación/descarte ya implementada.
- **Cubre:** REQ-007, REQ-008, REQ-009

### DEC-002 Gestionar el Modelfile del director desde el backend

- **Decisión:** El Modelfile del director reside como recurso del backend (por ejemplo, junto a las plantillas de prompt) y se aprovisiona en Ollama al arranque mediante un `DirectorModelProvisioner`, análogo al `CharacterModelProvisioner`. La carpeta `agents/director-story-agent/` se elimina y `agents/load_model.ps1` deja de referenciar al director, manteniéndose únicamente para los agentes topics, writer y narrator mientras no migren.
- **Alternativas descartadas:** Seguir manteniéndolo en `agents/director-story-agent/` (incumple REQ-015) o cargarlo manualmente en Ollama fuera de la aplicación (más frágil).
- **Cubre:** REQ-015

### DEC-003 Eliminar `DiscoverCharacterService` y el prompt `discover_characcter.st`

- **Decisión:** Al desaparecer el MODO 3 del director y al ser el reparto secundario ligero (sin ficha visual ni catálogo), no se requiere un servicio de descubrimiento de personajes. Los secundarios que aparezcan en una escena los resuelve el escritor a partir del beat y de la descripción de escena.
- **Alternativas descartadas:** Rehacer el descubrimiento con el agente de personaje (crearía entradas de catálogo para personajes episódicos, contradiciendo REQ-005) o mantener `DiscoverCharacterService` con el director (imposible sin MODO 3).
- **Cubre:** REQ-005, REQ-014

### DEC-004 Validar ediciones directas con el director-agent

- **Decisión:** La edición directa de un beat se valida contra las reglas de edad invocando al `director-agent`, de modo que las reglas residen en un único lugar.
- **Alternativas descartadas:** Implementar un validador local con las mismas reglas (duplicaría lógica y podría divergir del agente).
- **Cubre:** REQ-010

### DEC-005 Convivencia de cuentos antiguos y nuevos mediante marcador de versión

- **Decisión:** Se añade un marcador de versión de guion a `Story` para distinguir los cuentos creados con el formato anterior de los creados con escaleta contractual. Los antiguos se mantienen con su estructura original; los nuevos exigen `StoryOutline`.
- **Alternativas descartadas:** Migrar cuentos antiguos (REQ-016 lo prohíbe) o forzar a todos los cuentos a tener escaleta (rompería los antiguos).
- **Cubre:** REQ-016

### DEC-006 Beats vinculados a páginas interiores

- **Decisión:** Cada beat se asocia a una página interior del cuento (numeración 1..N); la portada (página 0) sigue su propio flujo de generación con el título, la sinopsis y el protagonista como contexto.
- **Alternativas descartadas:** Incluir un beat para la portada (su contrato es principalmente visual y ya tiene plantilla propia) o no vincular beats a números de página (dificultaría entregar el contrato al escritor).
- **Cubre:** REQ-004, REQ-013

## Riesgos

- **El director no conserva exactamente los beats inalterados:** aunque el prompt lo ordene, el modelo puede reescribir levemente beats no implicados por el hint. Mitigación: comparar los beats devueltos con los actuales en el backend y marcar como cambiados solo los que realmente difieran.
- **Edición directa depende de la disponibilidad de Ollama:** si el director no responde, no se puede validar una edición de beat. Mitigación: devolver un error claro al usuario y permitir reintentar; no aplicar cambios sin validación.
- **Cuentos antiguos sin escaleta en la misma interfaz:** el `StoryModal` debe mostrar la vista antigua para cuentos previos y la nueva `story-script-panel` para cuentos nuevos. Mitigación: usar el marcador de versión de guion para decidir la representación.

## Trazabilidad

| Requisito | Componentes | Decisiones |
|---|---|---|
| REQ-001 | story-service, topic-selection-modal, character-catalog-service | - |
| REQ-002 | story-service, character-catalog-service | - |
| REQ-003 | director-agent, story-service | - |
| REQ-004 | director-agent, story-service, story-outline-repository | DEC-006 |
| REQ-005 | director-agent, story-outline-repository | DEC-003 |
| REQ-006 | story-service, story-page-service | - |
| REQ-007 | story-service, story-script-panel | - |
| REQ-008 | director-agent, story-service, story-review-repository | DEC-001 |
| REQ-009 | director-agent, story-service | DEC-001 |
| REQ-010 | beat-editor, director-agent, story-outline-repository | DEC-004 |
| REQ-011 | beat-editor, story-service, story-page-service | - |
| REQ-012 | beat-editor, story-page-service, story-script-panel | - |
| REQ-013 | story-page-service, writer-agent, story-outline-repository | DEC-006 |
| REQ-014 | story-service, character-agent | DEC-003 |
| REQ-015 | director-model-provisioner, director-agent | DEC-002 |
| REQ-016 | story-service, story-outline-repository | DEC-005 |
| REQ-017 | story-script-panel, story-service | - |
| REQ-018 | story-script-panel, story-service | DEC-001 |
| REQ-019 | story-script-panel, beat-editor | - |
| REQ-020 | topic-selection-modal, character-catalog-service | - |

## Contratos afectados

| Protocolo | Contrato | Cambio |
|---|---|---|
| openapi | docs/contracts/openapi/openapi.yaml | Añadir esquemas de escaleta y reparto ligero; añadir paths de aprobación de guion y edición de beat |
| openapi | docs/contracts/openapi/schemas/requests/story-generate-command.yaml | Añadir `catalogCharacterId` (obligatorio salvo elección al azar) y campo que indique selección aleatoria |
| openapi | docs/contracts/openapi/schemas/story.yaml | Añadir estado del guion, versión de guion, escaleta y reparto episódico ligero |
| openapi | docs/contracts/openapi/schemas/story-review.yaml | Sustituir o ampliar `previewStory` por la escaleta candidata, indicador de beats cambiados y resultado de aceptación del hint |
| openapi | docs/contracts/openapi/paths/story/generation-service.yaml | Actualizar descripción: el director recibe protagonista del catálogo y devuelve escaleta |
| openapi | docs/contracts/openapi/paths/story/review-service.yaml | Actualizar descripción: revisión global que devuelve escaleta completa con aceptación o rechazo |
| openapi | docs/contracts/openapi/paths/story/confirm-review-service.yaml | Confirmar/descartar la revisión global de la escaleta |
| openapi | docs/contracts/openapi/paths/story/review-by-id-service.yaml | Obtener la revisión global pendiente con la escaleta candidata y los beats marcados como cambiados |
| openapi | docs/contracts/openapi/paths/story/*.yaml | Añadir endpoint para aprobar el guion explícitamente |
| openapi | docs/contracts/openapi/paths/story/*.yaml | Añadir endpoint para editar directamente un beat (validación de edad incluida) |
| openapi | docs/contracts/openapi/schemas/story-page.yaml | Posible ampliación del contexto de generación; el beat aprobado se entrega internamente al escritor |

## Cambios sugeridos al mapa

- ~ Módulo: `agents | agents/ | Carga de agentes topics, writer y narrator | Modelfiles + script PowerShell`
- Módulo: `director-agent | api/src/main/resources/agents/director-story-agent/Modelfile | Prompt y parámetros del agente director | ->`
- Módulo: `director-model-provisioner | api/src/.../config | Aprovisiona el modelo del director en Ollama | api, ollama`
- ~ Módulo: `api | api/ | Lógica de negocio, orquestación de agentes, persistencia y API REST/WS | Java 21, Spring Boot 4.1, Spring AI, JPA, Liquibase`
- ~ Módulo: `app | app/ | Interfaz de usuario SPA | Vue 3, Vite, TypeScript, Pinia`
- Contrato: `openapi | docs/contracts/openapi/paths/story/*.yaml | app -> api`
- Contrato: `openapi | docs/contracts/openapi/schemas/story*.yaml | app <-> api`
- - Módulo: `discover-character | api/src/.../application/service/DiscoverCharacterService | Detecta personajes nuevos en escena | director-agent`

## Avisos

- El mapa actual indica que `DiscoverCharacterService` usa el agente director. Esta feature elimina ese servicio porque el reparto secundario es ligero y el director pierde el MODO 3. Se asume que el escritor puede resolver personajes episódicos desde el beat y la descripción de escena.
- El mapa no detalla dónde residirá el Modelfile del director tras el traslado; este diseño propone recursos del backend con un provisioner análogo al de personaje.
- Los cuentos antiguos se mantienen sin migración; la UI deberá decidir su representación mediante el marcador de versión de guion.
