# Requisitos: character-portraits

Estado: aprobado
Revisión: 2

## Contexto

Storyteller Agent genera retratos de los personajes del catálogo mediante ComfyUI. Esta feature define la generación de cuatro propuestas cartoon aptas para niños de 3 a 4 años, la selección del retrato aprobado, la comunicación asíncrona por WebSocket, el bloqueo de operaciones durante la generación y la gestión de cambios visuales en personajes que ya están en uso en cuentos.

## Fuera de alcance

- Entrenamiento, fine-tuning o modificación de modelos de ComfyUI.
- Edición manual de imágenes (recorte, pintura, retoque).
- Generación de imágenes de escenas o portadas de cuentos.
- Definición técnica del workflow de ComfyUI (nodos, parámetros, seeds).
- Implementación concreta de almacenamiento físico de assets; solo se especifican reglas de conservación y eliminación.

## Requisitos funcionales

### REQ-001 Generación de cuatro retratos candidatos

- **Requisito:** Cuando el usuario solicita generar el retrato de un personaje del catálogo, el sistema deberá pedir a ComfyUI cuatro propuestas visuales distintas del personaje, con estilo cartoon y contenido apto para niños de 3 a 4 años.
- **Criterios de aceptación:**
  - Dado un personaje del catálogo con ficha textual completa, cuando el usuario solicita generar retratos, entonces el sistema inicia la generación de exactamente 4 imágenes candidatas.
  - Dada una generación completada, cuando el usuario visualiza las propuestas, entonces las 4 imágenes son distintas entre sí y mantienen la identidad descrita en la ficha.
  - Dada una generación completada, cuando se revisa el contenido de las imágenes, entonces no incluyen elementos inapropiados para niños de 3 a 4 años.

### REQ-002 Estilo cartoon coherente

- **Requisito:** El sistema deberá aplicar un estilo visual cartoon consistente a todos los retratos generados para un mismo personaje y entre personajes del catálogo.
- **Criterios de aceptación:**
  - Dados dos retratos candidatos del mismo personaje, cuando se comparan visualmente, entonces comparten un estilo artístico reconocible como cartoon.
  - Dados dos personajes distintos del catálogo, cuando se comparan sus retratos aprobados, entonces ambos pertenecen al mismo universo visual cartoon del producto.

### REQ-003 Selección del retrato aprobado

- **Requisito:** Cuando el usuario elige uno de los cuatro retratos candidatos, el sistema deberá designarlo como retrato aprobado del personaje y descartar las tres propuestas restantes.
- **Criterios de aceptación:**
  - Dados 4 retratos candidatos de un personaje, cuando el usuario selecciona uno, entonces ese queda registrado como retrato aprobado del personaje.
  - Dado un retrato seleccionado, cuando se consulta la ficha del personaje, entonces se visualiza el retrato aprobado.
  - Dada una selección de retrato, cuando se inspecciona el almacenamiento, entonces solo permanece el retrato aprobado y las propuestas descartadas se eliminan.

### REQ-004 Regeneración de propuestas

- **Requisito:** Cuando el usuario no elige ningún retrato candidato, el sistema deberá permitir solicitar una nueva generación de cuatro propuestas distintas.
- **Criterios de aceptación:**
  - Dado un personaje con 4 propuestas no seleccionadas, cuando el usuario solicita regenerar, entonces el sistema inicia una nueva generación de 4 propuestas distintas.
  - Dada una regeneración solicitada, cuando finaliza la nueva generación, entonces las propuestas anteriores se reemplazan por las nuevas.
  - Dado un usuario que no ha seleccionado ninguna propuesta, cuando solicita regenerar varias veces, entonces el sistema no impone un límite al número de regeneraciones.

### REQ-005 Comunicación asíncrona por WebSocket

- **Requisito:** Mientras se generan los retratos de un personaje, el sistema deberá notificar al usuario el progreso por WebSocket hasta completar o fallar la generación.
- **Criterios de aceptación:**
  - Dada una generación de retratos en curso, cuando comienza el proceso, entonces el sistema envía una notificación de inicio por WebSocket.
  - Dada una generación de retratos en curso, cuando finaliza el proceso, entonces el sistema envía una notificación de finalización por WebSocket que incluye el identificador del personaje y el estado de éxito.
  - Dado un fallo en la generación, cuando se detecta el error, entonces el sistema envía una notificación por WebSocket con el motivo del fallo.

### REQ-006 Bloqueo de operaciones durante generación

- **Requisito:** Mientras se generan los retratos de un personaje, el sistema deberá bloquear cualquier modificación de su ficha, cualquier revisión pendiente o nueva, y su uso como protagonista en nuevos cuentos.
- **Criterios de aceptación:**
  - Dada una generación de retratos en curso, cuando el usuario intenta editar el nombre del personaje, entonces el sistema rechaza la operación y muestra un indicador de generación en curso.
  - Dada una generación de retratos en curso, cuando el usuario intenta solicitar una revisión del personaje, entonces el sistema rechaza la operación y muestra un indicador de generación en curso.
  - Dada una generación de retratos en curso, cuando el usuario intenta seleccionar el personaje como protagonista de un nuevo cuento, entonces el sistema no permite la selección hasta que finalice la generación.
  - Dada una generación de retratos en curso, cuando el usuario consulta la ficha del personaje, entonces puede ver el progreso y el estado de generación.

### REQ-007 Liberación del bloqueo

- **Requisito:** Cuando finaliza la generación de retratos (con éxito o con fallo), el sistema deberá liberar el bloqueo sobre el personaje.
- **Criterios de aceptación:**
  - Dada una generación completada con éxito, cuando el usuario intenta editar el personaje, entonces la operación se permite.
  - Dada una generación fallida, cuando el usuario intenta solicitar una nueva generación de retratos, entonces el sistema permite iniciarla.

### REQ-008 Estado borrador hasta retrato aprobado

- **Requisito:** El sistema deberá considerar que un personaje del catálogo está en estado “borrador” mientras no tenga un retrato aprobado.
- **Criterios de aceptación:**
  - Dado un personaje sin retrato aprobado, cuando se lista en el catálogo, entonces aparece el estado “borrador”.
  - Dado un personaje con retrato aprobado, cuando se lista en el catálogo, entonces no aparece el estado “borrador”.
  - Dado un personaje cuyo retrato aprobado se invalida, cuando se lista en el catálogo, entonces vuelve a aparecer el estado “borrador”.

### REQ-009 Aviso de consistencia visual ante cambio descriptivo

- **Requisito:** Si el usuario acepta una revisión que modifica la descripción visual de un personaje que ya tiene imágenes generadas, el sistema deberá advertirle de la pérdida de consistencia visual antes de aplicar el cambio.
- **Criterios de aceptación:**
  - Dado un personaje con retrato aprobado e imágenes generadas, cuando se propone una revisión que modifica la descripción visual, entonces el sistema muestra un aviso de consistencia visual junto con la propuesta.
  - Dado un personaje sin imágenes generadas, cuando se propone una revisión visual, entonces no aparece el aviso de consistencia visual.
  - Dado un personaje con imágenes generadas, cuando se propone una revisión solo narrativa, entonces no aparece el aviso de consistencia visual.

### REQ-010 Invalidación del retrato ante cambio visual

- **Requisito:** Cuando el usuario confirma una revisión que modifica la descripción visual de un personaje no usado en ningún cuento, el sistema deberá invalidar el retrato aprobado y las imágenes generadas asociadas a ese personaje.
- **Criterios de aceptación:**
  - Dado un personaje sin cuentos asociados y con retrato aprobado, cuando el usuario confirma un cambio visual, entonces el retrato aprobado deja de ser válido.
  - Dado un personaje cuyo retrato se invalida, cuando se consulta su ficha, entonces no se muestra ningún retrato aprobado y el personaje aparece como borrador.
  - Dada una invalidación, cuando se consultan las imágenes generadas previas del personaje, entonces se marcan como obsoletas o se eliminan según la política de assets.

### REQ-011 Aviso previo al fork

- **Requisito:** Si el usuario confirma una revisión visual de un personaje que ya está en uso en algún cuento, el sistema deberá avisar al usuario y esperar su confirmación antes de crear un fork del personaje; no ofrecerá ni permitirá editar el personaje original afectando a los cuentos previos.
- **Criterios de aceptación:**
  - Dado un personaje usado en al menos un cuento, cuando el usuario confirma un cambio visual, entonces el sistema muestra un aviso explicando que se creará un nuevo personaje derivado para conservar los cuentos existentes.
  - Dado el aviso de fork, cuando el usuario confirma, entonces el sistema crea el fork y aplica el cambio visual al nuevo personaje.
  - Dado el aviso de fork, cuando el usuario cancela, entonces no se crea el fork, no se modifica la ficha del personaje original y no se ofrece la opción de editar el original.

### REQ-012 Creación del fork

- **Requisito:** Cuando el usuario confirma el aviso de fork, el sistema deberá crear un nuevo personaje en el catálogo copiando la ficha del original, aplicando el cambio visual confirmado e invalidando el retrato del fork.
- **Criterios de aceptación:**
  - Dado un personaje original usado en cuentos, cuando se confirma el fork, entonces se crea un nuevo personaje con el mismo nombre más un sufijo distintivo y la descripción visual revisada.
  - Dado el nuevo personaje creado, cuando se consulta su ficha, entonces no tiene retrato aprobado y aparece en estado borrador hasta generar uno nuevo.
  - Dado el personaje original, cuando se crea el fork, entonces conserva su ficha, retrato aprobado e imágenes previas sin cambios.

### REQ-013 Renombrado del fork

- **Requisito:** El sistema deberá permitir renombrar el personaje fork después de su creación, sin afectar al personaje original ni a los cuentos asociados a este.
- **Criterios de aceptación:**
  - Dado un personaje fork recién creado, cuando el usuario cambia su nombre, entonces el catálogo muestra el nuevo nombre.
  - Dado un personaje fork renombrado, cuando se consulta el personaje original, entonces conserva su nombre original.

## Requisitos no funcionales

Ninguno.

## Supuestos

### SUP-001 Configuración de estilo cartoon en ComfyUI

- **Tipo:** contextual
- **Descripción:** La configuración concreta del estilo cartoon (checkpoint, LoRAs, prompts positivo/negativo) reside en la configuración de ComfyUI y no se define en esta especificación.
- **Impacto:** REQ-001 y REQ-002 asumen que existe una configuración que produce retratos cartoon coherentes.
- **Estado:** validado

### SUP-002 Existencia del catálogo y del puerto de condiciones visuales

- **Tipo:** contextual
- **Descripción:** El catálogo de personajes (`CatalogCharacter`) y el puerto `character-image-condition-port` ya existen como resultado de la feature `character-inventory`.
- **Impacto:** REQ-008, REQ-009 y REQ-010 consultan o actualizan condiciones mantenidas por este módulo.
- **Estado:** validado

### SUP-003 Definición de “en uso en un cuento”

- **Tipo:** contextual
- **Descripción:** Un personaje se considera “en uso” cuando existe al menos una instantánea `StoryActorSnapshot` asociada a un cuento.
- **Impacto:** REQ-011 y REQ-012 deciden crear un fork en función de esta condición.
- **Estado:** validado

### SUP-004 Aviso de fork: sin edición del original

- **Tipo:** decisional
- **Descripción:** Para un personaje usado en algún cuento, la única vía ante un cambio visual confirmado es crear un fork. El sistema no ofrece ni permite editar el personaje original afectando a los cuentos previos; el usuario solo puede confirmar la creación del fork o cancelar la operación.
- **Impacto:** REQ-011 y REQ-012 implementan un aviso con confirmación/cancelación: confirmar crea el fork y cancelar deja el original intacto, sin opción de edición directa del personaje en uso.
- **Estado:** validado

### SUP-005 Política de eliminación de propuestas descartadas

- **Tipo:** decisional
- **Descripción:** Las propuestas no elegidas se eliminan tras la selección, conservando solo el retrato aprobado.
- **Impacto:** REQ-003 incluye el criterio de que solo permanece el retrato aprobado.
- **Estado:** validado

### SUP-006 Personaje sin retrato aprobado como borrador

- **Tipo:** contextual
- **Descripción:** El estado “borrador” se calcula a partir de “no existe retrato aprobado”, de acuerdo con DEC-003 de `character-inventory`.
- **Impacto:** REQ-008 reutiliza esta condición.
- **Estado:** validado

## Preguntas abiertas

Ninguna.
