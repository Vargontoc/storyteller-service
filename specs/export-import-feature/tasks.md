# Tareas: export-import-feature

Estado: aprobado
Revisión: 1
Basado en: requirements@5, design@1

## Resumen

El trabajo se divide en cinco bloques en orden de dependencias: guion ampliado (lugar de escena por beat y planteamiento de portada), portada como página 0 revisable y fuera de la cascada, importación de imágenes, exportación para Nubi Game y exportación para el agente visual. Cada bloque tiene primero sus tareas de backend y después las de interfaz. Al final van las tareas de contrato, que se formalizan después de implementar (`implementation-first`). En total hay 43 tareas.

## Tareas

### [ ] TASK-001 Guion persistido con lugar de escena y planteamiento de portada

- **Cubre:** REQ-017, REQ-037, REQ-027, REQ-029, DEC-001
- **Componente:** story-outline-repository | api/src/main/java/es/vargontoc/storyteller/infrastructure/adapters/out/persistence/StoryOutlineRepositoryAdapter.java
- **Depende de:** -
- **Alcance:** El guion guarda y devuelve el lugar de escena de cada beat y el lugar y el tono propios de la portada. Los cuentos anteriores a la feature se leen sin planteamiento de portada ni lugar en sus beats.
- **Hecho cuando:** un guion con lugar en cada beat y con lugar y tono de portada se guarda y se recupera igual, con el planteamiento de portada separado del beat de la página 1, y un guion anterior a la feature se lee sin error y con esos campos ausentes.
- **Pruebas automáticas:**
  - Se guarda un guion de 4 beats, cada uno con su lugar, y una portada con lugar "habitación al anochecer" y tono de calma. Al leerlo, los 4 lugares y el lugar y el tono de la portada coinciden con lo guardado.
  - Se guarda una portada con lugar y tono distintos de los del beat de la página 1. Al leerlo, ambos se conservan por separado.
  - Un guion existente sin los campos nuevos se lee sin error, con el planteamiento de portada ausente y los beats sin lugar.
- **Prueba manual:**
  No aplica: es persistencia interna sin interfaz propia. Se verifica de extremo a extremo en TASK-017.

### [ ] TASK-002 Director genera lugar por beat y planteamiento de portada

- **Cubre:** REQ-017, REQ-037, DEC-001
- **Componente:** director-agent | api/src/main/java/es/vargontoc/storyteller/infrastructure/adapters/out/external/SpringAiDirectorAgent.java
- **Depende de:** -
- **Alcance:** La respuesta del director, tanto al crear el guion como en la revisión global por hint, incluye el lugar de escena de cada beat y el lugar y el tono propios de la portada, y el adaptador los entrega en el resultado.
- **Hecho cuando:** el adaptador transforma una respuesta del director con los campos nuevos en un guion que los contiene, al crearlo y en la revisión global, y una respuesta sin alguno de ellos llega a `story-service` con el campo ausente o vacío, sin inventar un valor.
- **Pruebas automáticas:**
  - Una respuesta simulada del director con 4 beats con lugar y con portada con lugar y tono da un guion con los 4 lugares y el planteamiento de portada.
  - Una respuesta simulada de la revisión global por hint con los campos nuevos da una escaleta candidata que los contiene.
  - Una respuesta simulada sin lugar en un beat da ese beat con el lugar vacío o ausente, sin valor inventado.
- **Prueba manual:**
  No aplica: se verifica con el modelo real en TASK-003 y TASK-017.

### [ ] TASK-003 Modelfile del director con las reglas de lugar y de planteamiento de portada

- **Cubre:** REQ-017, REQ-037
- **Componente:** director-model-provisioner | api/src/main/java/es/vargontoc/storyteller/infrastructure/config/DirectorModelProvisioner.java
- **Depende de:** TASK-002
- **Alcance:** El Modelfile del director pide un lugar de escena no vacío en cada beat y un lugar y un tono propios de la portada, distintos de los del beat de la página 1. El modelo registrado aplica estas reglas aunque ya hubiera uno registrado antes (riesgo del diseño).
- **Hecho cuando:** tras arrancar sobre un Ollama que ya tenía el modelo del director, el modelo registrado contiene las reglas nuevas, y un guion generado de 4 páginas cumple los criterios de REQ-017 y REQ-037.
- **Pruebas automáticas:**
  - Arranque con el modelo del director ya registrado con el Modelfile anterior: el modelo queda registrado con el Modelfile nuevo.
  - Arranque sin el modelo registrado: el modelo se registra con el Modelfile nuevo.
- **Prueba manual:**
  1. Arrancar la aplicación con el modelo del director antiguo ya registrado en Ollama.
  2. Crear un cuento nuevo de 4 páginas: los 4 beats tienen un lugar de escena no vacío, y la portada tiene un lugar y un tono propios no vacíos, mostrados aparte del beat de la página 1.

### [ ] TASK-004 Validación del guion ampliado

- **Cubre:** REQ-017, REQ-037, DEC-001, ALT-007
- **Componente:** story-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryService.java
- **Depende de:** TASK-001, TASK-002
- **Alcance:** Al crear el guion y al revisarlo de forma global, `story-service` exige un lugar no vacío en cada beat y un lugar y un tono de portada no vacíos. Si falta alguno, trata la respuesta como una salida del director que no supera la validación de campos obligatorios, igual que hoy, y no persiste el guion.
- **Hecho cuando:** un guion completo se persiste con los campos nuevos y un guion al que le falta cualquiera de ellos no se persiste, al crearlo y en la revisión global.
- **Pruebas automáticas:**
  - Director simulado con un guion completo de 4 páginas: se persiste con los 4 lugares y con el lugar y el tono de la portada.
  - Director simulado con un beat sin lugar al crear el guion: no se persiste y se aplica el tratamiento actual de los campos obligatorios que faltan (ALT-007).
  - Director simulado con la portada sin tono, o sin lugar, en la revisión global: la escaleta candidata no se acepta ni se persiste (ALT-007).
  - Lugar de beat o lugar de portada formado solo por espacios: se trata como vacío.
- **Prueba manual:**
  No aplica: la salida incompleta del director solo puede forzarse con dobles de prueba. El caso correcto se verifica en TASK-017.

### [ ] TASK-005 Lugar de escena editable en la edición directa de un beat

- **Cubre:** REQ-017
- **Componente:** beat-editor | api/src/main/java/es/vargontoc/storyteller/application/service/BeatEditorService.java
- **Depende de:** TASK-001
- **Alcance:** La edición directa de un beat trata el lugar de escena como un campo más del beat, que se edita, se valida como obligatorio y no vacío y se guarda. No toca el lugar ni el tono de la portada.
- **Hecho cuando:** editar el lugar de un beat guarda el nuevo lugar con el resto del comportamiento actual de la edición, y una edición con el lugar vacío se rechaza sin guardar.
- **Pruebas automáticas:**
  - Edición del beat 2 con lugar "cocina de la abuela": el beat guardado tiene ese lugar, y el lugar y el tono de la portada no cambian.
  - Edición de un beat con el lugar vacío: se rechaza y el beat conserva el lugar anterior.
- **Prueba manual:**
  No aplica: se verifica desde la interfaz en TASK-017.

### [ ] TASK-006 Escritor redacta la escena de portada desde su lugar y su tono

- **Cubre:** REQ-038, REQ-039, ALT-005
- **Componente:** writer-agent | api/src/main/java/es/vargontoc/storyteller/infrastructure/adapters/out/external/SpringAiWriterAgent.java
- **Depende de:** -
- **Alcance:** El escritor recibe el lugar y el tono propios de la portada para redactar su descripción de escena, y para redactar una nueva versión completa con hint, que puede aceptar o rechazar con motivo, igual que en las páginas.
- **Hecho cuando:** la petición de portada al escritor incluye el lugar y el tono de la portada, la respuesta se entrega como una descripción completa, y el rechazo de un hint se entrega con su motivo.
- **Pruebas automáticas:**
  - Portada con lugar "habitación al anochecer" y tono de calma: la petición enviada al modelo contiene ambos valores y la respuesta simulada se entrega como descripción no vacía.
  - Hint "que se vea la luna llena" con respuesta simulada de aceptación: se entrega una descripción completa nueva.
  - Hint con respuesta simulada de rechazo: se entrega el rechazo con su motivo y sin descripción nueva (ALT-005).
- **Prueba manual:**
  No aplica: se verifica con el modelo real en TASK-007 y TASK-021.

### [ ] TASK-007 Modelfile del escritor con las reglas de la escena de portada

- **Cubre:** REQ-038, REQ-039
- **Componente:** writer-model-provisioner | api/src/main/java/es/vargontoc/storyteller/infrastructure/config/WriterModelProvisioner.java
- **Depende de:** TASK-006
- **Alcance:** El Modelfile del escritor incluye las reglas para redactar la escena de portada, y su versión con hint, desde el lugar y el tono propios de la portada. El modelo registrado aplica estas reglas aunque ya hubiera uno registrado antes (riesgo del diseño).
- **Hecho cuando:** tras arrancar sobre un Ollama que ya tenía el modelo del escritor, el modelo registrado contiene las reglas nuevas.
- **Pruebas automáticas:**
  - Arranque con el modelo del escritor ya registrado con el Modelfile anterior: el modelo queda registrado con el Modelfile nuevo.
  - Arranque sin el modelo registrado: el modelo se registra con el Modelfile nuevo.
- **Prueba manual:**
  1. Arrancar la aplicación con el modelo del escritor antiguo ya registrado en Ollama.
  2. Generar la portada de un cuento con lugar "habitación al anochecer" y tono de calma: la descripción de escena presentada es coherente con ese lugar y ese tono.

### [ ] TASK-008 Generación y revisión con hint de la portada

- **Cubre:** REQ-038, REQ-039, DEC-004, ALT-004, ALT-005, ALT-006
- **Componente:** story-page-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageService.java
- **Depende de:** TASK-001, TASK-006
- **Alcance:** La operación existente de generar la portada y el hint sobre la portada, pendiente o aceptada, piden turno a `generation-gate`, mandan al escritor el lugar y el tono de la portada, guardan las versiones en `story-page-version-repository` y publican STARTED, VERSION_PRESENTED, HINT_REJECTED o FAILED. Si el guion no tiene lugar y tono de portada, la petición se rechaza sin llamar al escritor.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-039, la portada generada queda pendiente de revisión con una descripción no vacía, y las peticiones sobre cuentos sin planteamiento de portada se rechazan con un mensaje que indica que al guion le faltan el lugar y el tono de la portada.
- **Pruebas automáticas:**
  - Guion con planteamiento de portada: se genera la portada, se pide y se libera el turno, se publica VERSION_PRESENTED y la portada queda pendiente con la descripción del escritor.
  - Portada pendiente y aceptación: la portada queda aceptada con esa descripción.
  - Portada pendiente y hint "que se vea la luna llena": se guarda una nueva versión completa pendiente.
  - Portada aceptada y hint: se presenta una nueva versión pendiente.
  - Hint rechazado por el escritor: se publica HINT_REJECTED con el motivo y la versión vigente no cambia (ALT-005).
  - Fallo del escritor durante la generación, inicial o con hint: se publica FAILED, no hay reintento y la portada se queda como estaba (ALT-004).
  - Cuento sin lugar y tono de portada y petición de generar la portada o de hint: se rechaza sin llamar al escritor y la portada no cambia (ALT-006).
  - Generación de portada con otra generación en curso: se rechaza por el turno, como en las páginas.
- **Prueba manual:**
  No aplica: se verifica desde la interfaz en TASK-021.

### [ ] TASK-009 Página 1 bloqueada hasta aceptar la portada

- **Cubre:** REQ-040, DEC-002
- **Componente:** story-page-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageService.java
- **Depende de:** TASK-008
- **Alcance:** Mientras la portada de un cuento con guion aprobado no se haya aceptado por primera vez, se rechaza la generación de la página 1. Aceptar la portada habilita la página 1, que sigue generándose bajo demanda.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-040: la página 1 no se genera con la portada pendiente o sin descripción, y se puede generar tras aceptar la portada.
- **Pruebas automáticas:**
  - Guion aprobado y portada pendiente de revisión: la petición de generar la página 1 se rechaza y no se llama al escritor.
  - Guion aprobado y portada sin descripción: la petición de generar la página 1 se rechaza.
  - Portada aceptada por primera vez: la generación de la página 1 se admite.
  - Portada aceptada y después en revisión con hint: la página 1, y las siguientes según el flujo actual, se pueden seguir generando.
  - Aceptar la portada no genera la página 1 por sí solo.
- **Prueba manual:**
  No aplica: se verifica desde la interfaz en TASK-021.

### [ ] TASK-010 Portada fuera de la cascada y retirada de imágenes al reaprobar

- **Cubre:** REQ-007, REQ-008, REQ-041, REQ-043, DEC-003
- **Componente:** story-page-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageService.java
- **Depende de:** TASK-008
- **Alcance:** Aceptar una nueva versión de la portada retira solo su imagen importada y conserva las páginas interiores aceptadas, con su texto, su descripción, su imagen y su audio. Reaprobar una página interior, o descartarla por la cascada de PDEC-020, retira su imagen. Los cambios en las páginas interiores no tocan la portada.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-008, REQ-041, el primero de REQ-043 y el segundo de REQ-007.
- **Pruebas automáticas:**
  - Portada y páginas 1 a 3 aceptadas, página 2 con imagen: al aceptar una nueva versión de la portada, las páginas 1 a 3 siguen aceptadas con el mismo texto, la página 2 conserva su imagen y su audio se mantiene.
  - Portada aceptada con imagen: al aceptar una nueva versión de la portada, la portada no tiene imagen importada.
  - Páginas 2 y 3 con imagen: al aceptar una nueva versión de la página 2, ni la página 2 ni la 3 tienen imagen.
  - Portada aceptada con imagen: al aceptar una nueva versión de la página 2, la portada conserva la misma imagen.
- **Prueba manual:**
  No aplica: se verifica desde la interfaz en TASK-020 y TASK-021.

### [ ] TASK-011 Aprobar el guion lanza la generación de la portada

- **Cubre:** REQ-038, REQ-040, DEC-002, ALT-003
- **Componente:** story-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryService.java (integración con story-page-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageService.java)
- **Depende de:** TASK-004, TASK-008
- **Alcance:** Al aprobarse el guion, `story-service` pide a `story-page-service` que inicie la generación de la portada con la operación existente. La respuesta de la aprobación indica si la generación empezó. Si no puede empezar, la aprobación se completa igualmente y no hay reintento.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-038: tras aprobar el guion, la portada se presenta pendiente de revisión y la página 1 no existe, y una aprobación con la generación imposible deja el guion aprobado y la portada sin descripción.
- **Pruebas automáticas:**
  - Aprobación de un guion con planteamiento de portada y el turno libre: se inicia la generación de la portada, la respuesta indica que empezó y no se genera la página 1.
  - Aprobación con otra generación en curso: el guion queda aprobado, la respuesta indica que la portada no empezó y no hay reintento (ALT-003).
  - Aprobación con el escritor no disponible: el guion queda aprobado y la portada sigue sin descripción (ALT-003).
- **Prueba manual:**
  No aplica: se verifica desde la interfaz en TASK-021.

### [ ] TASK-012 Rehacer el guion descarta la portada y su imagen

- **Cubre:** REQ-042, REQ-043, DEC-001, ALT-008
- **Componente:** story-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryService.java (integración con story-page-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageService.java)
- **Depende de:** TASK-008, TASK-011
- **Alcance:** Se engancha al mecanismo de "rehacer el guion" que la aplicación tiene implementado hoy, sin redefinirlo ni cambiar qué acciones lo disparan: cuando ese mecanismo actúa, se descartan la descripción de escena de la portada (versiones y contenido aceptado) y su imagen importada.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-042 y el segundo de REQ-043: tras rehacer el guion con el mecanismo actual, la portada no está aceptada ni muestra imagen y, al aprobar el nuevo guion, presenta una descripción nueva pendiente de revisión.
- **Pruebas automáticas:**
  - Portada aceptada con imagen y guion rehecho con el mecanismo actual: la portada queda sin versiones, sin contenido aceptado y sin imagen importada.
  - Portada pendiente de revisión y guion rehecho: se descartan sus versiones.
  - Guion rehecho y aprobado de nuevo: se genera una descripción de portada nueva, pendiente de revisión y distinta de la descartada.
  - Las acciones que hoy no rehacen el guion siguen sin tocar la portada.
- **Prueba manual:**
  1. Abrir un cuento con la portada aceptada y con imagen importada.
  2. Rehacer el guion con la acción que ofrece hoy la aplicación: la portada deja de mostrar la imagen y deja de estar aceptada.
  3. Aprobar el nuevo guion: la portada muestra una descripción de escena nueva pendiente de revisión, no la anterior.

### [ ] TASK-013 Detección de imagen por su contenido

- **Cubre:** REQ-004, REQ-005, DEC-013
- **Componente:** image-content-inspector | por definir (analista técnico)
- **Depende de:** -
- **Alcance:** Decide si un fichero es una imagen por su contenido real, sin tener en cuenta la extensión, el tipo declarado, las dimensiones ni el peso.
- **Hecho cuando:** los ficheros de imagen de cualquier formato y tamaño se reconocen como imagen y los que no lo son se reconocen como no imagen, sea cual sea su extensión o tipo declarado.
- **Pruebas automáticas:**
  - PNG de 300 × 200 px: es imagen.
  - JPEG de más de 20 MB: es imagen.
  - BMP: es imagen.
  - Fichero de texto: no es imagen.
  - Fichero de texto con extensión `.png` y tipo declarado de imagen: no es imagen.
  - PNG válido con extensión `.txt`: es imagen.
- **Prueba manual:**
  No aplica: es un componente interno. Se verifica desde la interfaz en TASK-020.

### [ ] TASK-014 Subida de imagen en la portada y en las páginas aceptadas

- **Cubre:** REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, DEC-013, ALT-001, ALT-002
- **Componente:** story-page-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageService.java
- **Depende de:** TASK-013
- **Alcance:** La operación de subida, que se amplía si ya existe o se crea si no, admite imágenes solo en una portada o página aceptada, consulta a `image-content-inspector`, asocia la imagen al contenido aceptado de esa portada o página y sustituye la anterior. No aplica límite de tamaño propio y, si el fichero no es imagen, rechaza la subida sin tocar la imagen que había.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-003 a REQ-006 y el primero de REQ-007, y la subida en una portada o página sin aceptar se rechaza sin asociar ninguna imagen.
- **Pruebas automáticas:**
  - Subida de un PNG de 300 × 200 px en una página aceptada: se acepta y queda asociada a esa página.
  - Subida de un JPEG de más de 20 MB en la portada aceptada: se acepta.
  - Tres páginas aceptadas y subida en la página 3: solo la página 3 tiene imagen.
  - Página 1 con la imagen A y subida de B: la página 1 queda con B y A deja de estar asociada.
  - Página con imagen y subida de un fichero de texto: se rechaza con el motivo "no es una imagen" y la página conserva su imagen (ALT-001).
  - Subida en la portada sin aceptar, con guion aprobado: se rechaza y la portada no tiene imagen (ALT-002).
  - Subida en una página pendiente de revisión: se rechaza y la página no tiene imagen (ALT-002).
  - La imagen subida se sigue leyendo igual tras reiniciar la aplicación.
- **Prueba manual:**
  No aplica: se verifica desde la interfaz en TASK-020.

### [ ] TASK-015 Descripción de escena completa de la portada y de las páginas en la vista

- **Cubre:** REQ-001, REQ-036
- **Componente:** story-page-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageService.java
- **Depende de:** TASK-008
- **Alcance:** La consulta de la portada y de las páginas devuelve completa la descripción de escena aceptada de la portada y la de cada página interior aceptada, cada una la suya.
- **Hecho cuando:** la vista de la portada aceptada devuelve su propia descripción completa, no la de la página 1, y la de cada página aceptada devuelve la suya completa.
- **Pruebas automáticas:**
  - Portada y página 1 aceptadas con descripciones distintas: la consulta de la portada devuelve la de la portada, sin recortar.
  - Página 2 aceptada: su consulta devuelve su descripción de escena completa.
- **Prueba manual:**
  No aplica: se verifica desde la interfaz en TASK-019.

### [ ] TASK-016 Cliente del guion con los campos ampliados

- **Cubre:** REQ-017, REQ-037
- **Componente:** story-script-api | app/src/api/storyScript.ts
- **Depende de:** TASK-004, TASK-005, TASK-011
- **Alcance:** El cliente transporta el lugar de escena de cada beat y el lugar y el tono de la portada, envía el lugar en la edición directa de un beat y expone si la aprobación inició la generación de la portada.
- **Hecho cuando:** la consulta del guion expone los campos nuevos, la edición de beat envía el lugar y la respuesta de aprobación indica si la portada empezó a generarse.
- **Pruebas automáticas:**
  - Respuesta simulada del guion con lugar por beat y lugar y tono de portada: el cliente los expone.
  - Respuesta simulada de un cuento antiguo sin esos campos: el cliente los expone como ausentes, sin error.
  - Edición de beat con lugar: la petición enviada incluye el lugar.
  - Respuesta de aprobación con la portada iniciada y no iniciada: el cliente distingue ambos casos.
- **Prueba manual:**
  No aplica: se verifica en TASK-017.

### [ ] TASK-017 Escaleta con lugar por beat y planteamiento de portada

- **Cubre:** REQ-017, REQ-037
- **Componente:** story-script-panel | app/src/components/StoryScriptPanel.vue
- **Depende de:** TASK-016
- **Alcance:** La escaleta muestra el lugar de escena de cada beat y, aparte, el lugar y el tono de la portada para que el adulto los apruebe con ella, y la edición directa de un beat permite editar su lugar.
- **Hecho cuando:** el adulto ve y aprueba en la escaleta el lugar de cada beat y el lugar y el tono de la portada, y puede editar el lugar de un beat.
- **Pruebas automáticas:**
  - Guion de 4 beats con lugar: se muestran los 4 lugares.
  - Guion con lugar y tono de portada: se muestran aparte de los del beat de la página 1.
  - Edición de beat: el formulario incluye el lugar y lo envía.
- **Prueba manual:**
  1. Crear un cuento nuevo de 4 páginas: la escaleta muestra un lugar de escena en cada beat y, aparte, el lugar y el tono de la portada.
  2. Editar el lugar del beat 2 y guardar: la escaleta muestra el nuevo lugar.
  3. Aprobar el guion: la aprobación se completa.

### [ ] TASK-018 Cliente de portada y páginas: subida de imagen y hint de portada

- **Cubre:** REQ-002, REQ-003, REQ-039
- **Componente:** story-pages-api | app/src/api/storyPages.ts
- **Depende de:** TASK-008, TASK-014
- **Alcance:** El cliente ofrece la subida de imagen en la portada o en una página y el hint sobre la portada, y expone los rechazos (no es imagen, portada o página sin aceptar, guion sin planteamiento de portada).
- **Hecho cuando:** el cliente envía la subida y el hint de portada a sus operaciones y entrega los motivos de rechazo.
- **Pruebas automáticas:**
  - Subida de un fichero en la página 3: la petición va a la página 3 con el fichero.
  - Respuesta simulada de rechazo "no es una imagen": el cliente la entrega con ese motivo.
  - Hint sobre la portada: la petición va a la portada con el hint.
  - Respuesta simulada de rechazo por falta de planteamiento de portada: el cliente la entrega con ese motivo.
- **Prueba manual:**
  No aplica: se verifica en TASK-020 y TASK-021.

### [ ] TASK-019 Descripción de escena completa visible en la portada y en las páginas

- **Cubre:** REQ-001, REQ-036
- **Componente:** story-pages-panel | app/src/components/StoryPagesPanel.vue
- **Depende de:** TASK-015, TASK-018
- **Alcance:** La portada aceptada y cada página aceptada muestran completa su propia descripción de escena.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-001 y REQ-036.
- **Pruebas automáticas:**
  - Página 2 aceptada: el panel muestra su descripción completa.
  - Portada y página 1 con descripciones distintas: la portada muestra la suya, no la de la página 1.
- **Prueba manual:**
  1. Abrir la página 2 aceptada de un cuento: se ve su descripción de escena completa.
  2. Abrir la portada aceptada: se ve su propia descripción completa, distinta de la de la página 1.

### [ ] TASK-020 Subida de imagen desde el panel de portada y páginas

- **Cubre:** REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, ALT-001, ALT-002
- **Componente:** story-pages-panel | app/src/components/StoryPagesPanel.vue
- **Depende de:** TASK-018
- **Alcance:** El panel ofrece la opción de subir imagen solo en la portada o la página aceptada, muestra la imagen tras subirla, avisa de que el fichero no es una imagen manteniendo la anterior y la sustituye al subir otra.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-002 a REQ-006 y el primero de REQ-007 vistos desde la interfaz.
- **Pruebas automáticas:**
  - Portada aceptada: se muestra la opción de subir imagen.
  - Portada sin aceptar con guion aprobado: no se muestra la opción (ALT-002).
  - Página pendiente de revisión: no se muestra la opción (ALT-002).
  - Respuesta de subida correcta: se muestra la imagen nueva.
  - Respuesta de rechazo "no es una imagen": se muestra el mensaje y se mantiene la imagen anterior (ALT-001).
- **Prueba manual:**
  1. Con la portada aceptada, subir un JPEG de más de 20 MB: la portada lo muestra.
  2. En la página 3 aceptada, subir un PNG de 300 × 200 px: solo la página 3 muestra la imagen y las páginas 1 y 2 no cambian.
  3. En la página 3, subir otro PNG: se muestra el nuevo en lugar del anterior.
  4. En la página 3, subir un fichero de texto: aparece un mensaje que indica que no es una imagen y se sigue viendo la imagen anterior.
  5. Cerrar y volver a abrir la aplicación y el cuento: la página 3 sigue mostrando la misma imagen.

### [ ] TASK-021 Revisión de la portada en el panel y bloqueo de la página 1

- **Cubre:** REQ-038, REQ-039, REQ-040, REQ-041, REQ-043, ALT-003, ALT-004, ALT-005, ALT-006
- **Componente:** story-pages-panel | app/src/components/StoryPagesPanel.vue (integración con story-page-notifier | api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageNotifier.java por websocket)
- **Depende de:** TASK-009, TASK-010, TASK-011, TASK-018
- **Alcance:** El panel recibe por WebSocket los eventos de la portada, también de la que se lanza al aprobar el guion. Permite aceptar la portada o pedirle un cambio con hint, muestra el motivo de un hint rechazado o el fallo, avisa de que a la portada le faltan el lugar y el tono en los cuentos antiguos, y no ofrece la página 1 hasta que la portada esté aceptada.
- **Hecho cuando:** el adulto revisa la portada con hint como una página y no puede generar la página 1 hasta aceptarla, y los eventos de la portada lanzada al aprobar el guion actualizan el panel.
- **Pruebas automáticas:**
  - Evento VERSION_PRESENTED de la portada: el panel muestra la descripción pendiente con las acciones de aceptar y pedir cambio.
  - Evento HINT_REJECTED de la portada: se muestra el motivo y se mantiene la versión vigente (ALT-005).
  - Evento FAILED de la portada: se muestra el fallo y la opción de volver a pedirla (ALT-004).
  - Portada sin aceptar: la acción de generar la página 1 no está disponible.
  - Cuento sin planteamiento de portada: se muestra el aviso de que faltan el lugar y el tono de la portada (ALT-006).
  - Aprobación cuya portada no pudo empezar: se ofrece pedir la portada a mano (ALT-003).
- **Prueba manual:**
  1. Aprobar el guion de un cuento nuevo: aparece una descripción de portada pendiente de revisión y la página 1 no se puede generar.
  2. Pedir un cambio con el hint "que se vea la luna llena": aparece una descripción nueva completa pendiente de revisión.
  3. Aceptar la portada: queda aceptada y se habilita la generación de la página 1, que no empieza sola.
  4. Con las páginas 1 a 3 aceptadas y la página 2 con imagen, y la portada con imagen, pedir un cambio en la portada y aceptarlo: las páginas 1 a 3 siguen aceptadas con el mismo texto, la página 2 conserva su imagen y la portada ya no tiene imagen.
  5. Abrir un cuento anterior a la feature: la portada avisa de que faltan su lugar y su tono.

### [ ] TASK-022 Derivación de slugs deterministas

- **Cubre:** REQ-022, REQ-024, REQ-032, DEC-012
- **Componente:** story-slugger | por definir (analista técnico)
- **Depende de:** -
- **Alcance:** Deriva un slug desde un texto con la regla de SUP-009: minúsculas, tildes, diéresis y `ñ` transliteradas, secuencias no alfanuméricas convertidas en un guion, sin guiones iniciales ni finales y recortado a 40 caracteres sin guion final. Indica si el resultado tiene menos de 3 caracteres.
- **Hecho cuando:** el mismo texto da siempre el mismo slug con la regla de SUP-009, y un resultado demasiado corto se detecta.
- **Pruebas automáticas:**
  - "La luna dormida" da `la-luna-dormida`.
  - "Niña Ñoño, ¡el pingüino!" da `nina-nono-el-pinguino`.
  - Un título de más de 40 caracteres cuyo corte cae tras un separador da 40 caracteres o menos y sin guion final.
  - "¡¡ !!" o "Yo" dan un resultado de menos de 3 caracteres, que se marca como no válido.
  - Dos llamadas con el mismo texto dan el mismo slug.
- **Prueba manual:**
  No aplica: es una función interna determinista, cubierta por las pruebas automáticas.

### [ ] TASK-023 Conversión de imágenes a WebP para la exportación

- **Cubre:** REQ-015, REQ-033, REQ-034, REQ-035, DEC-007, ALT-010
- **Componente:** export-image-converter | por definir (analista técnico)
- **Depende de:** -
- **Alcance:** Convierte una imagen importada a WebP con calidad 85, con la orientación aplicada y sin metadatos. Reduce el lado mayor a 2048 px si lo supera, conservando la proporción y sin ampliar nunca. Trabaja sobre una copia, sin modificar el original, e informa de que no ha podido convertir cuando la imagen no se puede convertir.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-033 (los dos primeros) y REQ-034, y una imagen no convertible da un fallo identificable sin modificar el original.
- **Pruebas automáticas:**
  - PNG y BMP: la salida es WebP.
  - JPEG con orientación EXIF de 90°: la salida se ve con la misma orientación y no tiene metadatos.
  - 4096 × 3072 px: la salida mide 2048 × 1536 px.
  - 300 × 200 px: la salida mide 300 × 200 px.
  - Tras convertir, el fichero original es idéntico byte a byte.
  - Imagen que se reconoce como imagen pero no se puede decodificar para convertirla: se informa del fallo y el original no cambia (ALT-010).
- **Prueba manual:**
  No aplica: se verifica de extremo a extremo en TASK-032.

### [ ] TASK-024 Zip para Nubi Game con referencias explícitas

- **Cubre:** REQ-011, REQ-012, REQ-013, REQ-014, REQ-015, REQ-016, REQ-033, DEC-005, DEC-014
- **Componente:** nubi-game-exporter | no consta en el mapa
- **Depende de:** TASK-023
- **Alcance:** Sustituye la composición actual del zip con la convención `page_N` por un zip con `story.json` (título, ruta de la portada y, por página y en orden, texto aceptado, ruta de la imagen y ruta del audio), la portada convertida y, por página, su imagen convertida y su audio. Las rutas son relativas a la raíz y el zip no se persiste.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-011 a REQ-015, y el zip resultante ya no usa la convención `page_N` como único medio para asociar los ficheros.
- **Pruebas automáticas:**
  - Cuento completo de 5 páginas: el zip contiene `story.json`, 1 imagen de portada, 5 imágenes de página y 5 audios, y ningún audio de portada.
  - Imágenes importadas en PNG: todas las del zip están en WebP y ninguna en PNG.
  - Cuento "La luna dormida": el `story.json` tiene ese título y una ruta de portada que existe en el zip.
  - Cuento de 3 páginas: hay 3 entradas de página en orden 1, 2, 3, cada una con su texto aceptado y sus rutas de imagen y audio.
  - Cada ruta del `story.json`, resuelta desde la raíz del zip, apunta a un fichero existente.
  - Se renombran todos los ficheros de imagen y audio y se actualizan sus rutas: cada página sigue asociada a su imagen y su audio.
  - Página 2 con la imagen B: la imagen referenciada por la página 2 tiene el mismo contenido que B convertida.
  - Tras exportar, no queda ningún zip ni imagen convertida guardados en el servidor.
- **Prueba manual:**
  No aplica: se verifica de extremo a extremo en TASK-032.

### [ ] TASK-025 Exportación para Nubi Game con evaluación completa de faltas

- **Cubre:** REQ-009, REQ-010, REQ-016, REQ-035, DEC-004, DEC-006, ALT-009, ALT-010
- **Componente:** story-export-service | por definir (analista técnico)
- **Depende de:** TASK-001, TASK-024
- **Alcance:** Expone la operación de exportación para Nubi Game, que sustituye a la actual. Comprueba la portada aceptada (aceptada y con planteamiento de portada en el guion), cada página aceptada, la imagen de la portada y la imagen y el audio de cada página, este último según `narration-repository`. Acumula todas las faltas, intenta convertir todas las imágenes, rechaza sin zip indicando cada falta o cada imagen no convertible con su portada o página y, si no hay faltas, entrega el zip.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-009, REQ-010 y REQ-035, y la operación de exportación anterior deja de estar disponible.
- **Pruebas automáticas:**
  - Cuento completo: se entrega el zip.
  - Cuento completo sin audio de portada: se entrega el zip.
  - Falta la imagen de la página 2 y el audio de la página 4: no hay zip y la respuesta indica "imagen" en la página 2 y "audio" en la página 4 (ALT-009).
  - Falta la imagen de portada: no hay zip y se indica la falta de imagen en la portada.
  - Portada sin aceptar: no hay zip y se indica "portada sin aceptar".
  - Cuento anterior a la feature con imágenes y audio: no hay zip y se indica "portada sin aceptar" (DEC-004).
  - Página 3 sin aceptar: se indica la falta de texto aceptado en la página 3.
  - Imagen de la página 3 no convertible: no hay zip, se indica la página 3 y se intentan convertir las demás (ALT-010).
  - Varias faltas a la vez: se devuelven todas en la misma respuesta.
  - La ruta de la exportación anterior ya no responde con un zip.
- **Prueba manual:**
  No aplica: se verifica desde la interfaz en TASK-032.

### [ ] TASK-026 Lectura de la instantánea del protagonista

- **Cubre:** REQ-024, REQ-025
- **Componente:** story-protagonist-snapshot | no consta en el mapa
- **Depende de:** -
- **Alcance:** Garantiza el acceso de lectura, desde `storyteller-server`, al nombre, la descripción visual y el retrato de la instantánea del protagonista de un cuento.
- **Hecho cuando:** para un cuento se obtienen el nombre, la descripción visual y el retrato de su instantánea, y no los del catálogo actual.
- **Pruebas automáticas:**
  - Cuento con instantánea: se leen su nombre, su descripción visual y su retrato.
  - Protagonista editado en el catálogo después de crear el cuento: la lectura devuelve los datos y el retrato de la instantánea, no los actuales.
- **Prueba manual:**
  No aplica: es un acceso interno. Se verifica de extremo a extremo en TASK-032.

### [ ] TASK-027 Eliminación del fondo del retrato con ComfyUI

- **Cubre:** REQ-025, DEC-008, ALT-012
- **Componente:** character-background-remover | por definir (analista técnico)
- **Depende de:** -
- **Alcance:** Usa `comfyui-client` (api/src/main/java/es/vargontoc/storyteller/infrastructure/adapters/in/rest/clients/ComfyUIClient.java) para subir el retrato recibido, ejecutar un workflow de eliminación de fondo y descargar el PNG resultante, sin reescalarlo y sin pedir turno a `generation-gate`. Informa de que no ha podido obtener la imagen si el proceso no termina.
- **Hecho cuando:** un retrato con fondo de color da un PNG con el protagonista y el fondo transparente, del mismo tamaño que el retrato, y un fallo de ComfyUI da un resultado identificable de imagen no obtenida. El nodo de eliminación de fondo está disponible en la instalación de ComfyUI del proyecto.
- **Pruebas automáticas:**
  - ComfyUI simulado que devuelve un PNG: se entrega ese PNG y no se pide turno a `generation-gate`.
  - ComfyUI simulado con error o sin resultado: se informa de que no se pudo obtener la imagen, sin reintento (ALT-012).
  - Retrato de 768 × 768 px: la salida no se reescala.
- **Prueba manual:**
  1. Con ComfyUI levantado, ejecutar la eliminación de fondo sobre un retrato con fondo de color: el PNG resultante muestra al protagonista con el fondo transparente.

### [ ] TASK-028 Validador del contrato v0.1 del agente visual

- **Cubre:** REQ-021, REQ-023, REQ-026, REQ-030, REQ-031, REQ-032, DEC-006, DEC-009, ALT-013
- **Componente:** visual-contract-validator | por definir (analista técnico)
- **Depende de:** -
- **Alcance:** Comprueba en una sola pasada un `story.json` y la imagen del personaje con las reglas de la sección 5 de `docs/contracts/agent-visual/input.md` v0.1, tomando la raíz del zip como `input/`. Incluye UTF-8 sin BOM, JSON sin claves duplicadas, `id` únicos en formato slug, referencias de escena existentes, campos fijos, paleta base y reglas de la imagen (PNG real, alfa con fondo transparente, al menos 1024 × 1024 px, 1:1). Devuelve todas las reglas incumplidas.
- **Hecho cuando:** un `story.json` y una imagen conformes no dan errores, y cada regla de la sección 5 incumplida aparece en la lista devuelta, todas a la vez.
- **Pruebas automáticas:**
  - Entrada conforme: ningún error.
  - Imagen de 768 × 768 px: error de imagen de menos de 1024 × 1024 px.
  - Imagen no cuadrada, sin alfa o con fondo opaco, o JPEG con extensión `.png`: error por cada regla incumplida.
  - `storyId` de menos de 3 caracteres: error de identificador del cuento fuera de patrón.
  - `visualDescription` de la escena `cover` vacía: error de descripción visual de portada vacía.
  - Escena que referencia un `id` inexistente en `characters`: error de referencia.
  - `schemaVersion`, `language` o `ageRange` distintos de "0.1", "es" y "3-4": error.
  - `style` con `colors` o con paleta distinta de "base": error.
  - Fichero con BOM o con claves duplicadas: error.
  - `id` de escenas duplicados: error.
  - Imagen pequeña y `storyId` corto a la vez: se devuelven ambos errores.
- **Prueba manual:**
  No aplica: es un componente interno. Su efecto se verifica en TASK-032.

### [ ] TASK-029 Composición del `story.json` y del zip para el agente visual

- **Cubre:** REQ-020, REQ-021, REQ-022, REQ-023, REQ-024, REQ-027, REQ-028, REQ-029, REQ-030, REQ-031, DEC-005, DEC-010, DEC-011, DEC-012
- **Componente:** visual-agent-exporter | por definir (analista técnico)
- **Depende de:** TASK-001, TASK-022, TASK-026, TASK-027, TASK-028
- **Alcance:** Compone el `story.json` v0.1 y el zip con `characters/<id>/character.png`: campos fijos, `title` y `storyId`, paleta base, el protagonista de la instantánea como único personaje con 1 a 5 rasgos tomados de su descripción visual, la escena `cover` con el título, la descripción, el lugar y el tono de la portada, y una escena `scene` por página con su texto, su descripción, el lugar del beat y su tono literal, todas referenciando al protagonista con `id` posicionales. Pasa el resultado a `visual-contract-validator` y solo genera el zip si no hay errores.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-020 a REQ-024 y REQ-027 a REQ-031, y el zip no se genera ni se persiste si el validador devuelve errores.
- **Pruebas automáticas:**
  - Protagonista "Nina": el zip contiene `story.json` en la raíz y `characters/nina/character.png`.
  - `schemaVersion` "0.1", `language` "es", `ageRange` "3-4", `style` con `paletteId` y `paletteSource` "base" y sin `colors`.
  - "La luna dormida": `title` "La luna dormida" y `storyId` `la-luna-dormida`; dos exportaciones sin cambios dan los mismos `storyId` e `id`.
  - Reparto con un conejo blanco: `characters` tiene un solo elemento, con `role` "protagonist" y el nombre de la instantánea.
  - `immutableTraits` tiene de 1 a 5 elementos no vacíos que aparecen literalmente en la descripción visual. Una descripción sin separadores da 1 rasgo.
  - Portada con descripción "Nina mira la luna desde la ventana", lugar "habitación al anochecer" y tono de calma: la primera escena es `cover`, con `text` "La luna dormida", esa `visualDescription`, ese `setting` y `emotion` igual al tono, sin espacios sobrantes.
  - Portada y página 1 con descripción, lugar y tono distintos: la escena `cover` usa los de la portada.
  - 4 páginas: `scenes` tiene 5 elementos en orden, portada y páginas 1 a 4, cada una con su texto y su descripción.
  - Página 2 con lugar "cocina de la abuela" y tono alegre: su escena tiene ese `setting` y ese tono como `emotion`.
  - Todas las escenas referencian solo el `id` del protagonista.
  - El resultado completo pasa el validador sin errores.
  - Validador simulado con errores: no se genera el zip y se devuelven los errores (ALT-013).
- **Prueba manual:**
  No aplica: se verifica de extremo a extremo en TASK-032.

### [ ] TASK-030 Exportación para el agente visual con evaluación completa de condiciones

- **Cubre:** REQ-018, REQ-019, REQ-025, REQ-026, REQ-032, DEC-004, DEC-006, ALT-011, ALT-012, ALT-013
- **Componente:** story-export-service | por definir (analista técnico)
- **Depende de:** TASK-025, TASK-029
- **Alcance:** Expone la operación de exportación para el agente visual. Comprueba el guion aprobado, la portada aceptada (con planteamiento de portada), cada página aceptada, el lugar en todos los beats y el lugar y el tono de la portada, sin exigir imágenes ni audio. Acumula todas las condiciones que faltan. Si se cumplen, pide el zip a `visual-agent-exporter` y lo entrega, o devuelve todas las reglas incumplidas o el fallo al obtener la imagen del protagonista.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-018, REQ-019, REQ-026 y REQ-032 desde la operación de exportación.
- **Pruebas automáticas:**
  - Cuento nuevo con guion aprobado, portada y páginas aceptadas, sin imágenes ni audio: se entrega el zip.
  - Beats sin lugar de escena: no hay zip y se indica que el guion no tiene el lugar de escena necesario (ALT-011).
  - Beats con lugar y portada sin lugar ni tono: no hay zip y se indica que a la portada le faltan el lugar y el tono.
  - Página 3 sin aceptar: no hay zip y se indica la página 3.
  - Portada sin aceptar: no hay zip y se indica "portada sin aceptar".
  - Varias condiciones a la vez: se indican todas.
  - Retrato de 768 × 768 px: no hay zip y se indica que la imagen mide menos de 1024 × 1024 px (ALT-012).
  - Eliminación de fondo fallida: no hay zip, se indica que no se pudo obtener la imagen del protagonista y la instantánea no cambia (ALT-012).
  - Título que da un `storyId` de menos de 3 caracteres y portada con descripción vacía: no hay zip y se indican ambas reglas (ALT-013).
- **Prueba manual:**
  No aplica: se verifica desde la interfaz en TASK-032.

### [ ] TASK-031 Cliente REST de las exportaciones

- **Cubre:** REQ-009, REQ-016, REQ-018
- **Componente:** story-export-api | por definir (analista técnico)
- **Depende de:** TASK-025, TASK-030
- **Alcance:** Cliente de las dos operaciones de exportación, que entrega el zip como descarga o la lista de faltas o reglas incumplidas.
- **Hecho cuando:** el cliente distingue la respuesta con zip de la de rechazo en las dos exportaciones y entrega la lista completa en el rechazo.
- **Pruebas automáticas:**
  - Respuesta simulada con zip para Nubi Game y para el agente visual: el cliente entrega el fichero.
  - Respuesta simulada de rechazo con varias faltas: el cliente entrega todas, con su portada o página.
- **Prueba manual:**
  No aplica: se verifica en TASK-032.

### [ ] TASK-032 Acciones de exportación en la interfaz

- **Cubre:** REQ-009, REQ-010, REQ-016, REQ-018, REQ-019, REQ-026, REQ-032, REQ-035, ALT-009, ALT-011
- **Componente:** story-export-actions | no consta en el mapa
- **Depende de:** TASK-031
- **Alcance:** Sustituye la acción actual de exportación para Nubi Game por la nueva, añade la exportación para el agente visual, descarga el zip o muestra la lista completa de faltas o reglas incumplidas.
- **Hecho cuando:** el adulto solo encuentra una exportación para Nubi Game y una para el agente visual, descarga el zip cuando se cumplen las condiciones y ve todas las faltas cuando no.
- **Pruebas automáticas:**
  - Se muestra una única acción de exportación para Nubi Game y una para el agente visual.
  - Respuesta de rechazo con varias faltas: se muestran todas, con su portada o página.
  - Respuesta con zip: se inicia la descarga.
- **Prueba manual:**
  1. En un cuento nuevo completo (portada y páginas aceptadas, con imágenes PNG en la portada y en las páginas, y audio en cada página), abrir las opciones de exportación: solo hay una exportación para Nubi Game.
  2. Exportar para Nubi Game: se descarga un zip con `story.json`, la portada y una imagen y un audio por página. Las imágenes están en WebP con el lado mayor de 2048 px como máximo, y las rutas del `story.json` existen en el zip.
  3. Volver a abrir la página 2: se ve la imagen importada original.
  4. En otro cuento completo salvo la imagen de la página 2, que no se ha subido, exportar para Nubi Game: se muestra "imagen" en la página 2 y no se descarga nada.
  5. Exportar el cuento del paso 1 para el agente visual: se descarga un zip con `story.json` y `characters/<id>/character.png` con el fondo transparente.
  6. Validar ese zip con las reglas de la sección 5 de `docs/contracts/agent-visual/input.md`: no hay errores.
  7. Exportar para el agente visual un cuento anterior a la feature: no se descarga nada y se indica qué condición falta.

### [ ] TASK-033 Contrato: openapi - lugar de escena en el beat

- **Cubre:** REQ-017, DEC-001
- **Componente:** story-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryService.java. Contrato: docs/contracts/openapi/schemas/story-beat.yaml
- **Depende de:** TASK-004
- **Alcance:** Actualizar `story-beat.yaml` para añadir el lugar de escena obligatorio y no vacío al beat.
- **Hecho cuando:** el contrato existe en `docs/contracts/openapi/` y describe el beat tal como quedó implementado.
- **Pruebas automáticas:**
  - El documento OpenAPI completo es válido según la especificación OpenAPI.
- **Prueba manual:**
  1. Repasar que el lugar de escena del beat aparece en el esquema como obligatorio y coincide con la implementación.

### [ ] TASK-034 Contrato: openapi - lugar y tono de la portada en el guion

- **Cubre:** REQ-017, REQ-037, DEC-001
- **Componente:** story-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryService.java. Contrato: docs/contracts/openapi/schemas/story-outline.yaml, story-script.yaml
- **Depende de:** TASK-004
- **Alcance:** Actualizar `story-outline.yaml` y `story-script.yaml` para incluir el lugar y el tono propios de la portada, ausentes en los cuentos anteriores.
- **Hecho cuando:** el contrato existe en `docs/contracts/openapi/` y describe el guion ampliado tal como quedó implementado.
- **Pruebas automáticas:**
  - El documento OpenAPI completo es válido según la especificación OpenAPI.
- **Prueba manual:**
  1. Repasar que el lugar y el tono de la portada aparecen en ambos esquemas, que se indica su ausencia en los cuentos anteriores y que coinciden con la implementación.

### [ ] TASK-035 Contrato: openapi - lugar de escena en la edición de beat

- **Cubre:** REQ-017
- **Componente:** story-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryService.java. Contrato: docs/contracts/openapi/paths/story/beat-edit-service.yaml, requests/beat-edit-request.yaml
- **Depende de:** TASK-005
- **Alcance:** Actualizar la edición directa de un beat para que incluya el lugar de escena y su rechazo cuando está vacío.
- **Hecho cuando:** el contrato existe en `docs/contracts/openapi/` y describe la edición tal como quedó implementada, incluido el rechazo por lugar vacío.
- **Pruebas automáticas:**
  - El documento OpenAPI completo es válido según la especificación OpenAPI.
- **Prueba manual:**
  1. Repasar que la petición incluye el lugar y que la respuesta de rechazo por lugar vacío coincide con la implementación.

### [ ] TASK-036 Contrato: openapi - aprobar el guion lanza la portada

- **Cubre:** REQ-038, REQ-040, DEC-002, ALT-003
- **Componente:** story-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryService.java. Contrato: docs/contracts/openapi/paths/story/approve-service.yaml
- **Depende de:** TASK-011
- **Alcance:** Actualizar la aprobación del guion para indicar que lanza la generación de la portada, que la respuesta informa de si empezó y que la aprobación no falla por ello.
- **Hecho cuando:** el contrato existe en `docs/contracts/openapi/` y describe la aprobación tal como quedó implementada.
- **Pruebas automáticas:**
  - El documento OpenAPI completo es válido según la especificación OpenAPI.
- **Prueba manual:**
  1. Repasar que la respuesta de aprobación distingue si la portada empezó o no a generarse, y que coincide con la implementación.

### [ ] TASK-037 Contrato: openapi - generación de portada sin planteamiento

- **Cubre:** REQ-038, DEC-004, ALT-006
- **Componente:** story-page-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageService.java. Contrato: docs/contracts/openapi/paths/story/cover-generate-service.yaml
- **Depende de:** TASK-008, TASK-011
- **Alcance:** Actualizar la generación de portada con el rechazo cuando el guion no tiene lugar y tono de portada, e indicar que también se lanza sola al aprobar el guion.
- **Hecho cuando:** el contrato existe en `docs/contracts/openapi/` y describe la operación tal como quedó implementada, con el nuevo rechazo.
- **Pruebas automáticas:**
  - El documento OpenAPI completo es válido según la especificación OpenAPI.
- **Prueba manual:**
  1. Repasar que el rechazo por falta de lugar y tono de portada figura con su respuesta y coincide con la implementación.

### [ ] TASK-038 Contrato: openapi - hint de portada, bloqueo de página 1 y aceptación sin cascada

- **Cubre:** REQ-008, REQ-039, REQ-040, REQ-041, REQ-043, DEC-003, DEC-004
- **Componente:** story-page-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageService.java. Contrato: docs/contracts/openapi/paths/story/page-hint-service.yaml, page-generate-service.yaml, page-accept-service.yaml, page-accept-proposal-service.yaml
- **Depende de:** TASK-008, TASK-009, TASK-010
- **Alcance:** Actualizar las operaciones para admitir el hint en la portada pendiente o aceptada, con su rechazo por falta de lugar y tono. Además, la página 1 se rechaza sin portada aceptada, y aceptar una versión nueva de la portada no aplica la cascada y retira solo su imagen.
- **Hecho cuando:** el contrato existe en `docs/contracts/openapi/` y describe las cuatro operaciones tal como quedaron implementadas.
- **Pruebas automáticas:**
  - El documento OpenAPI completo es válido según la especificación OpenAPI.
- **Prueba manual:**
  1. Repasar que el hint de portada, el rechazo de la página 1 sin portada aceptada y la aceptación de la portada sin cascada figuran y coinciden con la implementación.

### [ ] TASK-039 Contrato: openapi - subida de imagen de portada o página

- **Cubre:** REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, DEC-013
- **Componente:** story-page-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageService.java. Contrato: docs/contracts/openapi/, fichero por definir (analista técnico)
- **Depende de:** TASK-014
- **Alcance:** Formalizar o crear la operación de subida: solo para contenido aceptado, acepta cualquier imagen, rechaza lo que no es imagen conservando la anterior y sustituye la imagen previa.
- **Hecho cuando:** el contrato existe en `docs/contracts/openapi/` y describe la subida tal como quedó implementada, con los rechazos por fichero que no es imagen y por contenido sin aceptar.
- **Pruebas automáticas:**
  - El documento OpenAPI completo es válido según la especificación OpenAPI.
- **Prueba manual:**
  1. Repasar que la operación, sus dos rechazos y la ausencia de límite de formato o tamaño figuran y coinciden con la implementación.

### [ ] TASK-040 Contrato: openapi - exportación para Nubi Game

- **Cubre:** REQ-009, REQ-010, REQ-016, REQ-035, DEC-004, DEC-006
- **Componente:** story-export-service | por definir (analista técnico). Contrato: docs/contracts/openapi/, fichero por definir (analista técnico)
- **Depende de:** TASK-025
- **Alcance:** Formalizar la operación de exportación para Nubi Game, que sustituye a la actual: devuelve el zip nuevo o la lista de faltas por portada o página, incluidas las imágenes no convertibles.
- **Hecho cuando:** el contrato existe en `docs/contracts/openapi/` y describe la operación tal como quedó implementada, con la respuesta de rechazo y sus tipos de falta, y la operación anterior ya no figura.
- **Pruebas automáticas:**
  - El documento OpenAPI completo es válido según la especificación OpenAPI.
- **Prueba manual:**
  1. Repasar que la respuesta con zip y la de rechazo (portada sin aceptar, texto aceptado, imagen, audio, imagen no convertible, cada una con su portada o página) coinciden con la implementación.

### [ ] TASK-041 Contrato: openapi - exportación para el agente visual

- **Cubre:** REQ-018, REQ-019, REQ-026, REQ-032, DEC-006
- **Componente:** story-export-service | por definir (analista técnico). Contrato: docs/contracts/openapi/, fichero por definir (analista técnico)
- **Depende de:** TASK-030
- **Alcance:** Crear la operación de exportación para el agente visual: devuelve el zip visual o la lista de condiciones o reglas del contrato incumplidas.
- **Hecho cuando:** el contrato existe en `docs/contracts/openapi/` y describe la operación tal como quedó implementada, con la respuesta de rechazo.
- **Pruebas automáticas:**
  - El documento OpenAPI completo es válido según la especificación OpenAPI.
- **Prueba manual:**
  1. Repasar que la respuesta con zip y la de rechazo (condiciones de REQ-018, reglas del contrato y fallo de la imagen del protagonista) coinciden con la implementación.

### [ ] TASK-042 Contrato: file/json - zip para Nubi Game

- **Cubre:** REQ-011, REQ-012, REQ-013, REQ-014, REQ-015, REQ-033, REQ-034, DEC-014
- **Componente:** nubi-game-exporter | no consta en el mapa. Contrato: docs/contracts/nubi-game/story-export.md (nuevo, según "Contratos afectados")
- **Depende de:** TASK-024
- **Alcance:** Crear el contrato del zip para Nubi Game: `story.json` con título, ruta de la portada y páginas en orden con texto, ruta de imagen y ruta de audio; rutas relativas a la raíz; imágenes en WebP con el lado mayor de 2048 px como máximo.
- **Hecho cuando:** el contrato existe en `docs/contracts/nubi-game/story-export.md` y describe el zip tal como quedó implementado.
- **Pruebas automáticas:**
  - El `story.json` de un zip exportado en TASK-024 cumple la estructura descrita en el contrato.
- **Prueba manual:**
  1. Repasar que cada campo del `story.json`, la estructura del zip y las reglas de imagen coinciden con un zip exportado.

### [ ] TASK-043 Contrato: file/json - StoryTeller como productor del agente visual

- **Cubre:** REQ-020, REQ-021, REQ-022, REQ-023, REQ-024, REQ-025, REQ-026, REQ-027, REQ-028, REQ-029, REQ-030, REQ-031
- **Componente:** visual-agent-exporter | por definir (analista técnico). Contrato: docs/contracts/agent-visual/input.md
- **Depende de:** TASK-029, TASK-030
- **Alcance:** Actualizar el estado y la trazabilidad del contrato v0.1 para reflejar que StoryTeller es un productor implementado por esta feature, sin cambiar su estructura.
- **Hecho cuando:** el contrato en `docs/contracts/agent-visual/input.md` indica que StoryTeller es un productor implementado y lo traza a esta feature, con la estructura v0.1 intacta.
- **Pruebas automáticas:**
  No aplica: solo cambian el estado y la trazabilidad, no la estructura. La conformidad del zip se prueba en TASK-028 y TASK-029.
- **Prueba manual:**
  1. Repasar que las secciones de estructura y de validación no han cambiado y que el estado y la trazabilidad citan esta feature.

## Cobertura

| Requisito | Tareas |
|---|---|
| REQ-001 | TASK-015, TASK-019 |
| REQ-002 | TASK-014, TASK-018, TASK-020, TASK-039 |
| REQ-003 | TASK-014, TASK-018, TASK-020, TASK-039 |
| REQ-004 | TASK-013, TASK-014, TASK-020, TASK-039 |
| REQ-005 | TASK-013, TASK-014, TASK-020, TASK-039 |
| REQ-006 | TASK-014, TASK-020, TASK-039 |
| REQ-007 | TASK-010, TASK-014, TASK-020, TASK-039 |
| REQ-008 | TASK-010, TASK-038 |
| REQ-009 | TASK-025, TASK-031, TASK-032, TASK-040 |
| REQ-010 | TASK-025, TASK-032, TASK-040 |
| REQ-011 | TASK-024, TASK-042 |
| REQ-012 | TASK-024, TASK-042 |
| REQ-013 | TASK-024, TASK-042 |
| REQ-014 | TASK-024, TASK-042 |
| REQ-015 | TASK-023, TASK-024, TASK-042 |
| REQ-016 | TASK-024, TASK-025, TASK-031, TASK-032, TASK-040 |
| REQ-017 | TASK-001, TASK-002, TASK-003, TASK-004, TASK-005, TASK-016, TASK-017, TASK-033, TASK-034, TASK-035 |
| REQ-018 | TASK-030, TASK-031, TASK-032, TASK-041 |
| REQ-019 | TASK-030, TASK-032, TASK-041 |
| REQ-020 | TASK-029, TASK-043 |
| REQ-021 | TASK-028, TASK-029, TASK-043 |
| REQ-022 | TASK-022, TASK-029, TASK-043 |
| REQ-023 | TASK-028, TASK-029, TASK-043 |
| REQ-024 | TASK-022, TASK-026, TASK-029, TASK-043 |
| REQ-025 | TASK-026, TASK-027, TASK-030, TASK-043 |
| REQ-026 | TASK-028, TASK-030, TASK-032, TASK-041, TASK-043 |
| REQ-027 | TASK-001, TASK-029, TASK-043 |
| REQ-028 | TASK-029, TASK-043 |
| REQ-029 | TASK-001, TASK-029, TASK-043 |
| REQ-030 | TASK-028, TASK-029, TASK-043 |
| REQ-031 | TASK-028, TASK-029, TASK-043 |
| REQ-032 | TASK-022, TASK-028, TASK-030, TASK-032, TASK-041 |
| REQ-033 | TASK-023, TASK-024, TASK-042 |
| REQ-034 | TASK-023, TASK-042 |
| REQ-035 | TASK-023, TASK-025, TASK-032, TASK-040 |
| REQ-036 | TASK-015, TASK-019 |
| REQ-037 | TASK-001, TASK-002, TASK-003, TASK-004, TASK-016, TASK-017, TASK-034 |
| REQ-038 | TASK-006, TASK-007, TASK-008, TASK-011, TASK-021, TASK-036, TASK-037 |
| REQ-039 | TASK-006, TASK-007, TASK-008, TASK-018, TASK-021, TASK-038 |
| REQ-040 | TASK-009, TASK-011, TASK-021, TASK-036, TASK-038 |
| REQ-041 | TASK-010, TASK-021, TASK-038 |
| REQ-042 | TASK-012 |
| REQ-043 | TASK-010, TASK-012, TASK-021, TASK-038 |
