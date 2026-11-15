# Diseño: character-inventory

Estado: aprobado
Revisión: 1
Basado en: requirements@1

## Resumen

El catálogo de personajes se modela como una biblioteca maestra (`CatalogCharacter`) independiente de los cuentos, con un nuevo agente de personaje encargado de generar y validar la ficha textual. Las revisiones conversacionales se gestionan con el mismo patrón de revisiones ya existente, pero aplicado al catálogo; el estado "borrador" y el aviso de consistencia visual se calculan consultando condiciones mantenidas por el módulo de assets, sin gestionarlas aquí.

## Supuestos sobre el sistema actual

- El backend ya tiene un patrón de revisiones con estados `PENDING`, `CONFIRMED`, `DISCARDED` para guion, personaje y página.
- La entidad `Actor` actual está vinculada a un cuento y actúa como personaje de cuento; esta feature requiere separar el catálogo maestro de la instantánea usada en cada cuento.
- El módulo de assets/imágenes ya existe o se diseñará en otra feature; este diseño solo lee dos condiciones de él: "retrato aprobado" e "imágenes generadas".
- Las reglas de edad y especies residen en la configuración y el prompt del agente de personaje (SUP-001 validado).

## Componentes

| Componente | Responsabilidad | Existente o nuevo |
|---|---|---|
| character-agent | Agente Ollama que genera fichas textuales y valida o revisa cambios según reglas de edad y especies | nuevo |
| character-catalog-service | Casos de uso del catálogo: listar, alta automática/manual, consultar ficha y renombrar | nuevo |
| character-review-service | Solicitar, consultar, aceptar y descartar revisiones de personajes del catálogo | existente |
| character-validation | Valida el contrato de salida del agente y la presencia de campos obligatorios | existente |
| character-repository | Persistencia de personajes del catálogo | existente |
| character-review-repository | Persistencia de revisiones de personajes | existente |
| character-image-condition-port | Puerto que consulta si un personaje tiene retrato aprobado o imágenes generadas | nuevo |
| character-catalog-view | Vista principal del catálogo en la SPA | nuevo |
| character-form-modal | Modal de alta automática o manual de personaje | nuevo |
| character-review-panel | Muestra la revisión pendiente, el diff y los botones de aceptar o descartar | existente |

## Flujo principal

1. El usuario abre `character-catalog-view`.
2. `character-catalog-service` consulta `character-repository` y devuelve la lista con nombre, estado (borrador o activo) y badge de revisión pendiente.
3. El usuario selecciona un personaje; la vista muestra su ficha textual y, si existe, la revisión pendiente.
4. El usuario elige "Automático" o "Manual" en `character-form-modal`.
5. En modo automático, `character-catalog-service` pide al `character-agent` una ficha completa; en modo manual, le envía la frase del usuario.
6. Si el agente rechaza la entrada, `character-catalog-service` devuelve el aviso con el motivo y no persiste nada.
7. Si el agente acepta, devuelve nombre, descripción narrativa y descripción visual; `character-catalog-service` crea el `CatalogCharacter` en estado borrador.
8. El usuario puede renombrar el personaje; `character-catalog-service` actualiza solo el catálogo.

## Flujos alternativos y de fallo

### ALT-001 Rechazo en la creación manual o automática

- **Condición:** El `character-agent` determina que la frase o la generación no cumple las reglas de edad o especies.
- **Comportamiento:** Devuelve un motivo de rechazo; `character-catalog-service` no crea el personaje y la UI muestra el aviso.
- **Responsable:** character-agent
- **Resultado:** El catálogo no se modifica; el usuario ve el motivo del rechazo.
- **Cubre:** REQ-004, REQ-008

### ALT-002 Revisión previa pendiente

- **Condición:** El usuario solicita una revisión y ya existe otra en estado `PENDING` para el mismo personaje.
- **Comportamiento:** `character-review-service` advierte al usuario; si confirma, marca la revisión previa como `DISCARDED` y procesa la nueva.
- **Responsable:** character-review-service
- **Resultado:** Solo queda una revisión pendiente por personaje.
- **Cubre:** REQ-007

### ALT-003 Cambio visual con imágenes generadas

- **Condición:** La propuesta de revisión modifica la descripción visual y el personaje ya tiene imágenes generadas.
- **Comportamiento:** `character-review-service` consulta `character-image-condition-port` y, si aplica, incluye un aviso de pérdida de consistencia visual en la respuesta mostrada al usuario.
- **Responsable:** character-review-service
- **Resultado:** El usuario ve la propuesta junto con el aviso de consistencia.
- **Cubre:** REQ-009

## Interfaces entre componentes

| Origen | Destino | Qué se comunica | Protocolo |
|---|---|---|---|
| app | character-catalog-service | Peticiones de listado, alta, detalle y renombrado | openapi |
| app | character-review-service | Solicitud, consulta, aceptación y descarte de revisiones | openapi |
| character-catalog-service | character-agent | Prompt de generación o validación con modo y frase | ollama |
| character-review-service | character-agent | Prompt de revisión con hint, target y ficha actual | ollama |
| character-catalog-service | character-repository | Lectura y escritura de personajes del catálogo | - |
| character-review-service | character-review-repository | Lectura y escritura de revisiones | - |
| character-review-service | character-image-condition-port | Consulta de retrato aprobado e imágenes generadas | - |

## Modelo de datos lógico

- `CatalogCharacter`: personaje maestro del catálogo. Relaciones: tiene cero o varias `CharacterReview`; es referenciado por cero o varias `StoryActorSnapshot` (instantáneas de cuentos, fuera de alcance).
- `CharacterReview`: revisión de un personaje. Relaciones: pertenece a un `CatalogCharacter`. Estados: `PENDING`, `CONFIRMED`, `DISCARDED`. Contiene el hint, el target (`VISUAL`, `NARRATIVE`, `BOTH`), la propuesta candidata y, si fue rechazada, el motivo.
- `StoryActorSnapshot` (mencionado, no diseñado): copia inmutable del personaje usada en un cuento, para conservar el nombre y la ficha originales si el catálogo cambia.

## Decisiones

### DEC-001 Separación del catálogo maestro y la instantánea de cuento

- **Decisión:** Crear `CatalogCharacter` como entidad maestra independiente; cada cuento guarda una `StoryActorSnapshot` derivada del catálogo en el momento de uso.
- **Alternativas descartadas:** Reutilizar `Actor` como catálogo (imposibilita conservar instantáneas inmutables) o no conservar instantáneas (rompería cuentos ya generados al editar).
- **Cubre:** REQ-006, FUN-005, PDEC-007

### DEC-002 Agente de personaje como agente propio

- **Decisión:** Añadir un nuevo agente Ollama (`character-agent`) con su propio Modelfile para generar y revisar fichas textuales.
- **Alternativas descartadas:** Reutilizar el agente director (mezclaría responsabilidades y contexto de guion) o un modelo más pequeño (riesgo de calidad en descripción visual y rechazos).
- **Cubre:** REQ-003, REQ-004, REQ-008, FUN-007, PDEC-003

### DEC-003 Estado "borrador" como condición derivada

- **Decisión:** El estado "borrador" se calcula a partir de "no existe retrato aprobado", consultado a través de `character-image-condition-port`.
- **Alternativas descartadas:** Campo booleano explícito en el catálogo (requeriría que el catálogo gestione el retrato, que está fuera de alcance).
- **Cubre:** REQ-005

### DEC-004 Aviso de consistencia visual en el servicio de revisión

- **Decisión:** `character-review-service` consulta `character-image-condition-port` para detectar imágenes generadas y, solo si el cambio afecta a la descripción visual, añade el aviso.
- **Alternativas descartadas:** Delegar la decisión al agente (la existencia de imágenes es un hecho del sistema, no una regla narrativa).
- **Cubre:** REQ-009

### DEC-005 Renombrado solo en el catálogo

- **Decisión:** El renombrado actualiza `CatalogCharacter`; las `StoryActorSnapshot` ya creadas conservan el nombre anterior.
- **Alternativas descartadas:** Propagar el cambio a cuentos existentes (rompería la coherencia de cuentos ya aceptados).
- **Cubre:** REQ-006, PDEC-011

## Riesgos

- **Impacto en generación de cuentos existente:** separar catálogo de instantánea requiere adaptar la generación de cuentos para leer del catálogo y crear snapshots. Mitigación: mantener un mecanismo de compatibilidad temporal o migración de personajes históricos.
- **Dependencia no diseñada de assets:** `character-image-condition-port` requiere que el módulo de imágenes exponga la condición de retrato aprobado e imágenes generadas. Mitigación: definir el puerto aquí y dejar su implementación para la feature de assets.
- **Coherencia de reglas entre agentes:** el agente de personaje y el director deben compartir criterios de edad y especies. Mitigación: centralizar la configuración de reglas base y referenciarla en ambos Modelfiles.

## Trazabilidad

| Requisito | Componentes | Decisiones |
|---|---|---|
| REQ-001 | character-catalog-service, character-catalog-view, character-repository | - |
| REQ-002 | character-catalog-service, character-catalog-view | - |
| REQ-003 | character-agent, character-catalog-service | DEC-002 |
| REQ-004 | character-agent, character-catalog-service, character-validation | DEC-002 |
| REQ-005 | character-image-condition-port, character-catalog-service, character-catalog-view | DEC-003 |
| REQ-006 | character-catalog-service, character-repository | DEC-001, DEC-005 |
| REQ-007 | character-review-service, character-review-repository | - |
| REQ-008 | character-agent, character-review-service, character-validation | DEC-002 |
| REQ-009 | character-image-condition-port, character-review-service | DEC-004 |
| REQ-010 | character-review-service, character-catalog-view | - |
| REQ-011 | character-review-service, character-repository | - |
| REQ-012 | character-review-service, character-review-repository | - |

## Contratos afectados

| Protocolo | Contrato | Cambio |
|---|---|---|
| openapi | docs/contracts/openapi/openapi.yaml | Añadir recurso de catálogo de personajes para listado, detalle, alta automática o manual y renombrado |
| openapi | docs/contracts/openapi/paths/actor/*.yaml | Adaptar revisiones de personaje para actuar sobre el catálogo en lugar del actor de cuento |
| openapi | docs/contracts/openapi/schemas/actor.yaml | Incluir referencia al catálogo y distinguir snapshot de cuento |
| openapi | docs/contracts/openapi/schemas/actor-review-command.yaml | Eliminar `storyId` como obligatorio; usar identificador de catálogo |

## Cambios sugeridos al mapa

- Módulo: `character-agent | agents/character-agent/Modelfile | Genera y revisa fichas textuales de personaje | ->`
- Módulo: `character-catalog | api/src/.../character | Catálogo maestro de protagonistas reutilizables | api, agents`
- ~ Módulo: `api | api/ | Lógica de negocio, orquestación de agentes, persistencia y API REST/WS | Java 21, Spring Boot 4.1, Spring AI, JPA, Liquibase`
- ~ Módulo: `app | app/ | Interfaz de usuario SPA | Vue 3, Vite, TypeScript, Pinia`
- Contrato: `openapi | docs/contracts/openapi/paths/character/*.yaml | app -> api`
- Contrato: `openapi | docs/contracts/openapi/schemas/character*.yaml | app <-> api`
