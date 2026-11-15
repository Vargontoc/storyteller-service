# Tareas: character-inventory

Estado: aprobado
Revisión: 1
Basado en: requirements@1, design@1

## Resumen

El trabajo se divide entre la definición del agente de personaje, los casos de uso y adaptadores del catálogo, la gestión de revisiones, la interfaz de usuario y la formalización de los contratos OpenAPI afectados. El puerto de condiciones visuales se define como dependencia de integración, sin implementar la generación ni gestión de assets.

## Tareas

### [x] TASK-001 Definir el agente de personaje y su contrato de salida textual

- **Cubre:** REQ-003, REQ-004, REQ-008, DEC-002
- **Componente:** character-agent | api/src/main/java/es/vargontoc/storyteller/infrastructure/adapters/out/external/SpringAiCharacterAgent.java
- **Depende de:** -
- **Alcance:** Definir el agente Ollama propio para generar fichas textuales, validar entradas y proponer revisiones, usando las reglas de edad y especies de la configuración compartida.
- **Hecho cuando:** el agente dispone de una definición cargable con Modelfile y prompts que permiten obtener nombre, descripción narrativa y descripción visual, además de distinguir entradas aceptadas y rechazadas con motivo.
- **Pruebas automáticas:**
  - Una solicitud de generación automática produce un resultado con nombre, descripción narrativa y descripción visual válidos.
  - Una entrada válida en modo manual produce una ficha textual completa.
  - Una entrada no válida según las reglas configuradas produce un rechazo con motivo y no una ficha aplicable.
  - Una solicitud de revisión válida produce una propuesta y una solicitud de revisión no válida produce un rechazo con motivo.
- **Prueba manual:**
  1. Cargar el modelo del agente de personaje según el procedimiento de los módulos `agents`.
  2. Solicitar una ficha automática y comprobar que incluye nombre, descripción narrativa y descripción visual.
  3. Someter una frase apta y otra no apta para el público infantil y comprobar que la apta se completa y la no apta devuelve el motivo del rechazo.

### [ ] TASK-002 Definir el puerto de condiciones visuales de personaje

- **Cubre:** REQ-005, REQ-009, DEC-003, DEC-004, ALT-003
- **Componente:** character-image-condition-port | por definir (analista técnico)
- **Depende de:** -
- **Alcance:** Definir la interfaz de salida que permite consultar si existe un retrato aprobado y si existen imágenes generadas para un personaje del catálogo, dejando su implementación fuera de esta feature.
- **Hecho cuando:** el contrato de la interfaz permite consultar ambas condiciones por identificador de personaje sin depender de una implementación concreta de assets.
- **Pruebas automáticas:**
  - No aplica: esta tarea define una interfaz de puerto; sus implementaciones pertenecen a la feature de assets.
- **Prueba manual:**
  1. Revisar la interfaz y comprobar que expone consultas separadas para retrato aprobado e imágenes generadas.
  2. Comprobar que el puerto no incluye operaciones para generar, seleccionar, almacenar o modificar imágenes.

### [ ] TASK-003 Implementar el alta y la validación de fichas en el catálogo

- **Cubre:** REQ-003, REQ-004, ALT-001, DEC-002
- **Componente:** character-catalog-service | api/src/.../character
- **Depende de:** TASK-001, TASK-002
- **Alcance:** Implementar los casos de uso de creación automática y manual mediante el agente, validando la salida y persistiendo una ficha completa solo cuando sea aceptada.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-003 y REQ-004 y el catálogo crea un personaje con nombre, descripción narrativa y descripción visual, sin persistir nada ante un rechazo.
- **Pruebas automáticas:**
  - En modo automático, una respuesta aceptada del agente produce una ficha textual completa y persistida.
  - En modo manual, una frase válida produce una ficha textual completa y persistida.
  - En modo manual o automático, una respuesta rechazada devuelve el motivo, no crea el personaje y no modifica el catálogo.
  - Una salida del agente a la que le falte cualquiera de los campos obligatorios se rechaza sin persistir una ficha incompleta.
- **Prueba manual:**
  1. Crear un personaje en modo automático sin introducir texto y verificar que se obtiene una ficha completa.
  2. Crear un personaje con una frase que incluya nombre y descripción y verificar que el agente completa la ficha.
  3. Intentar un alta no apta y verificar que se muestra el motivo y no aparece el personaje en el catálogo.

### [ ] TASK-004 Implementar el listado, detalle, estado derivado y renombrado del catálogo

- **Cubre:** REQ-001, REQ-002, REQ-005, REQ-006, DEC-001, DEC-003, DEC-005
- **Componente:** character-catalog-service | api/src/.../character
- **Depende de:** TASK-002, TASK-003
- **Alcance:** Implementar la consulta del catálogo y de la ficha, el cálculo derivado del estado de borrador y el renombrado limitado al personaje maestro del catálogo.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-001, REQ-002, REQ-005 y REQ-006, el listado incluye nombre, estado y situación de revisión, y el renombrado no modifica instantáneas ya existentes.
- **Pruebas automáticas:**
  - El listado devuelve todos los personajes con nombre y estado, y una colección vacía cuando no hay personajes.
  - El detalle devuelve nombre, descripción narrativa y descripción visual.
  - Un personaje sin retrato aprobado aparece como borrador tanto en listado como en detalle; con retrato aprobado no aparece como borrador.
  - Renombrar actualiza el nombre del catálogo y no altera el nombre de ninguna instantánea de cuento asociada.
- **Prueba manual:**
  1. Abrir el catálogo con dos personajes y comprobar que aparecen sus nombres y estados.
  2. Abrir el catálogo vacío y comprobar que se muestra el indicador de catálogo vacío.
  3. Seleccionar un personaje y revisar que se muestran nombre, descripción narrativa, descripción visual y estado.
  4. Cambiar el nombre de un personaje y comprobar que la ficha muestra el nuevo nombre.

### [ ] TASK-005 Adaptar el servicio de revisiones al catálogo y al agente de personaje

- **Cubre:** REQ-007, REQ-008, REQ-009, ALT-001, ALT-002, ALT-003, DEC-002, DEC-004
- **Componente:** character-review-service | no consta en el mapa
- **Depende de:** TASK-001, TASK-002, TASK-003
- **Alcance:** Implementar la solicitud, consulta y persistencia de revisiones de personajes del catálogo, incluyendo validación de cambios, descarte de una revisión previa confirmada y el aviso de consistencia visual.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-007, REQ-008 y REQ-009, las revisiones válidas quedan pendientes y solo hay una revisión pendiente por personaje.
- **Pruebas automáticas:**
  - Una solicitud sin revisión previa se procesa directamente y queda en estado pendiente.
  - Una solicitud con revisión previa devuelve el aviso de confirmación; al confirmar, la previa queda descartada y la nueva se procesa como pendiente.
  - Un cambio no válido devuelve el motivo del rechazo y no crea una revisión pendiente.
  - Un cambio válido crea una revisión pendiente con la propuesta del agente.
  - Un cambio visual con imágenes generadas incluye el aviso de consistencia visual.
  - Un cambio visual sin imágenes generadas no incluye ese aviso.
  - Un cambio solo narrativo con imágenes generadas no incluye el aviso de consistencia visual.
- **Prueba manual:**
  1. Solicitar una revisión de un personaje sin revisión previa y comprobar que se muestra directamente la propuesta pendiente.
  2. Repetir la solicitud y confirmar el aviso; comprobar que la revisión anterior queda descartada y la nueva pendiente.
  3. Enviar un cambio no apto y comprobar que se muestra el motivo sin crear revisión.
  4. Revisar un cambio visual de un personaje con imágenes y comprobar que la propuesta aparece acompañada del aviso de consistencia visual.

### [ ] TASK-006 Implementar la aceptación y el descarte de revisiones del catálogo

- **Cubre:** REQ-011, REQ-012
- **Componente:** character-review-service | no consta en el mapa
- **Depende de:** TASK-004, TASK-005
- **Alcance:** Implementar la aplicación de una propuesta aceptada al texto del personaje y el descarte de una revisión conservando intacta la ficha vigente.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-011 y REQ-012, la revisión deja de estar pendiente en ambos casos y solo la aceptación modifica el texto del catálogo.
- **Pruebas automáticas:**
  - Aceptar una revisión narrativa actualiza la descripción narrativa de la ficha y cambia el estado de la revisión a confirmado.
  - Aceptar una revisión visual actualiza la descripción visual de la ficha y cambia el estado de la revisión a confirmado.
  - Descartar una revisión conserva sin cambios nombre, descripción narrativa y descripción visual y cambia el estado de la revisión a descartado.
- **Prueba manual:**
  1. Abrir una revisión pendiente que cambie la descripción narrativa y pulsar "Aceptar"; comprobar que la ficha muestra el texto nuevo y desaparece el badge.
  2. Abrir otra revisión pendiente y pulsar "Descartar"; comprobar que la ficha conserva su texto y desaparece el badge.

### [ ] TASK-007 Construir la vista del catálogo y el modal de alta

- **Cubre:** REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-010, ALT-001
- **Componente:** character-catalog-view | app/
- **Depende de:** TASK-003, TASK-004
- **Alcance:** Construir la vista principal del catálogo, la selección de ficha y el modal de alta automática o manual, incluyendo estados vacíos, errores de rechazo, borrador y badges de revisión.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-001, REQ-002, REQ-003, REQ-004, REQ-005 y REQ-010 en el flujo de usuario del catálogo.
- **Pruebas automáticas:**
  - No aplica: la verificación del flujo visual de la SPA requiere pruebas manuales hasta que el proyecto disponga de cobertura automatizada para este componente.
- **Prueba manual:**
  1. Abrir el catálogo vacío y comprobar que aparece el indicador de que no hay personajes.
  2. Abrir el catálogo con personajes y comprobar que cada tarjeta o fila muestra nombre, estado y, si corresponde, badge de revisión.
  3. Seleccionar un personaje y comprobar que se ven nombre, descripción narrativa, descripción visual y el estado de borrador cuando procede.
  4. Crear un personaje en modo automático y comprobar que se presenta una ficha completa.
  5. Crear un personaje en modo manual con una frase válida y comprobar que se presenta una ficha completa.
  6. Enviar una frase no válida y comprobar que se muestra el motivo del rechazo y no se crea el personaje.
  7. Comprobar que una ficha con revisión pendiente muestra el badge tanto en el catálogo como junto a la sección de revisión.

### [ ] TASK-008 Integrar el panel de revisión en el catálogo

- **Cubre:** REQ-007, REQ-008, REQ-009, REQ-010, REQ-011, REQ-012, ALT-002, ALT-003
- **Componente:** character-review-panel | app/
- **Depende de:** TASK-005, TASK-006, TASK-007
- **Alcance:** Integrar en la ficha del catálogo la solicitud de revisión, la propuesta y su diff, la confirmación de descarte de una previa, los avisos aplicables y las acciones de aceptar o descartar.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-007 a REQ-012 y el usuario puede completar desde la ficha todo el ciclo de revisión sin salir del catálogo.
- **Pruebas automáticas:**
  - No aplica: la verificación del flujo visual de la SPA requiere pruebas manuales hasta que el proyecto disponga de cobertura automatizada para este componente.
- **Prueba manual:**
  1. Enviar un cambio válido y comprobar que se muestran la propuesta pendiente y el diff.
  2. Enviar un cambio no válido y comprobar que aparece el motivo del rechazo.
  3. Solicitar una revisión cuando ya existe una pendiente, confirmar el aviso y comprobar que la nueva queda pendiente y la previa descartada.
  4. Comprobar que el aviso de consistencia visual aparece junto a la propuesta cuando se cumplen sus condiciones.
  5. Aceptar la revisión y comprobar que se aplica a la ficha y desaparece el badge.
  6. Descartar la revisión y comprobar que se conserva la ficha y desaparece el badge.

### [ ] TASK-009 Contrato: openapi - recurso de catálogo de personajes

- **Cubre:** REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006
- **Componente:** app | app/
- **Depende de:** TASK-003, TASK-004
- **Alcance:** Crear o actualizar el contrato OpenAPI del recurso de catálogo de personajes para listado, detalle, alta automática o manual y renombrado, en `docs/contracts/openapi/`, con el nombre del fichero por definir (analista técnico).
- **Hecho cuando:** el contrato existe en `docs/contracts/openapi/` y describe el recurso de catálogo y sus operaciones con los datos y respuestas definidos por la implementación, incluidas las respuestas de rechazo requeridas.
- **Pruebas automáticas:**
  - El contrato OpenAPI es válido según las reglas de su formato.
  - El contrato recoge las operaciones de listado, detalle, alta automática, alta manual y renombrado.
  - El contrato recoge una respuesta de rechazo con motivo para las altas no válidas.
- **Prueba manual:**
  1. Recorrer el contrato y comprobar que cada interfaz de esta tarea aparece con su método, entrada, salida y errores relevantes.
  2. Comparar el contrato con la implementación y confirmar que listado, detalle, alta automática, alta manual y renombrado coinciden.

### [ ] TASK-010 Contrato: openapi - adaptación de revisiones de personaje al catálogo

- **Cubre:** REQ-007, REQ-008, REQ-009, REQ-010, REQ-011, REQ-012
- **Componente:** app | app/
- **Depende de:** TASK-005, TASK-006
- **Alcance:** Crear o actualizar el contrato OpenAPI de revisiones de personaje para actuar sobre el catálogo en lugar del actor de cuento, en `docs/contracts/openapi/paths/actor/` y `docs/contracts/openapi/schemas/`, con los nombres de fichero indicados en el diseño.
- **Hecho cuando:** el contrato existe en `docs/contracts/openapi/` y describe la solicitud, consulta, aceptación y descarte de revisiones sobre el identificador de catálogo, con las respuestas y avisos definidos por la implementación.
- **Pruebas automáticas:**
  - Los contratos OpenAPI afectados son válidos según las reglas de su formato.
  - El contrato de comando de revisión no exige `storyId` y utiliza el identificador de catálogo.
  - El contrato recoge las respuestas de rechazo, confirmación de descarte previo y aviso de consistencia visual.
- **Prueba manual:**
  1. Recorrer el contrato y comprobar que aparecen las cuatro interfaces de revisión de personaje y que todas utilizan el identificador de catálogo.
  2. Comparar cada interfaz y sus errores con la implementación y confirmar que coinciden.

### [ ] TASK-011 Contrato: openapi - referencia al catálogo y separación del snapshot de cuento

- **Cubre:** REQ-006, DEC-001, DEC-005
- **Componente:** app | app/
- **Depende de:** TASK-004
- **Alcance:** Crear o actualizar el contrato OpenAPI de esquemas de actor para incluir la referencia al catálogo y distinguir la instantánea de cuento, en `docs/contracts/openapi/schemas/actor.yaml` y, si procede, los esquemas relacionados indicados por el diseño.
- **Hecho cuando:** el contrato existe en `docs/contracts/openapi/` y representa de forma distinguible el personaje maestro del catálogo y la instantánea usada en cuentos, con la relación requerida para conservar nombres previos.
- **Pruebas automáticas:**
  - El contrato OpenAPI afectado es válido según las reglas de su formato.
  - Los esquemas distinguen el personaje maestro del catálogo y el snapshot de cuento.
  - El esquema del snapshot permite conservar el nombre previo al renombrado del catálogo.
- **Prueba manual:**
  1. Revisar los esquemas y comprobar que la referencia al catálogo y la representación del snapshot están diferenciadas.
  2. Comparar los esquemas con la implementación y confirmar que la separación y las propiedades conservadas coinciden.

## Cobertura

| Requisito | Tareas |
|---|---|
| REQ-001 | TASK-004, TASK-007, TASK-009 |
| REQ-002 | TASK-004, TASK-007, TASK-009 |
| REQ-003 | TASK-001, TASK-003, TASK-007, TASK-009 |
| REQ-004 | TASK-001, TASK-003, TASK-007, TASK-009 |
| REQ-005 | TASK-002, TASK-004, TASK-007, TASK-009 |
| REQ-006 | TASK-004, TASK-009, TASK-011 |
| REQ-007 | TASK-005, TASK-008, TASK-010 |
| REQ-008 | TASK-001, TASK-005, TASK-008, TASK-010 |
| REQ-009 | TASK-002, TASK-005, TASK-008, TASK-010 |
| REQ-010 | TASK-005, TASK-007, TASK-008, TASK-010 |
| REQ-011 | TASK-006, TASK-008, TASK-010 |
| REQ-012 | TASK-006, TASK-008, TASK-010 |

## Avisos

- El mapa actual no contiene entradas para el catálogo ni para el agente de personaje; se usan las rutas sugeridas por el diseño (`agents/character-agent/Modelfile`, `api/src/.../character` y `app/`) y `character-review-service` queda marcado como componente existente sin ruta en el mapa.
- `character-image-condition-port` queda únicamente como interfaz. La implementación del adaptador y la generación o gestión de imágenes quedan fuera de alcance y deben ser aportadas por la feature de assets.
- `.sdd/team.md` no define `contract-strategy`; se aplica `implementation-first` por defecto.
- No existe `.sdd/task-types.md`, por lo que no se han generado tipos de tarea adicionales.
- Las tareas TASK-009 a TASK-011 formalizan las filas de contratos afectados del diseño; no se inventa el contenido operativo de sus ficheros.
