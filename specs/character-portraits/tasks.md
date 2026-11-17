# Tareas: character-portraits

Estado: aprobado
Revisión: 1
Basado en: requirements@2, design@2

## Resumen

El trabajo se divide entre la persistencia de retratos, candidatos y trabajos de generación, la orquestación de la generación de 4 candidatos cartoon en ComfyUI, el bloqueo derivado del estado del trabajo, la notificación por WebSocket, la selección del retrato aprobado, la invalidación y el fork ante cambios visuales, la interfaz de usuario y la formalización de los tres contratos OpenAPI afectados. El lock no se persiste como fuente de verdad: se deriva de la existencia de un `PortraitGenerationJob` en estado `RUNNING` y los jobs huérfanos se reconcilian al arrancar.

## Tareas

### [x] TASK-001 Persistir retrato aprobado, candidatos, trabajos de generación y relación de fork

- **Cubre:** REQ-003, REQ-010, REQ-012, DEC-004, DEC-006
- **Componente:** character-repository | api/src/main/java/es/vargontoc/storyteller/application/ports/out/persistence/CatalogCharacterRepository.java
- **Depende de:** -
- **Alcance:** Modificar el repositorio del catálogo para persistir el retrato aprobado de cada personaje, los candidatos pendientes de selección, los trabajos de generación con sus estados `RUNNING`/`COMPLETED`/`FAILED` y la relación entre un personaje original y sus forks.
- **Hecho cuando:** el repositorio permite guardar y consultar el retrato aprobado, registrar y eliminar candidatos, crear y actualizar trabajos de generación con su estado y personaje asociado, y registrar el fork conservando el personaje original intacto.
- **Pruebas automáticas:**
  - Un personaje con retrato aprobado persiste y recupera su retrato asociado.
  - Almacenar 4 candidatos de un personaje permite consultarlos y eliminarlos de forma individual o conjunta.
  - Un trabajo de generación creado en `RUNNING` se recupera con ese estado y se puede actualizar a `COMPLETED` o `FAILED`.
  - Un personaje con retrato y descripciones existentes y al que se le añade una relación de fork conserva sus datos originales y permite consultar sus forks.
- **Prueba manual:**
  1. Generar un retrato para un personaje y comprobar que el retrato aprobado queda asociado a ese personaje.
  2. Consultar la base de datos y verificar que existe el trabajo de generación con su estado y su personaje asociado.
  3. Crear un fork y comprobar que el personaje original conserva nombre, descripciones y retrato.

### [x] TASK-002 Implementar el almacén de estado de los trabajos de generación

- **Cubre:** REQ-006, REQ-007, DEC-008
- **Componente:** portrait-generation-state-store | api/src/main/java/es/vargontoc/storyteller/application/ports/out/persistence/PortraitGenerationJobStore.java
- **Depende de:** TASK-001
- **Alcance:** Crear el almacén que persiste los trabajos de generación de retratos y permite consultar los trabajos en curso y los jobs `RUNNING` huérfanos.
- **Hecho cuando:** se puede crear un trabajo en estado `RUNNING` para un personaje, actualizar su estado a `COMPLETED` o `FAILED`, consultar el trabajo en curso de un personaje y listar los trabajos `RUNNING` existentes.
- **Pruebas automáticas:**
  - Crear un trabajo para un personaje devuelve el trabajo con estado `RUNNING` y ese mismo personaje asociado.
  - Actualizar el trabajo a `COMPLETED` y a `FAILED` se persiste y se recupera con el estado final correspondiente.
  - Consultar el trabajo en curso de un personaje sin trabajos devuelve que no hay ninguno en curso.
  - Listar los trabajos `RUNNING` devuelve solo los que siguen en ese estado, excluyendo los completados y fallidos.
- **Prueba manual:**
  1. Solicitar la generación de retratos de un personaje y comprobar que queda registrado un trabajo en curso.
  2. Dejar terminar la generación y comprobar que el trabajo deja de estar en curso.

### [x] TASK-003 Implementar el almacén de candidatos de retrato

- **Cubre:** REQ-001, REQ-003, REQ-004, DEC-004
- **Componente:** portrait-candidate-store | api/src/main/java/es/vargontoc/storyteller/application/ports/out/persistence/PortraitCandidateStore.java
- **Depende de:** TASK-001
- **Alcance:** Crear el almacén que guarda temporalmente las propuestas generadas de un personaje, las asocia al personaje y permite reemplazarlas o eliminararlas.
- **Hecho cuando:** se pueden guardar 4 candidatos asociados a un personaje, consultarlos, reemplazar el conjunto completo por una nueva generación y eliminar candidatos concretos o todos.
- **Pruebas automáticas:**
  - Guardar 4 candidatos para un personaje permite recuperar los 4 asociados a ese personaje.
  - Un personaje sin candidatos devuelve una colección vacía.
  - Reemplazar los candidatos de un personaje deja únicamente los nuevos y descarta los anteriores.
  - Eliminar un candidato concreto deja el resto de candidatos del personaje intactos.
- **Prueba manual:**
  1. Generar los retratos de un personaje y comprobar que las 4 propuestas quedan disponibles para su ficha.
  2. Regenerar y comprobar que las propuestas mostradas son las nuevas y las anteriores ya no están.

### [x] TASK-004 Construir el adaptador de ComfyUI para lotes de 4 retratos cartoon

- **Cubre:** REQ-001, REQ-002, DEC-002
- **Componente:** portrait-comfyui-adapter | api/src/main/java/es/vargontoc/storyteller/infrastructure/adapters/out/external/PortraitComfyUiAdapter.java
- **Depende de:** -
- **Alcance:** Crear el adaptador que construye y envía a ComfyUI un workflow de 4 retratos cartoon en una sola orquestación a partir de la descripción visual del personaje, variando seeds y prompts para obtener diversidad manteniendo la identidad, y devuelve los 4 assets.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-001 y REQ-002, el adaptador encola un único workflow que produce 4 imágenes con estilo cartoon coherente derivadas de la descripción visual del personaje y devuelve las 4 imágenes descargadas.
- **Pruebas automáticas:**
  - Una solicitud de generación produce exactamente 4 imágenes descargadas de ComfyUI.
  - Las 4 imágenes de un mismo lote comparten la configuración de estilo cartoon y se diferencian entre sí en seed o prompt.
  - La descripción visual del personaje se traslada al prompt enviado a ComfyUI.
  - Un resultado incompleto de ComfyUI devuelve el error y no un conjunto de 4 imágenes.
- **Prueba manual:**
  1. Generar los retratos de un personaje y comprobar que se obtienen 4 imágenes distintas entre sí.
  2. Comparar dos retratos del mismo personaje y comprobar que comparten un estilo cartoon reconocible.
  3. Revisar el contenido de las 4 imágenes y comprobar que no incluye elementos inapropiados para la edad objetivo.

### [x] TASK-005 Implementar el notificador de eventos de retrato por WebSocket

- **Cubre:** REQ-005, DEC-005
- **Componente:** portrait-websocket-notifier | api/src/main/java/es/vargontoc/storyteller/application/service/PortraitWebsocketNotifier.java
- **Depende de:** -
- **Alcance:** Crear el notificador que emite en el canal `/storyteller` los eventos de inicio, progreso, finalización con éxito y error de la generación de retratos, incluyendo siempre el identificador del personaje.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-005, el notificador publica un evento de inicio, uno de finalización con identificador de personaje y estado de éxito, y uno de error con el motivo del fallo.
- **Pruebas automáticas:**
  - El inicio de una generación emite un evento de inicio con el identificador del personaje.
  - La finalización correcta emite un evento de finalización con identificador de personaje y estado de éxito.
  - Un fallo emite un evento de error con el motivo del fallo.
  - Los eventos de retrato se publican en el canal `/storyteller` con el identificador del personaje y no en un canal privado por sesión.
- **Prueba manual:**
  1. Solicitar la generación de retratos y comprobar que llega la notificación de inicio en la aplicación.
  2. Esperar el final de la generación y comprobar que llega la notificación de finalización con el personaje correcto.
  3. Provocar un fallo de generación y comprobar que llega la notificación de error con el motivo.

### [x] TASK-006 Implementar el bloqueo del personaje derivado del trabajo de generación

- **Cubre:** REQ-006, REQ-007, ALT-003, DEC-003, DEC-008
- **Componente:** portrait-lock-service | api/src/main/java/es/vargontoc/storyteller/application/service/PortraitLockService.java
- **Depende de:** TASK-002
- **Alcance:** Crear el servicio que expone si un personaje está bloqueado a partir de la existencia de un trabajo de generación en estado `RUNNING` y que libera el bloqueo cuando el trabajo deja de estar en curso, sin introducir ningún timeout de generación.
- **Hecho cuando:** un personaje con un trabajo `RUNNING` se reporta como bloqueado, el mismo personaje deja de estar bloqueado cuando su trabajo pasa a `COMPLETED` o `FAILED`, y el bloqueo sobrevive a un reinicio porque se deriva del estado persistido y no de memoria ni de expiración.
- **Pruebas automáticas:**
  - Un personaje con trabajo `RUNNING` se informa como bloqueado.
  - Un personaje sin trabajo en curso se informa como no bloqueado.
  - Un trabajo que pasa a `COMPLETED` libera el bloqueo del personaje.
  - Un trabajo que pasa a `FAILED` libera el bloqueo del personaje.
  - El bloqueo no se libera por el mero paso del tiempo: un trabajo `RUNNING` sigue bloqueando el personaje tras un intervalo de espera prolongado.
  - Una consulta de bloqueo no crea ni modifica trabajos de generación.
- **Prueba manual:**
  1. Iniciar la generación de un personaje y comprobar que su ficha muestra el indicador de generación en curso.
  2. Esperar a que termine la generación y comprobar que la ficha ya no muestra la generación en curso.
  3. Comprobar que la generación en curso no se cancela sola por esperar demasiado tiempo.

### [x] TASK-007 Implementar la orquestación de la generación de 4 candidatos

- **Cubre:** REQ-001, REQ-004, REQ-005, REQ-006, REQ-007, ALT-001, ALT-002, ALT-004, DEC-002
- **Componente:** portrait-generation-service | api/src/main/java/es/vargontoc/storyteller/application/service/PortraitGenerationService.java
- **Depende de:** TASK-002, TASK-003, TASK-004, TASK-005, TASK-006
- **Alcance:** Crear el servicio que atiende la solicitud de generar o regenerar retratos: crea el trabajo en curso, delega en el adaptador de ComfyUI, guarda los candidatos, actualiza el estado del trabajo, deja el personaje sin propuestas ante fallo y notifica por WebSocket, sin introducir ningún timeout de generación.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-001, REQ-004, REQ-005 y REQ-007, el servicio produce 4 candidatos por solicitud, permite regenerar sin límite de repeticiones reemplazando las propuestas anteriores, y ante un fallo deja el trabajo en `FAILED`, el personaje sin propuestas y el bloqueo liberado.
- **Pruebas automáticas:**
  - Una solicitud de generación crea un trabajo en `RUNNING`, obtiene 4 candidatos del adaptador de ComfyUI, los guarda y deja el trabajo en `COMPLETED`.
  - Una solicitud de generación sobre un personaje ya bloqueado se rechaza y no crea un segundo trabajo.
  - Una regeneración crea un trabajo nuevo y deja únicamente los 4 candidatos nuevos.
  - Varias regeneraciones consecutivas se atienden todas sin establecer ningún límite de regeneraciones.
  - Un error del adaptador de ComfyUI deja el trabajo en `FAILED`, no guarda candidatos y notifica el fallo con su motivo.
  - Un resultado incompleto de ComfyUI produce el mismo fallo que un error explícito.
- **Prueba manual:**
  1. Pulsar "Generar retratos" en la ficha de un personaje y comprobar que se obtienen 4 propuestas.
  2. Regenerar varias veces seguidas y comprobar que en cada ciclo se obtienen 4 propuestas nuevas y distintas.
  3. Con ComfyUI no disponible, solicitar la generación y comprobar que se notifica el error y el personaje queda sin propuestas.

### [x] TASK-008 Implementar la selección del retrato aprobado

- **Cubre:** REQ-003, REQ-008, ALT-005, DEC-004
- **Componente:** portrait-selection-service | api/src/main/java/es/vargontoc/storyteller/application/service/PortraitSelectionService.java
- **Depende de:** TASK-001, TASK-003
- **Alcance:** Crear el servicio que registra el candidato elegido como retrato aprobado del personaje, elimina las tres propuestas restantes y rechaza la selección de candidatos que ya no existen o han sido invalidados.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-003, el candidato seleccionado queda como único retrato aprobado, las otras 3 propuestas se eliminan del almacenamiento y el personaje deja de estar en estado borrador.
- **Pruebas automáticas:**
  - Seleccionar un candidato de entre 4 lo registra como retrato aprobado del personaje.
  - Tras la selección solo queda el retrato aprobado y las 3 propuestas descartadas se eliminan.
  - El personaje con retrato aprobado deja de aparecer como borrador.
  - Seleccionar un candidato que ya no existe se rechaza y no modifica el retrato aprobado existente.
  - Seleccionar un candidato invalidado por un cambio visual se rechaza y no modifica el retrato aprobado existente.
- **Prueba manual:**
  1. Elegir una de las 4 propuestas y comprobar que la ficha muestra ese retrato como aprobado.
  2. Comprobar que en el almacenamiento solo queda la imagen del retrato aprobado.
  3. Con la galería abierta, regenerar desde otra sesión e intentar seleccionar una propuesta antigua y comprobar que se rechaza.

### [x] TASK-009 Implementar el adaptador de condiciones visuales de personaje

- **Cubre:** REQ-008, REQ-009, REQ-010, DEC-001
- **Componente:** portrait-condition-adapter | api/src/main/java/es/vargontoc/storyteller/infrastructure/adapters/out/persistence/PortraitConditionAdapter.java
- **Depende de:** TASK-001
- **Alcance:** Crear la implementación real del puerto `character-image-condition-port` que consulta si un personaje tiene retrato aprobado y si tiene imágenes generadas, alimentando el estado de borrador, el aviso de consistencia visual y la invalidación.
- **Hecho cuando:** el puerto `character-image-condition-port` deja de ser una interfaz vacía y responde, por identificador de personaje, si existe retrato aprobado y si existen imágenes generadas.
- **Pruebas automáticas:**
  - Un personaje con retrato aprobado responde que tiene retrato aprobado.
  - Un personaje sin retrato aprobado responde que no lo tiene.
  - Un personaje con imágenes generadas responde que tiene imágenes generadas.
  - Un personaje sin imágenes generadas responde que no las tiene.
- **Prueba manual:**
  1. Revisar la implementación del puerto y comprobar que cubre las dos consultas definidas en la interfaz.
  2. Comprobar que no expone operaciones de generación, selección ni modificación de imágenes.

### [x] TASK-010 Implementar la creación del fork de personaje

- **Cubre:** REQ-012, REQ-013, ALT-007, DEC-006
- **Componente:** portrait-fork-service | api/src/main/java/es/vargontoc/storyteller/application/service/PortraitForkService.java
- **Depende de:** TASK-001
- **Alcance:** Crear el servicio que, ante un cambio visual confirmado en un personaje en uso, crea un nuevo personaje del catálogo con la ficha copiada, el nombre original más un sufijo distintivo, la descripción visual revisada y sin retrato aprobado, dejando el original inalterable.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-012 y REQ-013, el fork aparece en el catálogo como borrador con la descripción visual revisada, el original conserva nombre, descripción y retrato, y el fork puede renombrarse sin afectar al original ni a los cuentos asociados.
- **Pruebas automáticas:**
  - Confirmar un cambio visual en un personaje en uso crea un nuevo personaje con la descripción visual revisada.
  - El nombre del nuevo personaje es el del original más un sufijo distintivo.
  - El nuevo personaje no tiene retrato aprobado y aparece en estado borrador.
  - El personaje original conserva sin cambios nombre, descripciones, retrato aprobado e imágenes previas.
  - Renombrar el fork actualiza solo el nombre del fork y no altera el nombre del original.
  - Renombrar el fork no altera los nombres de las instantáneas de los cuentos asociados.
- **Prueba manual:**
  1. Confirmar un cambio visual en un personaje usado en un cuento y comprobar que aparece un personaje nuevo en el catálogo con otro nombre.
  2. Abrir el fork y comprobar que está en estado borrador y con la descripción visual revisada.
  3. Abrir el original y comprobar que conserva su retrato y su nombre.
  4. Renombrar el fork y comprobar que el original mantiene su nombre y que los cuentos previos no cambian.

### [x] TASK-011 Implementar la reconciliación de trabajos huérfanos al arrancar

- **Cubre:** REQ-005, REQ-006, REQ-007, ALT-004, DEC-008
- **Componente:** portrait-reconciliation-service | api/src/main/java/es/vargontoc/storyteller/application/service/PortraitGenerationReconciliationService.java
- **Depende de:** TASK-002, TASK-005, TASK-006
- **Alcance:** Crear el servicio que, al arrancar la aplicación, detecta los trabajos de generación en estado `RUNNING` sin tarea viva asociada, los marca como `FAILED`, libera el bloqueo del personaje y emite el evento de error por WebSocket.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-005, REQ-006 y REQ-007 en el escenario de reinicio, ningún personaje queda bloqueado indefinidamente tras una caída del servidor y el usuario recibe el evento de error correspondiente.
- **Pruebas automáticas:**
  - Un trabajo `RUNNING` sin tarea viva se marca como `FAILED` al arrancar.
  - Tras la reconciliación, el personaje asociado a ese trabajo deja de estar bloqueado.
  - La reconciliación emite un evento de error por WebSocket con el identificador del personaje.
  - Un trabajo ya `COMPLETED` o `FAILED` no se modifica.
  - Un trabajo `RUNNING` cuya tarea sigue viva se conserva sin alterar.
- **Prueba manual:**
  1. Iniciar una generación, detener el servidor antes de que termine y arrancarlo de nuevo.
  2. Comprobar que el personaje vuelve a estar editable y que se recibe la notificación de error.

### [ ] TASK-012 Modificar el servicio de catálogo para la generación, consulta de candidatos y fork

- **Cubre:** REQ-003, REQ-004, REQ-006, REQ-008, REQ-012, REQ-013, ALT-003, ALT-006, DEC-001, DEC-006
- **Componente:** character-catalog-service | api/src/main/java/es/vargontoc/storyteller/application/service/CharacterCatalogService.java
- **Depende de:** TASK-007, TASK-008, TASK-009, TASK-010
- **Alcance:** Modificar el servicio de catálogo para solicitar la generación y la regeneración de retratos, consultar los candidatos y el estado de generación, delegar la selección del retrato, derivar el estado de borrador del retrato aprobado real, rechazar el uso del personaje como protagonista de un cuento nuevo mientras esté bloqueado y exponer el renombrado del fork.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-003, REQ-004, REQ-006, REQ-008, REQ-012 y REQ-013, el catálogo muestra el estado de generación en curso, refleja el borrador a partir del retrato aprobado real y bloquea el uso del personaje en cuentos nuevos mientras dura la generación.
- **Pruebas automáticas:**
  - Una solicitud de generación delega en el servicio de generación y devuelve el personaje con el estado de generación en curso.
  - Una solicitud de regeneración delega en el servicio de generación reemplazando las propuestas anteriores.
  - La consulta de candidatos devuelve las propuestas vigentes del personaje y una colección vacía cuando no hay ninguna.
  - La consulta del personaje en bloqueo informa del estado de generación en curso y el progreso disponible.
  - Un personaje sin retrato aprobado aparece como borrador y uno con retrato aprobado no.
  - Con un trabajo en curso, el intento de usar el personaje como protagonista de un cuento nuevo se rechaza.
  - Con un trabajo en curso, el intento de renombrar el personaje se rechaza.
  - Sin trabajo en curso, el renombrado del personaje y del fork se permite y solo afecta al personaje indicado.
- **Prueba manual:**
  1. Abrir la ficha de un personaje y comprobar que aparece el botón de generar retratos y el estado de generación cuando está en curso.
  2. Con la generación en curso, intentar renombrar el personaje y comprobar que se rechaza con el indicador de generación en curso.
  3. Con la generación en curso, intentar elegir el personaje como protagonista de un cuento nuevo y comprobar que no se permite.
  4. Renombrar un fork y comprobar que el catálogo muestra el nuevo nombre y el original no cambia.

### [ ] TASK-013 Modificar el servicio de revisiones para la invalidación y el aviso de fork

- **Cubre:** REQ-006, REQ-008, REQ-009, REQ-010, REQ-011, ALT-006, ALT-007, DEC-001, DEC-006, DEC-007
- **Componente:** character-review-service | api/src/main/java/es/vargontoc/storyteller/application/service/CharacterReviewService.java
- **Depende de:** TASK-006, TASK-009, TASK-010
- **Alcance:** Modificar el servicio de revisiones para rechazar nuevas revisiones y la confirmación de revisiones pendientes mientras el personaje esté bloqueado, mantener el aviso de consistencia visual usando las condiciones reales, invalidar el retrato y las imágenes generadas ante un cambio visual confirmado en un personaje no usado, y lanzar la creación del fork con aviso previo ante un cambio visual confirmado en un personaje en uso.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-006, REQ-008, REQ-009, REQ-010 y REQ-011, un cambio visual confirmado en un personaje no usado invalida su retrato y lo devuelve al estado borrador, y un cambio visual confirmado en un personaje en uso muestra el aviso de fork y solo crea el fork cuando el usuario confirma, sin ofrecer la edición del original.
- **Pruebas automáticas:**
  - Con un trabajo en curso, la solicitud de revisión se rechaza.
  - Con un trabajo en curso, la confirmación de una revisión pendiente se rechaza.
  - Con el personaje bloqueado y sin imágenes generadas, un cambio visual no incluye el aviso de consistencia visual.
  - Con el personaje bloqueado y con imágenes generadas, un cambio visual incluye el aviso de consistencia visual.
  - Con el personaje bloqueado y con imágenes generadas, un cambio solo narrativo no incluye el aviso de consistencia visual.
  - Confirmar un cambio visual en un personaje sin cuentos asociados invalida el retrato aprobado, marca las imágenes generadas como obsoletas y devuelve el personaje al estado borrador.
  - Confirmar un cambio visual en un personaje con instantáneas de cuento muestra el aviso de fork y no modifica el original mientras el usuario no confirma.
  - Confirmar el aviso de fork crea el fork y aplica el cambio visual al personaje nuevo.
  - Cancelar el aviso de fork no crea ningún personaje, no modifica la ficha del original y no ofrece la opción de editarlo.
- **Prueba manual:**
  1. Con la generación en curso, solicitar una revisión del personaje y comprobar que se rechaza con el indicador de generación en curso.
  2. Revisar un cambio visual de un personaje con imágenes generadas y comprobar que aparece el aviso de consistencia visual.
  3. Aceptar un cambio visual de un personaje no usado en cuentos y comprobar que su ficha deja de mostrar el retrato y vuelve a estado borrador.
  4. Aceptar un cambio visual de un personaje usado en cuentos y comprobar que aparece el aviso de fork.
  5. Cancelar el aviso de fork y comprobar que el original queda intacto y que no se ofrece editarlo.

### [ ] TASK-014 Modificar la vista del catálogo con la generación, la galería y el progreso

- **Cubre:** REQ-003, REQ-005, REQ-006, REQ-008, ALT-001, ALT-003, DEC-005
- **Componente:** character-catalog-view | app/
- **Depende de:** TASK-012
- **Alcance:** Modificar la vista del catálogo para ofrecer la solicitud y regeneración de retratos, mostrar el indicador de progreso y estado de generación, mostrar la galería de 4 candidatos y permitir seleccionar uno, e invalidar la galería cuando llega un evento de regeneración.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-003, REQ-005, REQ-006 y REQ-008 en el flujo de usuario, la ficha ofrece generar y regenerar retratos, muestra el progreso de la generación y la galería de 4 propuestas, y la selección de una propuesta actualiza el retrato mostrado.
- **Pruebas automáticas:**
  - No aplica: la verificación del flujo visual de la SPA requiere pruebas manuales hasta que el proyecto disponga de cobertura automatizada para este componente.
- **Prueba manual:**
  1. Abrir la ficha de un personaje y pulsar "Generar retratos"; comprobar que aparece el indicador de generación en curso.
  2. Esperar la notificación de finalización y comprobar que se muestra la galería con 4 propuestas.
  3. Pulsar "Regenerar" y comprobar que se muestra un indicador de generación en curso y que al terminar la galería muestra 4 propuestas nuevas.
  4. Seleccionar una propuesta y comprobar que la ficha muestra ese retrato como aprobado y que la galería deja de mostrar candidatos.
  5. Con la generación en curso, comprobar que los controles de edición y de uso del personaje están deshabilitados y que la ficha sigue siendo consultable.
  6. Con la galería abierta, regenerar desde otra sesión y comprobar que la galería abierta se invalida.

### [ ] TASK-015 Modificar el panel de revisión con el aviso de fork y su confirmación

- **Cubre:** REQ-009, REQ-011, ALT-007, DEC-001
- **Componente:** character-review-panel | app/
- **Depende de:** TASK-013
- **Alcance:** Modificar el panel de revisión para mostrar el aviso de consistencia visual junto a la propuesta cuando procede, mostrar el aviso de fork antes de crear el personaje derivado, y permitir confirmar o cancelar la creación del fork sin ofrecer la edición del original.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-009 y REQ-011 en el flujo de usuario, el aviso de consistencia visual aparece solo en sus condiciones, el aviso de fork ofrece confirmar o cancelar, y cancelar no deja ninguna vía de edición del personaje en uso.
- **Pruebas automáticas:**
  - No aplica: la verificación del flujo visual de la SPA requiere pruebas manuales hasta que el proyecto disponga de cobertura automatizada para este componente.
- **Prueba manual:**
  1. Revisar un cambio visual de un personaje con imágenes generadas y comprobar que el aviso de consistencia visual aparece junto a la propuesta.
  2. Revisar un cambio visual de un personaje sin imágenes generadas y comprobar que ese aviso no aparece.
  3. Aceptar un cambio visual de un personaje usado en cuentos y comprobar que aparece el aviso de fork explicando que se creará un personaje derivado.
  4. Confirmar el aviso de fork y comprobar que se crea el personaje nuevo y que el original queda intacto.
  5. Cancelar el aviso de fork y comprobar que no se crea ningún personaje y que no se ofrece la edición del original.

### [ ] TASK-016 Integrar el flujo completo de generación de retratos

- **Cubre:** REQ-001, REQ-004, REQ-005, REQ-006, REQ-007, ALT-001, ALT-002, DEC-005
- **Componente:** character-catalog-view | app/
- **Depende de:** TASK-005, TASK-007, TASK-012, TASK-014
- **Alcance:** Verificar de extremo a extremo la comunicación entre la vista de catálogo, el servicio de catálogo, el servicio de generación, el notificador de WebSocket y el difusor existente, desde la pulsación de "Generar retratos" hasta la galería de candidatos y la liberación del bloqueo.
- **Hecho cuando:** una solicitud de generación desde la ficha produce 4 candidatos visibles en la galería, notifica el inicio y la finalización por `/storyteller`, bloquea el personaje mientras dura y lo libera al terminar, sin que ninguna parte del flujo introduzca un timeout de generación.
- **Pruebas automáticas:**
  - No aplica: la verificación del flujo visual de la SPA requiere pruebas manuales hasta que el proyecto disponga de cobertura automatizada para este componente.
- **Prueba manual:**
  1. Con la aplicación abierta, pulsar "Generar retratos" en una ficha de personaje y comprobar que llegan la notificación de inicio y, al terminar, la de finalización.
  2. Comprobar que la galería muestra 4 propuestas y que el personaje queda bloqueado mientras la generación está en curso.
  3. Confirmar que la generación en curso no se abandona por espera y que al terminar el personaje vuelve a estar editable.
  4. Repetir el flujo con ComfyUI no disponible y comprobar que llega la notificación de error y que el personaje queda libre.

### [ ] TASK-017 Contrato: openapi - endpoints de generación y selección de retratos

- **Cubre:** REQ-001, REQ-003, REQ-004, REQ-005, REQ-006
- **Componente:** character-catalog-service | api/src/main/java/es/vargontoc/storyteller/application/service/CharacterCatalogService.java
- **Depende de:** TASK-012
- **Alcance:** Crear o actualizar el contrato OpenAPI de los endpoints de generación de retratos, regeneración, consulta de candidatos y selección de retrato aprobado en `docs/contracts/openapi/paths/character/`, con el nombre del fichero por definir (analista técnico).
- **Hecho cuando:** el contrato existe en `docs/contracts/openapi/` y describe las operaciones de generación, regeneración, consulta de candidatos y selección de retrato con los datos y respuestas definidos por la implementación, incluidas las respuestas de rechazo exigidas por los requisitos.
- **Pruebas automáticas:**
  - El contrato OpenAPI es válido según las reglas de su formato.
  - El contrato recoge las operaciones de generación, regeneración, consulta de candidatos y selección de retrato aprobado.
  - El contrato recoge la respuesta de rechazo cuando el personaje está bloqueado por una generación en curso.
  - El contrato recoge la respuesta de rechazo cuando la selección se hace sobre candidatos no vigentes.
- **Prueba manual:**
  1. Recorrer el contrato y comprobar que cada interfaz de esta tarea aparece con su método, entrada, salida y errores relevantes.
  2. Comparar el contrato con la implementación y confirmar que generación, regeneración, consulta de candidatos y selección coinciden.

### [ ] TASK-018 Contrato: openapi - esquemas de retrato, candidato y estado de generación

- **Cubre:** REQ-003, REQ-005, REQ-006, REQ-008, DEC-001, DEC-008
- **Componente:** character-catalog-service | api/src/main/java/es/vargontoc/storyteller/application/service/CharacterCatalogService.java
- **Depende de:** TASK-012
- **Alcance:** Crear o actualizar los esquemas OpenAPI de retrato aprobado, candidato, galería de candidatos, estado del trabajo de generación con los valores `RUNNING`/`COMPLETED`/`FAILED` y lock derivado en `docs/contracts/openapi/schemas/character*.yaml`, con el nombre del fichero por definir (analista técnico).
- **Hecho cuando:** el contrato existe en `docs/contracts/openapi/` y representa el retrato aprobado, el candidato, la galería de candidatos y el estado del trabajo de generación, exponiendo el bloqueo como un estado derivado y no como un recurso independiente.
- **Pruebas automáticas:**
  - Los contratos OpenAPI afectados son válidos según las reglas de su formato.
  - Los esquemas distinguen el retrato aprobado del candidato pendiente de selección.
  - El esquema del estado del trabajo de generación solo admite los valores `RUNNING`, `COMPLETED` y `FAILED`.
  - El bloqueo del personaje se expone como derivado del estado del trabajo y no como un recurso almacenable.
- **Prueba manual:**
  1. Revisar los esquemas y comprobar que el retrato aprobado, el candidato, la galería y el estado del trabajo están representados.
  2. Comparar los esquemas con la implementación y confirmar que la derivación del bloqueo y los estados coinciden.

### [ ] TASK-019 Contrato: openapi - revisiones de personaje con aviso de fork

- **Cubre:** REQ-006, REQ-009, REQ-011, REQ-012
- **Componente:** character-review-service | api/src/main/java/es/vargontoc/storyteller/application/service/CharacterReviewService.java
- **Depende de:** TASK-013
- **Alcance:** Actualizar el contrato OpenAPI de revisiones de personaje en `docs/contracts/openapi/paths/actor/` para incluir el aviso de consistencia visual, el aviso de fork y la confirmación de fork, con los nombres de fichero indicados en el diseño.
- **Hecho cuando:** el contrato existe en `docs/contracts/openapi/` y describe la solicitud, la consulta, la aceptación y el descarte de revisiones sobre el identificador de catálogo, recogiendo los avisos de consistencia visual y de fork y la respuesta al rechazo por personaje bloqueado.
- **Pruebas automáticas:**
  - Los contratos OpenAPI afectados son válidos según las reglas de su formato.
  - El contrato de comando de revisión no exige `storyId` y utiliza el identificador de catálogo.
  - El contrato recoge el aviso de consistencia visual, el aviso de fork y la confirmación del fork.
  - El contrato recoge la respuesta de rechazo cuando el personaje tiene una generación en curso.
- **Prueba manual:**
  1. Recorrer el contrato y comprobar que aparecen las interfaces de revisión de personaje y que todas utilizan el identificador de catálogo.
  2. Comparar cada interfaz y sus errores con la implementación y confirmar que los avisos y rechazos coinciden.

## Cobertura

| Requisito | Tareas |
|---|---|
| REQ-001 | TASK-001, TASK-003, TASK-004, TASK-007, TASK-016, TASK-017 |
| REQ-002 | TASK-004 |
| REQ-003 | TASK-001, TASK-003, TASK-008, TASK-012, TASK-014, TASK-017, TASK-018 |
| REQ-004 | TASK-003, TASK-007, TASK-012, TASK-016, TASK-017 |
| REQ-005 | TASK-005, TASK-007, TASK-011, TASK-014, TASK-016, TASK-017 |
| REQ-006 | TASK-002, TASK-006, TASK-007, TASK-011, TASK-012, TASK-013, TASK-014, TASK-016, TASK-018, TASK-019 |
| REQ-007 | TASK-002, TASK-006, TASK-007, TASK-011, TASK-016 |
| REQ-008 | TASK-001, TASK-008, TASK-009, TASK-012, TASK-013, TASK-014, TASK-018 |
| REQ-009 | TASK-009, TASK-013, TASK-015, TASK-019 |
| REQ-010 | TASK-001, TASK-009, TASK-013, TASK-018 |
| REQ-011 | TASK-010, TASK-013, TASK-015, TASK-019 |
| REQ-012 | TASK-001, TASK-010, TASK-012, TASK-019 |
| REQ-013 | TASK-010, TASK-012 |

## Avisos

- `.sdd/team.md` no define la opción `contract-strategy`; se aplica `implementation-first` por defecto, en línea con el criterio seguido en `character-inventory`. Las tareas TASK-017, TASK-018 y TASK-019 dependen de las tareas que implementan sus interfaces, y las de implementación no dependen de ellas.
- No existe `.sdd/task-types.md`, por lo que no se han generado tipos de tarea adicionales.
- Los componentes nuevos (portrait-generation-service, portrait-comfyui-adapter, portrait-candidate-store, portrait-selection-service, portrait-lock-service, portrait-fork-service, portrait-condition-adapter, portrait-websocket-notifier, portrait-generation-state-store, portrait-reconciliation-service) no tienen ruta asignada: se marcan como "por definir (analista técnico)". El diseño sugiere el módulo `portrait-generation` en `api/src/.../portrait` y `portrait-comfyui` en `api/src/.../portrait/external`, pero la decisión corresponde al analista técnico.
- `character-review-service` y `character-review-panel` ya existen de `character-inventory`; TASK-013 y TASK-015 los modifican en lugar de crearlos.
- El bloqueo no es un recurso persistido independiente: se deriva de la existencia de un `PortraitGenerationJob` en estado `RUNNING` (DEC-008) y su liberación se produce al pasar el trabajo a `COMPLETED` o `FAILED`. TASK-002, TASK-006 y TASK-011 concentran esa política.
- Ninguna tarea introduce un timeout de generación. TASK-006 incluye explícitamente la comprobación de que el bloqueo no se libera por el paso del tiempo.
- TASK-016 es una tarea de integración: su resultado verificable es la comunicación entre `character-catalog-view`, `character-catalog-service`, `portrait-generation-service` y `portrait-websocket-notifier` a través de `websocket-adapter`.
- El mapa de arquitectura no incluye contratos WebSocket: los eventos de retrato se asumen sobre el canal `/storyteller` ya existente y el difusor `websocket-adapter` se reutiliza sin cambios de contrato.
- `portrait-condition-adapter` materializa el puerto `character-image-condition-port` definido en `character-inventory` (TASK-002 de esa feature), que hasta ahora era una interfaz sin implementación.
- TASK-017, TASK-018 y TASK-019 formalizan las tres filas de contratos afectados del diseño; no se inventa su contenido operativo más allá de las interfaces, respuestas de error y avisos exigidos por los requisitos.
- La verificación de los flujos de la SPA (TASK-014, TASK-015 y TASK-016) se apoya en pruebas manuales, de forma coherente con `character-inventory`, porque el proyecto no dispone de cobertura automatizada para el frontend.
