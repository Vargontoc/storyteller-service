# Diseño: export-import-feature

Estado: aprobado
Revisión: 2
Basado en: requirements@7

## Resumen

El guion amplía cada beat con su lugar de escena y añade un lugar y un tono propios de la portada. Al aprobarse, el guion lanza solo la generación de la descripción de escena de la portada. La portada pasa a ser una página 0 que se revisa con hint, queda fuera de la cascada de PDEC-020 y es requisito para la página 1. La importación de imágenes se apoya en la imagen ligada al contenido aceptado que ya existe, con detección de imagen por contenido. Dos exportaciones síncronas bajo demanda, orquestadas por un servicio de exportación, evalúan primero todas las condiciones y después generan cada zip sin persistirlo: el zip para Nubi Game, con `story.json` de referencias explícitas y las imágenes originales sin modificar, cada una con la extensión de su formato real detectado por contenido, sustituye al actual, y el zip para el agente visual se valida contra el contrato v0.1 antes de entregarse.

## Supuestos sobre el sistema actual

- La portada es una página 0 del cuento con descripción de escena propia, histórico de versiones y una imagen importada ligada a su contenido aceptado. Así lo indican `story-page-service`, `story-page-repository`, `accepted-content.yaml` y `cover-generate-service.yaml`.
- Ya existe una operación para subir una imagen al contenido aceptado de una portada o página, porque `story-pages-panel` muestra la imagen importada. El mapa no la referencia: solo figura la descarga (`page-image-service.yaml`). Se asume que existe y que esta feature la amplía. No se sabe cómo valida hoy el fichero.
- Ya existe una primera exportación del zip para Nubi Game, con la convención `page_N` (FUN-018, SUP-003), junto con su acción en la interfaz. El mapa no la referencia. Se asume que está en `storyteller-server` y que se sustituye por completo.
- La instantánea del protagonista de cada cuento (`StoryActorSnapshot`) conserva el nombre, la descripción visual y el retrato. El mapa solo la cita de pasada en `catalog-character-usage-adapter`. Se asume que hay un acceso de lectura a ella desde `storyteller-server`.
- "Rehacer el guion" (REQ-042, REQ-043) significa que el director entrega una escaleta completa nueva para un cuento cuyo guion ya estaba aprobado, al confirmarse una revisión global por hint, y que el adulto debe volver a aprobarla. La edición directa de un beat (`beat-editor`) no rehace el guion ni toca el lugar y el tono de la portada, así que no descarta la portada. El usuario aclara que "rehacer el guion" es el mecanismo ya implementado, que este diseño no redefine.
- Cada beat ya tiene un tono (SUP-010): es el campo `tone` de `story-beat.yaml`.
- `comfyui-client` permite subir una imagen, encolar un workflow y descargar el resultado. La instalación de ComfyUI puede incorporar un nodo de eliminación de fondo.
- `director-model-provisioner` y `writer-model-provisioner` solo registran el modelo si no existe. Las reglas nuevas de los prompts necesitan que el modelo se vuelva a registrar.

## Componentes

| Componente | Responsabilidad | Existente o nuevo |
|---|---|---|
| director-agent | Genera el guion con lugar de escena en cada beat y con lugar y tono propios de la portada, también en la revisión global por hint | existente (se amplía) |
| director-model-provisioner | Registra el Modelfile del director con las reglas de lugar de escena y de lugar y tono de la portada | existente (se amplía) |
| story-outline-repository | Persiste el guion con el lugar de escena de cada beat y el lugar y el tono de la portada | existente (se amplía) |
| story-service | Valida los campos ampliados del guion; al aprobarlo, lanza la generación de la portada; al rehacerlo, descarta la portada | existente (se amplía) |
| beat-editor | Trata el lugar de escena como un campo más del beat editable y validado | existente (se amplía) |
| story-page-service | Genera la portada y le aplica hints a partir de su lugar y su tono, la excluye de la cascada, exige que esté aceptada antes de la página 1 y gestiona la subida de imágenes | existente (se amplía) |
| writer-agent | Redacta la descripción de escena de la portada, y su nueva versión con hint, a partir del lugar y el tono propios de la portada | existente (se amplía) |
| writer-model-provisioner | Registra el Modelfile del escritor con las reglas de la escena de portada desde su lugar y su tono | existente (se amplía) |
| story-page-repository | Conserva la imagen importada ligada al contenido aceptado de la portada o página y la retira cuando ese contenido se reaprueba o se descarta | existente |
| story-page-version-repository | Guarda el histórico de versiones de la portada igual que el de las páginas | existente |
| generation-gate | Concede el turno único para la generación de la portada, ya sea automática o pedida | existente |
| story-page-notifier | Publica los eventos de generación de la portada (STARTED, VERSION_PRESENTED, HINT_REJECTED, FAILED) | existente |
| image-content-inspector | Decide por su contenido real si un fichero subido es una imagen e identifica su formato real, sin mirar nombre, tipo declarado ni tamaño | nuevo |
| story-protagonist-snapshot | Da acceso de lectura a la instantánea del protagonista del cuento: nombre, descripción visual y retrato | existente (sin mapear) |
| narration-repository | Da el audio que la narración tiene asociado a cada página interior (SUP-020) | existente |
| story-export-service | Orquesta las dos exportaciones: evalúa las condiciones, acumula todas las faltas y entrega el zip o el rechazo | nuevo |
| nubi-game-exporter | Compone el zip para Nubi Game con `story.json` de rutas explícitas, la imagen de portada y, por página, su imagen y su audio, con cada imagen copiada sin modificar y con la extensión de su formato real | existente (sin mapear; se sustituye) |
| visual-agent-exporter | Compone el `story.json` v0.1 y el zip para el agente visual a partir del guion, las páginas aceptadas y la instantánea | nuevo |
| character-background-remover | Obtiene la imagen del protagonista con fondo transparente a partir del retrato de la instantánea | nuevo |
| comfyui-client | Ejecuta en ComfyUI el workflow de eliminación de fondo y descarga el resultado | existente |
| visual-contract-validator | Comprueba el `story.json` y la imagen del personaje contra las reglas de la sección 5 del contrato v0.1 y reporta todas las que se incumplen | nuevo |
| story-slugger | Deriva identificadores slug deterministas desde un texto (SUP-009, SUP-013) | nuevo |
| story-script-panel | Muestra en la escaleta el lugar de escena de cada beat y el lugar y el tono de la portada para aprobarlos, y permite editar el lugar en la edición directa de un beat | existente (se amplía) |
| story-script-api | Transporta en el guion los campos ampliados | existente (se amplía) |
| story-pages-panel | Muestra la descripción de escena completa de la portada y de cada página aceptada, ofrece subir imagen solo en contenido aceptado, gestiona la revisión con hint de la portada y avisa de que a la portada le faltan el lugar y el tono | existente (se amplía) |
| story-pages-api | Cliente de la subida de imagen y de las operaciones de portada | existente (se amplía) |
| story-export-actions | Acciones de la interfaz para pedir cada exportación, descargar el zip o mostrar la lista de faltas | existente (sin mapear; se amplía) |
| story-export-api | Cliente REST de las dos exportaciones | nuevo |

## Flujo principal

1. El adulto crea un cuento. `story-service` pide el guion a `director-agent`, que devuelve título, sinopsis, reparto, la escaleta con lugar de escena y tono en cada beat, y el lugar y el tono propios de la portada. `story-service` valida que ninguno esté vacío y `story-outline-repository` lo persiste (REQ-017, REQ-037).
2. `story-script-panel` muestra la escaleta con el lugar de cada beat y el lugar y el tono de la portada. El adulto la aprueba (SUP-011).
3. Al aprobarse el guion, `story-service` lanza la generación de la portada con la misma operación que existe para generarla. Esa operación pide turno a `generation-gate`, `writer-agent` redacta la descripción de escena de la portada a partir de su lugar y su tono, y `story-page-notifier` la presenta pendiente de revisión. La página 1 todavía no existe (REQ-038).
4. El adulto acepta la descripción de la portada o pide un cambio con hint. En ese caso `writer-agent` la regenera completa o rechaza el hint con motivo, con el mismo procedimiento que las páginas (REQ-039). Mientras la portada no se haya aceptado por primera vez, `story-page-service` rechaza la generación de la página 1 (REQ-040).
5. Con la portada aceptada se habilita la página 1, que sigue generándose bajo demanda. Las páginas interiores se generan y se aceptan una a una, como hasta ahora. `story-pages-panel` muestra la descripción de escena completa de la portada y de cada página aceptada (REQ-001, REQ-036).
6. El adulto sube una imagen en la portada o en una página aceptada. `story-page-service` pide a `image-content-inspector` que confirme que el fichero es una imagen, y `story-page-repository` la asocia al contenido aceptado de esa portada o página, sustituyendo la anterior si la había (REQ-002 a REQ-007).
7. Si una página interior se reaprueba o cae en la cascada de PDEC-020, `story-page-service` retira su imagen y la de las páginas descartadas (REQ-008). Si se reaprueba la portada, se retira solo la imagen de la portada y las páginas interiores no cambian (REQ-041, REQ-043).
8. La narración sigue funcionando como hasta ahora (SUP-020).
9. Exportación para Nubi Game: `story-export-actions` la pide. `story-export-service` comprueba todas las condiciones: portada aceptada, entendida como aceptada y con un guion que tiene lugar y tono de portada (SUP-021), todas las páginas aceptadas, imagen en la portada e imagen y audio en cada página, este último tomado de `narration-repository`. Después, `nubi-game-exporter` pide a `image-content-inspector` el formato real de cada imagen importada, copia cada imagen al zip tal como se subió, en un fichero cuya extensión corresponde a ese formato con independencia del nombre con que se subió, y compone el `story.json` (título, ruta de la portada y, por página y en orden, el texto aceptado y las rutas de su imagen y su audio), sin declarar el formato en ningún campo. El adulto descarga el zip (REQ-009 a REQ-016).
10. Exportación para el agente visual: `story-export-actions` la pide. `story-export-service` comprueba las condiciones de REQ-018. `visual-agent-exporter` obtiene de `story-protagonist-snapshot` el nombre, la descripción visual y el retrato. `character-background-remover` quita el fondo del retrato mediante `comfyui-client`. `story-slugger` deriva el `storyId` y el `id` del protagonista. `visual-agent-exporter` compone el `story.json`: campos fijos, paleta base, el protagonista como único personaje con 1 a 5 rasgos, una escena `cover` con la descripción, el lugar y el tono de la portada, y una escena `scene` por página con su texto, su descripción, el lugar y el tono de su beat, todas referenciando al protagonista. `visual-contract-validator` comprueba en una sola pasada el `story.json` y `characters/<id>/character.png`. Si no hay errores, el adulto descarga el zip (REQ-018 a REQ-032).

## Flujos alternativos y de fallo

### ALT-001 Fichero subido que no es imagen

- **Condición:** El adulto sube en la portada o en una página un fichero cuyo contenido no es una imagen.
- **Comportamiento:** Se rechaza la subida con un mensaje que indica que el fichero no es una imagen. No se toca la imagen que había.
- **Responsable:** story-page-service
- **Resultado:** La portada o página sigue mostrando su imagen anterior, si la tenía.
- **Cubre:** REQ-005

### ALT-002 Subida en una portada o página sin aceptar

- **Condición:** Se intenta subir una imagen en una portada o página interior cuyo contenido no está aceptado.
- **Comportamiento:** La interfaz no ofrece la opción. Si la petición llega de todos modos, se rechaza sin asociar ninguna imagen.
- **Responsable:** story-page-service
- **Resultado:** La portada o página no tiene imagen importada.
- **Cubre:** REQ-002, REQ-003

### ALT-003 La portada no puede empezar a generarse al aprobar el guion

- **Condición:** Al aprobarse el guion hay otra generación en curso o el escritor no está disponible, y la generación de la portada no puede empezar.
- **Comportamiento:** La aprobación del guion se completa y no hay reintento automático. El adulto puede pedir la portada con la operación que ya existe.
- **Responsable:** story-service
- **Resultado:** Guion aprobado, portada sin descripción de escena y página 1 bloqueada hasta que la portada se acepte.
- **Cubre:** REQ-038, REQ-040

### ALT-004 Generación de la portada interrumpida

- **Condición:** Una generación de portada ya empezada (sea la inicial o una nueva versión con hint) falla antes de presentar la descripción.
- **Comportamiento:** Se publica el evento FAILED, como en las páginas. La generación no se reintenta sola y la portada se queda en el estado que tenía antes.
- **Responsable:** story-page-service
- **Resultado:** La portada sigue sin descripción, o con su versión anterior. El adulto puede volver a pedirla.
- **Cubre:** REQ-038, REQ-039

### ALT-005 Hint de portada rechazado por el escritor

- **Condición:** El escritor considera que el hint pedido para la portada no es aceptable.
- **Comportamiento:** Se publica HINT_REJECTED con el motivo y se conserva la versión vigente de la portada (PDEC-021).
- **Responsable:** writer-agent
- **Resultado:** La portada no cambia y el adulto ve el motivo.
- **Cubre:** REQ-039

### ALT-006 Cuento sin lugar ni tono propios de la portada

- **Condición:** En un cuento cuyo guion no tiene lugar y tono propios de la portada (cuentos anteriores a la feature), se pide generar la portada o un cambio con hint sobre ella.
- **Comportamiento:** Se rechaza la petición sin llamar al escritor, con un mensaje que indica que el guion no tiene lugar y tono de portada.
- **Responsable:** story-page-service
- **Resultado:** La portada no cambia y el cuento sigue sin poder exportarse (SUP-021).
- **Cubre:** REQ-038, REQ-039

### ALT-007 El director devuelve un guion sin los campos nuevos

- **Condición:** El director devuelve, al crear el guion o en una revisión global, algún beat sin lugar de escena, o la portada sin lugar o sin tono.
- **Comportamiento:** El guion se trata como una salida del director que no supera la validación de campos obligatorios, igual que hoy cuando falta cualquier campo del beat.
- **Responsable:** story-service
- **Resultado:** No se persiste un guion sin esos campos.
- **Cubre:** REQ-017, REQ-037

### ALT-008 Rehacer el guion con la portada ya generada

- **Condición:** Se rehace el guion de un cuento que ya tiene descripción de escena de la portada, pendiente o aceptada, con o sin imagen importada.
- **Comportamiento:** Se descartan la descripción de la portada (versiones y contenido aceptado) y su imagen importada. Cuando el adulto aprueba el nuevo guion, la portada se genera como en el paso 3 del flujo principal.
- **Responsable:** story-service
- **Resultado:** La portada deja de estar aceptada, no muestra imagen y, tras aprobarse el nuevo guion, presenta una descripción nueva pendiente de revisión.
- **Cubre:** REQ-042, REQ-043

### ALT-009 Exportación para Nubi Game con el cuento incompleto

- **Condición:** Al pedir la exportación para Nubi Game falta alguna condición de REQ-009. Incluye la portada que no cuenta como aceptada porque el guion no tiene lugar y tono de portada.
- **Comportamiento:** Se rechaza sin generar el zip y se listan todas las faltas: portada sin aceptar, texto aceptado, imagen o audio, cada una con la portada o página en que falta.
- **Responsable:** story-export-service
- **Resultado:** No hay zip y el adulto ve la lista completa de faltas.
- **Cubre:** REQ-009, REQ-010

### ALT-010 Imagen que no se puede convertir para Nubi Game (retirado)

- **Retirado en la revisión 2.** Las imágenes se exportan sin conversión (SUP-007) y REQ-035 está retirado, así que la exportación para Nubi Game solo se rechaza por las faltas de ALT-009. El ID no se reutiliza.

### ALT-011 Exportación para el agente visual sin las condiciones

- **Condición:** Falta alguna condición de REQ-018: guion sin aprobar, portada sin aceptar, alguna página sin aceptar, beats sin lugar de escena o portada sin lugar y tono.
- **Comportamiento:** Se rechaza sin generar el zip y se indican todas las condiciones que faltan, con la página cuando corresponde.
- **Responsable:** story-export-service
- **Resultado:** No hay zip y el adulto ve qué falta.
- **Cubre:** REQ-019, REQ-018

### ALT-012 Imagen del protagonista no conforme o imposible de obtener

- **Condición:** La imagen sin fondo obtenida del retrato de la instantánea incumple alguna regla del contrato (PNG real, alfa con fondo transparente, al menos 1024 × 1024 px, proporción 1:1), o la eliminación de fondo no puede completarse.
- **Comportamiento:** Se rechaza la exportación sin generar el zip, indicando cada regla incumplida o que no se pudo obtener la imagen del protagonista. No hay reintento automático.
- **Responsable:** story-export-service
- **Resultado:** No hay zip y la instantánea y su retrato no cambian.
- **Cubre:** REQ-026, REQ-025

### ALT-013 `story.json` visual que incumple el contrato

- **Condición:** El `story.json` compuesto incumple alguna regla del contrato v0.1, por ejemplo un `storyId` de menos de 3 caracteres o una `visualDescription` de portada vacía.
- **Comportamiento:** Se rechaza la exportación sin generar el zip y se indican todas las reglas incumplidas, junto con las de la imagen del protagonista si las hay, en una sola respuesta.
- **Responsable:** visual-contract-validator
- **Resultado:** No hay zip y el adulto ve la lista completa de reglas incumplidas.
- **Cubre:** REQ-032, REQ-031

## Interfaces entre componentes

| Origen | Destino | Qué se comunica | Protocolo |
|---|---|---|---|
| story-service | director-agent | Pide el guion o su revisión global y recibe la escaleta con lugar y tono por beat y lugar y tono de la portada | - |
| story-service | story-outline-repository | Guarda y lee el guion ampliado | - |
| story-service | story-page-service | Al aprobar el guion, pide iniciar la generación de la portada. Al rehacerlo, pide descartar la portada y su imagen | - |
| beat-editor | story-outline-repository | Guarda el beat editado con su lugar de escena | - |
| story-page-service | generation-gate | Pide y libera el turno de la generación de la portada | - |
| story-page-service | writer-agent | Pide la descripción de escena de la portada, o su nueva versión con hint, con el lugar y el tono propios de la portada | - |
| story-page-service | story-page-notifier | Avisa del inicio, la presentación, el rechazo de hint o el fallo de la portada | - |
| story-page-service | image-content-inspector | Pregunta si un fichero subido es una imagen | - |
| story-page-service | story-page-repository | Asocia, sustituye o retira la imagen del contenido aceptado y conserva las páginas interiores al reaprobar la portada | - |
| story-page-notifier | story-pages-panel | Eventos de generación de la portada y las páginas, también de la portada lanzada al aprobar el guion | websocket |
| story-script-api | story-service | Consulta, aprobación y edición de beat del guion ampliado | openapi |
| story-pages-api | story-page-service | Generación de la portada, hints, aceptación y subida de imagen en la portada o en las páginas | openapi |
| story-export-api | story-export-service | Pide una exportación y recibe el zip o la lista de faltas o reglas incumplidas | openapi |
| story-export-service | story-outline-repository | Lee el guion: estado de aprobación, lugar y tono de los beats y de la portada | - |
| story-export-service | story-page-repository | Lee el contenido aceptado de la portada y las páginas y sus imágenes importadas | - |
| story-export-service | narration-repository | Lee el audio asociado a cada página interior | - |
| story-export-service | nubi-game-exporter | Le entrega el cuento completo para componer el zip para Nubi Game | - |
| nubi-game-exporter | image-content-inspector | Pide el formato real de cada imagen importada para fijar la extensión de su fichero en el zip | - |
| story-export-service | visual-agent-exporter | Le entrega el cuento que cumple REQ-018 para componer el zip visual | - |
| visual-agent-exporter | story-protagonist-snapshot | Lee el nombre, la descripción visual y el retrato de la instantánea | - |
| visual-agent-exporter | story-slugger | Pide los slugs del `storyId` y del `id` del protagonista | - |
| visual-agent-exporter | character-background-remover | Pide la imagen del protagonista con fondo transparente | - |
| character-background-remover | comfyui-client | Sube el retrato, ejecuta en ComfyUI el workflow de eliminación de fondo y descarga el PNG resultante | http (ComfyUI) |
| visual-agent-exporter | visual-contract-validator | Le entrega el `story.json` y la imagen del personaje y recibe la lista de reglas incumplidas | - |
| nubi-game-exporter | story-export-api | Zip con `story.json` y assets referenciados, destinado a Nubi Game | file/json |
| visual-agent-exporter | story-export-api | Zip con `story.json` v0.1 y `characters/<id>/character.png`, destinado al agente de recursos visuales | file/json |

## Modelo de datos lógico

- Guion del cuento (StoryOutline): título, sinopsis, reparto y escaleta aprobables. Relaciones: tiene un Beat por página interior y un Planteamiento de portada.
- Beat: lo que ocurre en una página interior, ampliado con su lugar de escena, además de su tono. Relaciones: pertenece al guion y alimenta una página interior.
- Planteamiento de portada: lugar de escena y tono propios de la portada, generados por el director y aprobados con la escaleta. Relaciones: pertenece al guion y alimenta la portada. Los cuentos anteriores a la feature no lo tienen.
- Portada (página 0): descripción de escena con versiones y contenido aceptado. Relaciones: pertenece al cuento, nace del planteamiento de portada y puede tener una imagen importada.
- Página interior: texto y descripción de escena con versiones y contenido aceptado. Relaciones: pertenece al cuento, nace de un beat y puede tener una imagen importada y un audio de narración.
- Imagen importada: fichero original subido por el adulto. Relaciones: ligada al contenido aceptado de una portada o página, y como mucho una por cada una.
- Instantánea del protagonista: nombre, descripción visual y retrato con que nació el cuento. Relaciones: pertenece al cuento y es la fuente del personaje en la exportación visual.
- Exportación para Nubi Game: zip generado bajo demanda y no persistido. Relaciones: deriva del guion, la portada, las páginas, sus imágenes importadas sin modificar y sus audios.
- Exportación para el agente visual: zip generado bajo demanda y no persistido. Relaciones: deriva del guion, la portada, las páginas y la instantánea del protagonista.

## Decisiones

### DEC-001 El lugar y el tono de la portada forman parte del guion

- **Decisión:** El lugar y el tono propios de la portada se guardan en el guion, junto a la escaleta. Los genera el director en la misma respuesta y el adulto los aprueba con ella. El lugar de escena se añade como un campo más de cada beat.
- **Alternativas descartadas:** Guardarlos con la portada (página 0): el director los genera y el adulto los aprueba con el guion (SUP-011), y rehacer el guion debe renovarlos. Pedirlos al director en una llamada aparte: añade latencia y otro punto de fallo.
- **Cubre:** REQ-017, REQ-037, REQ-042

### DEC-002 Generación automática de la portada al aprobar el guion

- **Decisión:** Aprobar el guion lanza la generación de la portada con la misma operación que existe para pedirla, que respeta `generation-gate`. Si no puede empezar, el guion queda aprobado y la portada se pide a mano, sin reintentos. Aceptar la portada solo habilita la página 1, que no se genera sola.
- **Alternativas descartadas:** Generar la portada dentro de la propia aprobación y de forma síncrona: bloquea la aprobación mientras escribe el escritor. Encadenar también la página 1: la decisión del usuario mantiene la página 1 bajo demanda (PDEC-020, sin gasto de GPU en cascada).
- **Cubre:** REQ-038, REQ-040

### DEC-003 La portada queda fuera de la cascada de PDEC-020

- **Decisión:** Reaprobar la portada solo afecta a la portada: retira su imagen y conserva las páginas interiores con su texto, su descripción, su imagen y su audio. La cascada sigue aplicándose desde la página interior que cambia. La portada solo se descarta al rehacer el guion.
- **Alternativas descartadas:** Tratar la portada como página 0 dentro de la cascada: contradice REQ-041.
- **Cubre:** REQ-041, REQ-043, REQ-008

### DEC-004 "Portada aceptada" exige el planteamiento de portada en el guion

- **Decisión:** En las dos exportaciones, la portada solo cuenta como aceptada si está aceptada y además el guion tiene lugar y tono propios de la portada. Si no, la exportación para Nubi Game se rechaza con "portada sin aceptar". En esos cuentos, pedir generar la portada o un cambio con hint se rechaza sin llamar al escritor.
- **Alternativas descartadas:** Dar por aceptada la portada de los cuentos existentes para Nubi Game: contradice SUP-021, validado por el usuario.
- **Cubre:** REQ-009, REQ-010, REQ-018, REQ-019, REQ-038, REQ-039

### DEC-005 Exportaciones síncronas bajo demanda y sin persistir

- **Decisión:** Cada exportación se calcula al pedirla y el zip se entrega como descarga directa. No se guarda en el servidor.
- **Alternativas descartadas:** Exportación asíncrona con aviso por WebSocket: ningún requisito la pide y añade estado de trabajos. Guardar el zip: duplica almacenamiento y obliga a invalidarlo cuando el cuento cambia.
- **Cubre:** REQ-011, REQ-016, REQ-020

### DEC-006 Evaluación completa antes de rechazar

- **Decisión:** `story-export-service` comprueba todas las condiciones de disponibilidad y devuelve todas las faltas juntas. En la exportación visual, la imagen del protagonista y el `story.json` se validan en una sola pasada que acumula todas las reglas incumplidas.
- **Alternativas descartadas:** Detenerse en la primera falta: REQ-010, REQ-019, REQ-026 y REQ-032 piden indicar cada elemento o regla.
- **Cubre:** REQ-010, REQ-019, REQ-026, REQ-032

### DEC-007 Conversión de imágenes al exportar, sobre una copia (retirada)

- **Retirada en la revisión 2.** El usuario descarta la conversión a WebP (SUP-015) y REQ-033, REQ-034 y REQ-035 están retirados. La sustituye DEC-015. El ID no se reutiliza.

### DEC-008 Eliminación del fondo del protagonista con ComfyUI

- **Decisión:** `character-background-remover` usa ComfyUI a través de `comfyui-client` con un workflow de eliminación de fondo. Trabaja sobre el retrato de la instantánea, no sobre el retrato actual del catálogo, y no lo reescala. No pide turno a `generation-gate`, porque ningún requisito permite rechazar la exportación por una generación en curso.
- **Alternativas descartadas:** Una librería dentro del backend: no hay en el stack ninguna opción madura en Java para segmentar el fondo. Un contenedor dedicado: añade otro servicio que desplegar para una operación puntual. Ampliar el retrato hasta 1024 px: inventaría píxeles y REQ-026 pide rechazar.
- **Cubre:** REQ-025, REQ-026

### DEC-009 Validación propia del contrato v0.1 antes de empaquetar

- **Decisión:** `visual-contract-validator` implementa en la aplicación las reglas de la sección 5 del contrato, tomando la raíz del zip como `input/`. Incluye la codificación UTF-8 sin BOM, el JSON sin claves duplicadas, los slugs únicos y las reglas de la imagen. El zip solo se genera si no hay errores.
- **Alternativas descartadas:** Invocar el validador del agente visual: queda fuera de alcance y no existe implementación.
- **Cubre:** REQ-031, REQ-032, REQ-026, REQ-021, REQ-023, REQ-030

### DEC-010 Emoción tomada literalmente del tono

- **Decisión:** La `emotion` de cada escena es el tono aprobado de su beat, o el tono propio de la portada en la escena `cover`, tal como figura en el guion y sin espacios sobrantes.
- **Alternativas descartadas:** Traducir el tono a un vocabulario cerrado de emociones: el contrato no lo exige y habría que inventar una correspondencia. Generarla con un agente al exportar: lo prohíbe PDEC-041.
- **Cubre:** REQ-027, REQ-029

### DEC-011 Rasgos inmutables extraídos de la descripción visual

- **Decisión:** `visual-agent-exporter` obtiene `immutableTraits` dividiendo de forma determinista la descripción visual de la instantánea en sus fragmentos (separados por comas o por frases) y toma los primeros no vacíos, hasta 5. Así cada rasgo aparece literalmente en la descripción.
- **Alternativas descartadas:** Pedir los rasgos a un agente: no es determinista y puede producir rasgos que no aparecen en la descripción.
- **Cubre:** REQ-024

### DEC-012 Identificadores deterministas

- **Decisión:** `story-slugger` aplica la regla de SUP-009 al título, para el `storyId`, y al nombre de la instantánea, para el `id` del protagonista (SUP-013). Los `id` de las escenas son posicionales y fijos (una portada y una escena por página en orden), de modo que dos exportaciones del mismo cuento sin cambios dan los mismos identificadores.
- **Alternativas descartadas:** Usar identificadores internos de la base de datos: no cumplen el patrón del `storyId` y no dicen nada al consumidor.
- **Cubre:** REQ-020, REQ-022, REQ-031

### DEC-013 Detección de imagen por contenido

- **Decisión:** `image-content-inspector` decide si lo subido es una imagen por la firma o la decodificación de su contenido real, sin tener en cuenta la extensión, el tipo declarado, las dimensiones ni el peso. La subida no aplica ningún límite de tamaño propio de la aplicación.
- **Alternativas descartadas:** Fiarse de la extensión o del tipo declarado: acepta ficheros que no son imagen. Limitar formatos o peso: lo prohíbe PDEC-042.
- **Cubre:** REQ-004, REQ-005

### DEC-014 Rutas explícitas en el zip para Nubi Game

- **Decisión:** `nubi-game-exporter` nombra los ficheros del zip a su criterio y escribe en `story.json` la ruta relativa a la raíz de cada uno: la portada y, por página, su imagen y su audio. El consumidor nunca deduce un fichero por su nombre ni por su posición.
- **Alternativas descartadas:** Mantener la convención `page_N` del zip actual: la sustituye PDEC-040 (SUP-003).
- **Cubre:** REQ-011, REQ-012, REQ-013, REQ-014, REQ-016

### DEC-015 Imágenes originales con extensión según su formato real, detectado al exportar

- **Decisión:** `nubi-game-exporter` copia al zip cada imagen importada tal como se subió, sin convertir, redimensionar, reorientar, recomprimir ni quitar metadatos. La extensión de cada fichero la fija el formato real de la imagen, que `image-content-inspector` identifica por su contenido en el momento de exportar, con la extensión habitual de ese formato (por ejemplo `.png`, `.jpg`, `.bmp`). El `story.json` no declara el formato en ningún campo: basta la extensión de la ruta.
- **Alternativas descartadas:** Usar la extensión del nombre con que se subió el fichero: no corresponde necesariamente al formato real, en contra de REQ-011. Guardar el formato detectado al subir la imagen: añade un dato persistido que el inspector puede obtener al exportar a partir del mismo contenido. Declarar el formato en un campo del `story.json`: lo descarta el usuario (SUP-022). Convertir las imágenes a un formato único: lo descarta el usuario (SUP-007, SUP-015).
- **Cubre:** REQ-011, REQ-014, REQ-015

## Riesgos

- Los provisioners solo registran el modelo si no existe, así que un modelo del director o del escritor ya registrado no aplicaría las reglas nuevas. La capa técnica debe prever cómo se vuelve a registrar.
- La subida sin límite de tamaño (PDEC-042) y la composición síncrona de un zip con imágenes originales grandes pueden consumir mucha memoria y alargar la exportación y la descarga. Se asume como riesgo aceptado en `producto.md` y se vigila.
- Nubi Game puede no admitir algunos formatos que la importación acepta (por ejemplo BMP, TIFF o HEIC). La comprobación queda fuera de alcance y a cargo de Nubi Game (SUP-007).
- Para cada formato que `image-content-inspector` acepte como imagen debe existir una extensión habitual que lo identifique (DEC-015). La capa técnica debe definir esa correspondencia para todos los formatos aceptados.
- Algunos formatos de imagen válidos, como TIFF o HEIC, pueden no verse en el navegador aunque la subida los acepte. REQ-004 solo exige mostrar formatos como PNG o JPEG. Queda como riesgo de visualización.
- La eliminación de fondo comparte la GPU con Ollama y ComfyUI sin pasar por `generation-gate`, lo que puede ralentizar o hacer fallar la exportación si coincide con una generación. En ese caso aplica ALT-012.
- Puede que la instalación de ComfyUI no tenga el nodo de eliminación de fondo, y habría que incorporarlo al contenedor.
- La calidad del recorte puede dejar restos de fondo que el validador detecte como fondo no transparente, con el rechazo de ALT-012.
- Según SUP-012, el retrato puede medir menos de 1024 × 1024 px y la exportación visual se rechazaría por REQ-026. La pregunta abierta de `producto.md` sigue pendiente.
- Una descripción visual de la instantánea sin separadores daría un solo rasgo muy largo (DEC-011). Cumple el contrato, pero es poco útil.
- Los cuentos existentes dejan de poder exportarse para Nubi Game (SUP-021). Lo ha aceptado el usuario.

## Trazabilidad

`requirements.md` no tiene requisitos no funcionales (`NFR-xxx`).

| Requisito | Componentes | Decisiones |
|---|---|---|
| REQ-001 | story-pages-panel, story-pages-api, story-page-service | - |
| REQ-002 | story-pages-panel, story-pages-api, story-page-service, story-page-repository | - |
| REQ-003 | story-pages-panel, story-pages-api, story-page-service, story-page-repository | - |
| REQ-004 | image-content-inspector, story-page-service, story-pages-panel | DEC-013 |
| REQ-005 | image-content-inspector, story-page-service, story-pages-panel | DEC-013 |
| REQ-006 | story-page-service, story-page-repository | - |
| REQ-007 | story-page-repository, story-page-service | DEC-003 |
| REQ-008 | story-page-service, story-page-repository | DEC-003 |
| REQ-009 | story-export-service, narration-repository, story-page-repository, story-outline-repository, story-export-actions, story-export-api | DEC-004 |
| REQ-010 | story-export-service, story-export-actions | DEC-004, DEC-006 |
| REQ-011 | nubi-game-exporter, image-content-inspector, story-page-repository, narration-repository | DEC-005, DEC-014, DEC-015 |
| REQ-012 | nubi-game-exporter | DEC-014 |
| REQ-013 | nubi-game-exporter, narration-repository, story-page-repository | DEC-014 |
| REQ-014 | nubi-game-exporter | DEC-014, DEC-015 |
| REQ-015 | nubi-game-exporter, story-page-repository | DEC-015 |
| REQ-016 | nubi-game-exporter, story-export-actions, story-export-api | DEC-005, DEC-014 |
| REQ-017 | director-agent, director-model-provisioner, story-service, story-outline-repository, beat-editor, story-script-panel, story-script-api | DEC-001 |
| REQ-018 | story-export-service, story-outline-repository, story-page-repository, story-export-actions, story-export-api | DEC-004 |
| REQ-019 | story-export-service, story-export-actions | DEC-004, DEC-006 |
| REQ-020 | visual-agent-exporter, story-slugger | DEC-005, DEC-012 |
| REQ-021 | visual-agent-exporter, visual-contract-validator | DEC-009 |
| REQ-022 | visual-agent-exporter, story-slugger | DEC-012 |
| REQ-023 | visual-agent-exporter, visual-contract-validator | DEC-009 |
| REQ-024 | visual-agent-exporter, story-protagonist-snapshot, story-slugger | DEC-011, DEC-012 |
| REQ-025 | character-background-remover, comfyui-client, story-protagonist-snapshot | DEC-008 |
| REQ-026 | visual-contract-validator, story-export-service | DEC-006, DEC-008, DEC-009 |
| REQ-027 | visual-agent-exporter, story-outline-repository, story-page-repository | DEC-001, DEC-010 |
| REQ-028 | visual-agent-exporter, story-page-repository | - |
| REQ-029 | visual-agent-exporter, story-outline-repository | DEC-010 |
| REQ-030 | visual-agent-exporter, visual-contract-validator | DEC-009 |
| REQ-031 | visual-contract-validator, visual-agent-exporter, story-slugger | DEC-009, DEC-012 |
| REQ-032 | visual-contract-validator, story-export-service, story-export-actions | DEC-006, DEC-009 |
| REQ-033 | retirado en requirements@6; sin cobertura | - |
| REQ-034 | retirado en requirements@6; sin cobertura | - |
| REQ-035 | retirado en requirements@6; sin cobertura | - |
| REQ-036 | story-pages-panel, story-pages-api, story-page-service | - |
| REQ-037 | director-agent, director-model-provisioner, story-service, story-outline-repository, story-script-panel, story-script-api | DEC-001 |
| REQ-038 | story-service, story-page-service, writer-agent, writer-model-provisioner, generation-gate, story-page-notifier, story-page-version-repository | DEC-002, DEC-004 |
| REQ-039 | story-page-service, writer-agent, story-page-version-repository, story-page-notifier, story-pages-panel, story-pages-api | DEC-004 |
| REQ-040 | story-page-service, story-pages-panel | DEC-002 |
| REQ-041 | story-page-service, story-page-repository | DEC-003 |
| REQ-042 | story-service, story-page-service, story-page-version-repository, story-page-repository | DEC-001 |
| REQ-043 | story-page-service, story-service, story-page-repository | DEC-003 |

## Contratos afectados

| Protocolo | Contrato | Cambio |
|---|---|---|
| openapi | docs/contracts/openapi/schemas/story-beat.yaml | Se añade el lugar de escena obligatorio y no vacío al beat. |
| openapi | docs/contracts/openapi/schemas/story-outline.yaml, story-script.yaml | El guion incluye el lugar y el tono propios de la portada, que faltan en los cuentos anteriores. |
| openapi | docs/contracts/openapi/paths/story/beat-edit-service.yaml, requests/beat-edit-request.yaml | La edición directa de un beat incluye y valida el lugar de escena. |
| openapi | docs/contracts/openapi/paths/story/approve-service.yaml | Aprobar el guion lanza la generación de la portada; la respuesta indica si empezó o no y la aprobación no falla por ello. |
| openapi | docs/contracts/openapi/paths/story/cover-generate-service.yaml | Nuevo rechazo cuando el guion no tiene lugar y tono de portada. La operación también se lanza sola al aprobar el guion. |
| openapi | docs/contracts/openapi/paths/story/page-hint-service.yaml, page-generate-service.yaml, page-accept-service.yaml, page-accept-proposal-service.yaml | El hint se admite en la portada, pendiente o aceptada, con rechazo cuando falta su lugar y su tono. La página 1 se rechaza sin portada aceptada. Aceptar una versión nueva de la portada no aplica la cascada y retira solo su imagen. |
| openapi | Operación de subida de imagen de la portada o página (ruta a definir; hoy no figura en el mapa) | Se formaliza o se crea: solo para contenido aceptado, acepta cualquier imagen, rechaza lo que no es imagen conservando la anterior y sustituye la imagen previa. |
| openapi | Operación de exportación para Nubi Game (ruta a definir; la actual no figura en el mapa) | Sustituye a la exportación actual: devuelve el zip nuevo o la lista de faltas por portada o página. |
| openapi | Operación de exportación para el agente visual (ruta a definir) | Nueva: devuelve el zip visual o la lista de condiciones o reglas del contrato incumplidas. |
| file/json | docs/contracts/nubi-game/story-export.md (nuevo) | Nuevo contrato del zip para Nubi Game: `story.json` con título, ruta de la portada y páginas en orden con texto, ruta de la imagen y ruta del audio; rutas relativas a la raíz; imágenes en su formato original, sin modificar, con la extensión de su formato real y sin campo que declare el formato. |
| file/json | docs/contracts/agent-visual/input.md | La estructura no cambia. StoryTeller pasa a ser un productor implementado; se actualiza el estado y la trazabilidad del contrato para incluir esta feature. |

## Cambios sugeridos al mapa

- + Componente: `story-export-service | (ruta a definir en api/) | Orquesta las exportaciones para Nubi Game y el agente visual, acumulando todas las faltas | story-outline-repository, story-page-repository, narration-repository, nubi-game-exporter, visual-agent-exporter`
- + Componente: `nubi-game-exporter | (ruta actual del zip para Nubi Game, hoy sin mapear) | Zip para Nubi Game: story.json con rutas, imágenes originales con extensión real | image-content-inspector`
- + Componente: `visual-agent-exporter | (ruta a definir en api/) | Compone el story.json v0.1 y el zip para el agente visual | story-protagonist-snapshot, story-slugger, character-background-remover, visual-contract-validator`
- + Componente: `character-background-remover | (ruta a definir en api/) | Quita el fondo del retrato de la instantánea mediante ComfyUI | comfyui-client`
- + Componente: `visual-contract-validator | (ruta a definir en api/) | Valida el story.json y la imagen del personaje con las reglas del contrato v0.1 | -`
- + Componente: `story-slugger | (ruta a definir en api/) | Deriva slugs deterministas para storyId e id del protagonista | -`
- + Componente: `image-content-inspector | (ruta a definir en api/) | Decide por contenido si un fichero es imagen e identifica su formato | -`
- + Componente: `story-protagonist-snapshot | (ruta actual de StoryActorSnapshot, hoy sin mapear) | Lectura de la instantánea del protagonista: nombre, descripción visual y retrato | JPA`
- + Componente: `story-export-api | (ruta a definir en app/src/api/) | Cliente REST de las exportaciones para Nubi Game y el agente visual | httpClient`
- + Componente: `story-export-actions | (ruta actual de la acción de exportación, hoy sin mapear) | Pide las exportaciones, descarga el zip o muestra las faltas | story-export-api`
- ~ Componente: `director-agent | api/src/main/java/es/vargontoc/storyteller/infrastructure/adapters/out/external/SpringAiDirectorAgent.java | Guion con un beat por página (con lugar de escena) y lugar y tono de la portada | Spring AI, Ollama`
- ~ Componente: `story-outline-repository | api/src/main/java/es/vargontoc/storyteller/infrastructure/adapters/out/persistence/StoryOutlineRepositoryAdapter.java | Guion con beats con lugar de escena y lugar y tono de la portada | JPA`
- ~ Componente: `story-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryService.java | Guion ampliado; al aprobarlo lanza la portada; al rehacerlo la descarta | director-agent, story-outline-repository, story-review-repository, character-catalog-service, generation-gate, story-page-service`
- ~ Componente: `beat-editor | api/src/main/java/es/vargontoc/storyteller/application/service/BeatEditorService.java | Edición directa de un beat, incluido el lugar de escena | director-agent, story-outline-repository, story-page-service, generation-gate, story-narration-service`
- ~ Componente: `story-page-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageService.java | Portada desde su lugar y su tono, con hint y fuera de la cascada; páginas; subida de imagen validada | story-page-repository, story-outline-repository, story-page-version-repository, writer-agent, page-length-validator, generation-gate, story-page-notifier, story-narration-service, image-content-inspector`
- ~ Componente: `writer-agent | api/src/main/java/es/vargontoc/storyteller/infrastructure/adapters/out/external/SpringAiWriterAgent.java | Escena de portada desde su lugar y su tono, con hint; páginas | Spring AI, Ollama`
- ~ Componente: `story-script-panel | app/src/components/StoryScriptPanel.vue | Escaleta con el lugar de cada beat y el lugar y el tono de la portada | story-script-api, ConfirmModal`
- ~ Componente: `story-pages-panel | app/src/components/StoryPagesPanel.vue | Portada y páginas: escena completa, subida de imagen, hint de portada | story-pages-api, websocket-store, ConfirmModal`
- ~ Componente: `story-pages-api | app/src/api/storyPages.ts | Cliente de portada y páginas, incluida la subida de imagen | httpClient`
- ~ Contrato: `openapi | docs/contracts/openapi/paths/story/cover-generate-service.yaml, page-generate-service.yaml, page-hint-service.yaml, page-regenerate-service.yaml, page-version-select-service.yaml, page-accept-service.yaml, page-accept-proposal-service.yaml, page-discard-proposals-service.yaml, page-image-service.yaml, (operación de subida de imagen, hoy sin mapear) | storyteller-app -> storyteller-server`
- + Contrato: `openapi | (operaciones de exportación para Nubi Game y para el agente visual; la actual de Nubi Game hoy sin mapear) | storyteller-app -> storyteller-server`
- + Contrato: `file/json | docs/contracts/agent-visual/input.md | storyteller-server -> visual-production-agent`
- + Contrato: `file/json | docs/contracts/nubi-game/story-export.md | storyteller-server -> nubi-game`
- ~ Módulo: `storyteller-server | api/ | Lógica de negocio, orquestación de agentes, persistencia y API REST/WS | Java 21, Spring Boot 4.1.1, Spring AI 2.0.0, JPA, Liquibase` (el mapa indica 4.1.0 y `.sdd/project.md` 4.1.1; hay que confirmar cuál es la correcta)
