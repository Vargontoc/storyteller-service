# Tareas: character-inventory

Estado: aprobado
RevisiÃ³n: 1
Basado en: requirements@1, design@1

## Resumen

El trabajo se divide entre la definiciÃ³n del agente de personaje, los casos de uso y adaptadores del catÃ¡logo, la gestiÃ³n de revisiones, la interfaz de usuario y la formalizaciÃ³n de los contratos OpenAPI afectados. El puerto de condiciones visuales se define como dependencia de integraciÃ³n, sin implementar la generaciÃ³n ni gestiÃ³n de assets.

## Tareas

### [x] TASK-001 Definir el agente de personaje y su contrato de salida textual

- **Cubre:** REQ-003, REQ-004, REQ-008, DEC-002
- **Componente:** character-agent | api/src/main/java/es/vargontoc/storyteller/infrastructure/adapters/out/external/SpringAiCharacterAgent.java
- **Depende de:** -
- **Alcance:** Definir el agente Ollama propio para generar fichas textuales, validar entradas y proponer revisiones, usando las reglas de edad y especies de la configuraciÃ³n compartida.
- **Hecho cuando:** el agente dispone de una definiciÃ³n cargable con Modelfile y prompts que permiten obtener nombre, descripciÃ³n narrativa y descripciÃ³n visual, ademÃ¡s de distinguir entradas aceptadas y rechazadas con motivo.
- **Pruebas automÃ¡ticas:**
  - Una solicitud de generaciÃ³n automÃ¡tica produce un resultado con nombre, descripciÃ³n narrativa y descripciÃ³n visual vÃ¡lidos.
  - Una entrada vÃ¡lida en modo manual produce una ficha textual completa.
  - Una entrada no vÃ¡lida segÃºn las reglas configuradas produce un rechazo con motivo y no una ficha aplicable.
  - Una solicitud de revisiÃ³n vÃ¡lida produce una propuesta y una solicitud de revisiÃ³n no vÃ¡lida produce un rechazo con motivo.
- **Prueba manual:**
  1. Cargar el modelo del agente de personaje segÃºn el procedimiento de los mÃ³dulos `agents`.
  2. Solicitar una ficha automÃ¡tica y comprobar que incluye nombre, descripciÃ³n narrativa y descripciÃ³n visual.
  3. Someter una frase apta y otra no apta para el pÃºblico infantil y comprobar que la apta se completa y la no apta devuelve el motivo del rechazo.

### [x] TASK-002 Definir el puerto de condiciones visuales de personaje

- **Cubre:** REQ-005, REQ-009, DEC-003, DEC-004, ALT-003
- **Componente:** character-image-condition-port | api/src/main/java/es/vargontoc/storyteller/application/ports/out/external/CharacterImageConditionPort.java
- **Depende de:** -
- **Alcance:** Definir la interfaz de salida que permite consultar si existe un retrato aprobado y si existen imÃ¡genes generadas para un personaje del catÃ¡logo, dejando su implementaciÃ³n fuera de esta feature.
- **Hecho cuando:** el contrato de la interfaz permite consultar ambas condiciones por identificador de personaje sin depender de una implementaciÃ³n concreta de assets.
- **Pruebas automÃ¡ticas:**
  - No aplica: esta tarea define una interfaz de puerto; sus implementaciones pertenecen a la feature de assets.
- **Prueba manual:**
  1. Revisar la interfaz y comprobar que expone consultas separadas para retrato aprobado e imÃ¡genes generadas.
  2. Comprobar que el puerto no incluye operaciones para generar, seleccionar, almacenar o modificar imÃ¡genes.

### [x] TASK-003 Implementar el alta y la validaciÃ³n de fichas en el catÃ¡logo

- **Cubre:** REQ-003, REQ-004, ALT-001, DEC-002
- **Componente:** character-catalog-service | api/src/main/java/es/vargontoc/storyteller/application/service/CharacterCatalogService.java
- **Depende de:** TASK-001, TASK-002
- **Alcance:** Implementar los casos de uso de creaciÃ³n automÃ¡tica y manual mediante el agente, validando la salida y persistiendo una ficha completa solo cuando sea aceptada.
- **Hecho cuando:** se cumplen los criterios de aceptaciÃ³n de REQ-003 y REQ-004 y el catÃ¡logo crea un personaje con nombre, descripciÃ³n narrativa y descripciÃ³n visual, sin persistir nada ante un rechazo.
- **Pruebas automÃ¡ticas:**
  - En modo automÃ¡tico, una respuesta aceptada del agente produce una ficha textual completa y persistida.
  - En modo manual, una frase vÃ¡lida produce una ficha textual completa y persistida.
  - En modo manual o automÃ¡tico, una respuesta rechazada devuelve el motivo, no crea el personaje y no modifica el catÃ¡logo.
  - Una salida del agente a la que le falte cualquiera de los campos obligatorios se rechaza sin persistir una ficha incompleta.
- **Prueba manual:**
  1. Crear un personaje en modo automÃ¡tico sin introducir texto y verificar que se obtiene una ficha completa.
  2. Crear un personaje con una frase que incluya nombre y descripciÃ³n y verificar que el agente completa la ficha.
  3. Intentar un alta no apta y verificar que se muestra el motivo y no aparece el personaje en el catÃ¡logo.

### [x] TASK-004 Implementar el listado, detalle, estado derivado y renombrado del catÃ¡logo

- **Cubre:** REQ-001, REQ-002, REQ-005, REQ-006, DEC-001, DEC-003, DEC-005
- **Componente:** character-catalog-service | api/src/main/java/es/vargontoc/storyteller/application/service/CharacterCatalogService.java
- **Depende de:** TASK-002, TASK-003
- **Alcance:** Implementar la consulta del catÃ¡logo y de la ficha, el cÃ¡lculo derivado del estado de borrador y el renombrado limitado al personaje maestro del catÃ¡logo.
- **Hecho cuando:** se cumplen los criterios de aceptaciÃ³n de REQ-001, REQ-002, REQ-005 y REQ-006, el listado incluye nombre, estado y situaciÃ³n de revisiÃ³n, y el renombrado no modifica instantÃ¡neas ya existentes.
- **Pruebas automÃ¡ticas:**
  - El listado devuelve todos los personajes con nombre y estado, y una colecciÃ³n vacÃ­a cuando no hay personajes.
  - El detalle devuelve nombre, descripciÃ³n narrativa y descripciÃ³n visual.
  - Un personaje sin retrato aprobado aparece como borrador tanto en listado como en detalle; con retrato aprobado no aparece como borrador.
  - Renombrar actualiza el nombre del catÃ¡logo y no altera el nombre de ninguna instantÃ¡nea de cuento asociada.
- **Prueba manual:**
  1. Abrir el catÃ¡logo con dos personajes y comprobar que aparecen sus nombres y estados.
  2. Abrir el catÃ¡logo vacÃ­o y comprobar que se muestra el indicador de catÃ¡logo vacÃ­o.
  3. Seleccionar un personaje y revisar que se muestran nombre, descripciÃ³n narrativa, descripciÃ³n visual y estado.
  4. Cambiar el nombre de un personaje y comprobar que la ficha muestra el nuevo nombre.

### [x] TASK-005 Adaptar el servicio de revisiones al catÃ¡logo y al agente de personaje

- **Cubre:** REQ-007, REQ-008, REQ-009, ALT-001, ALT-002, ALT-003, DEC-002, DEC-004
- **Componente:** character-review-service | no consta en el mapa
- **Depende de:** TASK-001, TASK-002, TASK-003
- **Alcance:** Implementar la solicitud, consulta y persistencia de revisiones de personajes del catÃ¡logo, incluyendo validaciÃ³n de cambios, descarte de una revisiÃ³n previa confirmada y el aviso de consistencia visual.
- **Hecho cuando:** se cumplen los criterios de aceptaciÃ³n de REQ-007, REQ-008 y REQ-009, las revisiones vÃ¡lidas quedan pendientes y solo hay una revisiÃ³n pendiente por personaje.
- **Pruebas automÃ¡ticas:**
  - Una solicitud sin revisiÃ³n previa se procesa directamente y queda en estado pendiente.
  - Una solicitud con revisiÃ³n previa devuelve el aviso de confirmaciÃ³n; al confirmar, la previa queda descartada y la nueva se procesa como pendiente.
  - Un cambio no vÃ¡lido devuelve el motivo del rechazo y no crea una revisiÃ³n pendiente.
  - Un cambio vÃ¡lido crea una revisiÃ³n pendiente con la propuesta del agente.
  - Un cambio visual con imÃ¡genes generadas incluye el aviso de consistencia visual.
  - Un cambio visual sin imÃ¡genes generadas no incluye ese aviso.
  - Un cambio solo narrativo con imÃ¡genes generadas no incluye el aviso de consistencia visual.
- **Prueba manual:**
  1. Solicitar una revisiÃ³n de un personaje sin revisiÃ³n previa y comprobar que se muestra directamente la propuesta pendiente.
  2. Repetir la solicitud y confirmar el aviso; comprobar que la revisiÃ³n anterior queda descartada y la nueva pendiente.
  3. Enviar un cambio no apto y comprobar que se muestra el motivo sin crear revisiÃ³n.
  4. Revisar un cambio visual de un personaje con imÃ¡genes y comprobar que la propuesta aparece acompaÃ±ada del aviso de consistencia visual.

### [ ] TASK-006 Implementar la aceptaciÃ³n y el descarte de revisiones del catÃ¡logo

- **Cubre:** REQ-011, REQ-012
- **Componente:** character-review-service | no consta en el mapa
- **Depende de:** TASK-004, TASK-005
- **Alcance:** Implementar la aplicaciÃ³n de una propuesta aceptada al texto del personaje y el descarte de una revisiÃ³n conservando intacta la ficha vigente.
- **Hecho cuando:** se cumplen los criterios de aceptaciÃ³n de REQ-011 y REQ-012, la revisiÃ³n deja de estar pendiente en ambos casos y solo la aceptaciÃ³n modifica el texto del catÃ¡logo.
- **Pruebas automÃ¡ticas:**
  - Aceptar una revisiÃ³n narrativa actualiza la descripciÃ³n narrativa de la ficha y cambia el estado de la revisiÃ³n a confirmado.
  - Aceptar una revisiÃ³n visual actualiza la descripciÃ³n visual de la ficha y cambia el estado de la revisiÃ³n a confirmado.
  - Descartar una revisiÃ³n conserva sin cambios nombre, descripciÃ³n narrativa y descripciÃ³n visual y cambia el estado de la revisiÃ³n a descartado.
- **Prueba manual:**
  1. Abrir una revisiÃ³n pendiente que cambie la descripciÃ³n narrativa y pulsar "Aceptar"; comprobar que la ficha muestra el texto nuevo y desaparece el badge.
  2. Abrir otra revisiÃ³n pendiente y pulsar "Descartar"; comprobar que la ficha conserva su texto y desaparece el badge.

### [ ] TASK-007 Construir la vista del catÃ¡logo y el modal de alta

- **Cubre:** REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-010, ALT-001
- **Componente:** character-catalog-view | app/
- **Depende de:** TASK-003, TASK-004
- **Alcance:** Construir la vista principal del catÃ¡logo, la selecciÃ³n de ficha y el modal de alta automÃ¡tica o manual, incluyendo estados vacÃ­os, errores de rechazo, borrador y badges de revisiÃ³n.
- **Hecho cuando:** se cumplen los criterios de aceptaciÃ³n de REQ-001, REQ-002, REQ-003, REQ-004, REQ-005 y REQ-010 en el flujo de usuario del catÃ¡logo.
- **Pruebas automÃ¡ticas:**
  - No aplica: la verificaciÃ³n del flujo visual de la SPA requiere pruebas manuales hasta que el proyecto disponga de cobertura automatizada para este componente.
- **Prueba manual:**
  1. Abrir el catÃ¡logo vacÃ­o y comprobar que aparece el indicador de que no hay personajes.
  2. Abrir el catÃ¡logo con personajes y comprobar que cada tarjeta o fila muestra nombre, estado y, si corresponde, badge de revisiÃ³n.
  3. Seleccionar un personaje y comprobar que se ven nombre, descripciÃ³n narrativa, descripciÃ³n visual y el estado de borrador cuando procede.
  4. Crear un personaje en modo automÃ¡tico y comprobar que se presenta una ficha completa.
  5. Crear un personaje en modo manual con una frase vÃ¡lida y comprobar que se presenta una ficha completa.
  6. Enviar una frase no vÃ¡lida y comprobar que se muestra el motivo del rechazo y no se crea el personaje.
  7. Comprobar que una ficha con revisiÃ³n pendiente muestra el badge tanto en el catÃ¡logo como junto a la secciÃ³n de revisiÃ³n.

### [ ] TASK-008 Integrar el panel de revisiÃ³n en el catÃ¡logo

- **Cubre:** REQ-007, REQ-008, REQ-009, REQ-010, REQ-011, REQ-012, ALT-002, ALT-003
- **Componente:** character-review-panel | app/
- **Depende de:** TASK-005, TASK-006, TASK-007
- **Alcance:** Integrar en la ficha del catÃ¡logo la solicitud de revisiÃ³n, la propuesta y su diff, la confirmaciÃ³n de descarte de una previa, los avisos aplicables y las acciones de aceptar o descartar.
- **Hecho cuando:** se cumplen los criterios de aceptaciÃ³n de REQ-007 a REQ-012 y el usuario puede completar desde la ficha todo el ciclo de revisiÃ³n sin salir del catÃ¡logo.
- **Pruebas automÃ¡ticas:**
  - No aplica: la verificaciÃ³n del flujo visual de la SPA requiere pruebas manuales hasta que el proyecto disponga de cobertura automatizada para este componente.
- **Prueba manual:**
  1. Enviar un cambio vÃ¡lido y comprobar que se muestran la propuesta pendiente y el diff.
  2. Enviar un cambio no vÃ¡lido y comprobar que aparece el motivo del rechazo.
  3. Solicitar una revisiÃ³n cuando ya existe una pendiente, confirmar el aviso y comprobar que la nueva queda pendiente y la previa descartada.
  4. Comprobar que el aviso de consistencia visual aparece junto a la propuesta cuando se cumplen sus condiciones.
  5. Aceptar la revisiÃ³n y comprobar que se aplica a la ficha y desaparece el badge.
  6. Descartar la revisiÃ³n y comprobar que se conserva la ficha y desaparece el badge.

### [ ] TASK-009 Contrato: openapi - recurso de catÃ¡logo de personajes

- **Cubre:** REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006
- **Componente:** app | app/
- **Depende de:** TASK-003, TASK-004
- **Alcance:** Crear o actualizar el contrato OpenAPI del recurso de catÃ¡logo de personajes para listado, detalle, alta automÃ¡tica o manual y renombrado, en `docs/contracts/openapi/`, con el nombre del fichero por definir (analista tÃ©cnico).
- **Hecho cuando:** el contrato existe en `docs/contracts/openapi/` y describe el recurso de catÃ¡logo y sus operaciones con los datos y respuestas definidos por la implementaciÃ³n, incluidas las respuestas de rechazo requeridas.
- **Pruebas automÃ¡ticas:**
  - El contrato OpenAPI es vÃ¡lido segÃºn las reglas de su formato.
  - El contrato recoge las operaciones de listado, detalle, alta automÃ¡tica, alta manual y renombrado.
  - El contrato recoge una respuesta de rechazo con motivo para las altas no vÃ¡lidas.
- **Prueba manual:**
  1. Recorrer el contrato y comprobar que cada interfaz de esta tarea aparece con su mÃ©todo, entrada, salida y errores relevantes.
  2. Comparar el contrato con la implementaciÃ³n y confirmar que listado, detalle, alta automÃ¡tica, alta manual y renombrado coinciden.

### [ ] TASK-010 Contrato: openapi - adaptaciÃ³n de revisiones de personaje al catÃ¡logo

- **Cubre:** REQ-007, REQ-008, REQ-009, REQ-010, REQ-011, REQ-012
- **Componente:** app | app/
- **Depende de:** TASK-005, TASK-006
- **Alcance:** Crear o actualizar el contrato OpenAPI de revisiones de personaje para actuar sobre el catÃ¡logo en lugar del actor de cuento, en `docs/contracts/openapi/paths/actor/` y `docs/contracts/openapi/schemas/`, con los nombres de fichero indicados en el diseÃ±o.
- **Hecho cuando:** el contrato existe en `docs/contracts/openapi/` y describe la solicitud, consulta, aceptaciÃ³n y descarte de revisiones sobre el identificador de catÃ¡logo, con las respuestas y avisos definidos por la implementaciÃ³n.
- **Pruebas automÃ¡ticas:**
  - Los contratos OpenAPI afectados son vÃ¡lidos segÃºn las reglas de su formato.
  - El contrato de comando de revisiÃ³n no exige `storyId` y utiliza el identificador de catÃ¡logo.
  - El contrato recoge las respuestas de rechazo, confirmaciÃ³n de descarte previo y aviso de consistencia visual.
- **Prueba manual:**
  1. Recorrer el contrato y comprobar que aparecen las cuatro interfaces de revisiÃ³n de personaje y que todas utilizan el identificador de catÃ¡logo.
  2. Comparar cada interfaz y sus errores con la implementaciÃ³n y confirmar que coinciden.

### [ ] TASK-011 Contrato: openapi - referencia al catÃ¡logo y separaciÃ³n del snapshot de cuento

- **Cubre:** REQ-006, DEC-001, DEC-005
- **Componente:** app | app/
- **Depende de:** TASK-004
- **Alcance:** Crear o actualizar el contrato OpenAPI de esquemas de actor para incluir la referencia al catÃ¡logo y distinguir la instantÃ¡nea de cuento, en `docs/contracts/openapi/schemas/actor.yaml` y, si procede, los esquemas relacionados indicados por el diseÃ±o.
- **Hecho cuando:** el contrato existe en `docs/contracts/openapi/` y representa de forma distinguible el personaje maestro del catÃ¡logo y la instantÃ¡nea usada en cuentos, con la relaciÃ³n requerida para conservar nombres previos.
- **Pruebas automÃ¡ticas:**
  - El contrato OpenAPI afectado es vÃ¡lido segÃºn las reglas de su formato.
  - Los esquemas distinguen el personaje maestro del catÃ¡logo y el snapshot de cuento.
  - El esquema del snapshot permite conservar el nombre previo al renombrado del catÃ¡logo.
- **Prueba manual:**
  1. Revisar los esquemas y comprobar que la referencia al catÃ¡logo y la representaciÃ³n del snapshot estÃ¡n diferenciadas.
  2. Comparar los esquemas con la implementaciÃ³n y confirmar que la separaciÃ³n y las propiedades conservadas coinciden.

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

- El mapa actual no contiene entradas para el catÃ¡logo ni para el agente de personaje; se usan las rutas sugeridas por el diseÃ±o (`agents/character-agent/Modelfile`, `api/src/.../character` y `app/`) y `character-review-service` queda marcado como componente existente sin ruta en el mapa.
- `character-image-condition-port` queda Ãºnicamente como interfaz. La implementaciÃ³n del adaptador y la generaciÃ³n o gestiÃ³n de imÃ¡genes quedan fuera de alcance y deben ser aportadas por la feature de assets.
- `.sdd/team.md` no define `contract-strategy`; se aplica `implementation-first` por defecto.
- No existe `.sdd/task-types.md`, por lo que no se han generado tipos de tarea adicionales.
- Las tareas TASK-009 a TASK-011 formalizan las filas de contratos afectados del diseÃ±o; no se inventa el contenido operativo de sus ficheros.
