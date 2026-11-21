# Diseño: agent-writer

Estado: aprobado
Revisión: 2
Basado en: requirements@5

## Resumen

`story-page-service` se reorganiza para que, en los cuentos con escaleta, genere la portada (solo la descripción de escena) y cada página a petición explícita del usuario. Lo hace a través de un adaptador propio del escritor, cuyo Modelfile pasa a ser un recurso del backend aprovisionado como el del director. Cada portada o página pendiente guarda un histórico de versiones con una versión vigente. El backend valida la longitud del texto narrativo y regenera dentro de la misma generación hasta 3 veces. Una puerta de generación única para toda la aplicación (`generation-gate`) serializa las generaciones del escritor, el director, el agente de personaje y los retratos, y el resultado llega a la interfaz por WebSocket. La aplicación no genera imágenes: solo muestra la imagen importada de la versión aceptada cuando existe, y la deja de asociar al aceptar otra versión o al eliminar la página.

## Supuestos sobre el sistema actual

- La invocación del escritor vive hoy dentro de `story-page-service`: usa directamente el cliente de chat del modelo del escritor y las plantillas `new_cover.st`, `review_cover.st`, `new_page.st` y `review_page.st`. El componente `writer-agent` que cita el mapa no tiene línea propia ni ruta.
- Hoy la generación de páginas es síncrona y deduce la página a generar (portada si no hay páginas, y si no la siguiente). Los cambios de página usan `StoryPageReview`, con una única revisión pendiente que se descarta al pedir otra.
- La persistencia del cuento, de sus páginas y de las revisiones de página (`StoryRepository`, `StoryPageRepository`, `StoryPageReviewRepository`) existe, pero el mapa no la referencia. La instantánea del protagonista (`StoryActorSnapshot`) se lee a través del cuento.
- El marcador de versión de guion (`StoryScriptVersion`) de agent-director distingue los cuentos con escaleta de los antiguos (`story.hasOutline()`).
- El Modelfile del escritor reside hoy en `agents/` y lo carga `agents/load_model.ps1`. No se ha comprobado, porque el mapa no referencia esa carpeta.
- Los temas ya no se generan con un agente: son un catálogo estático en la app (PDEC-024, componente `story-topics`). La parte de REQ-032 que habla de temas no tiene hoy ningún componente al que aplicarse.
- El backend se ejecuta como una única instancia (despliegue local en Docker con una sola GPU).
- La importación de imágenes queda fuera de alcance, así que esta feature no sabe cómo llega una imagen importada ni dónde se almacena. Se asume que la imagen importada, cuando exista, quedará asociada a la portada o página aceptada y que la aplicación podrá leerla para mostrarla. El mapa no referencia hoy ningún almacén de imágenes de página.

## Componentes

| Componente | Responsabilidad | Existente o nuevo |
|---|---|---|
| story-page-service | Casos de uso de portada y páginas: generación de la página N, cambio con hint, reapertura, regeneración sin hint, elección de vigente, aceptación con descarte en cascada y descarte de propuestas, y lectura de la imagen importada de la versión aceptada; bifurca los cuentos antiguos al flujo anterior | existente (modificado) |
| writer-agent | Adaptador del escritor en Ollama: genera la escena de portada o una página (texto y escena) y procesa hints con aceptación o rechazo motivado | nuevo (se extrae de story-page-service) |
| writer-model-provisioner | Registra en Ollama al arrancar el modelo del escritor desde su Modelfile, mantenido como recurso del backend | nuevo |
| page-length-validator | Cuenta las frases y las palabras del texto narrativo y clasifica el resultado: sin aviso, exceso de palabras, número de frases o ambos | nuevo |
| story-page-version-repository | Persiste el histórico de versiones pendientes de cada portada o página, con su aviso y su marca de vigente, y el histórico de propuestas de una reapertura | nuevo |
| story-outline-repository | Proporciona el título, la sinopsis, el reparto episódico y el beat N del guion aprobado | existente |
| generation-gate | Puerta única de generación para toda la aplicación: concede o rechaza el turno, sabe qué generación está en curso y de qué cuento, y lo libera al terminar | nuevo |
| story-page-notifier | Publica los eventos de generación de portada y página (inicio, versión presentada, hint rechazado, fallo) | nuevo |
| websocket-adapter | Difunde por el canal /storyteller los eventos de story-page-notifier | existente |
| story-service | Generación y revisión global del guion; pasa a pedir turno a generation-gate | existente (modificado) |
| beat-editor | Validación de la edición directa de beat con el director; pasa a pedir turno a generation-gate, y la eliminación de páginas invalidadas incluye sus históricos | existente (modificado) |
| character-catalog-service | Alta automática o manual de personajes con el agente de personaje; pasa a pedir turno a generation-gate, también al solicitar retratos | existente (modificado) |
| character-review-service | Revisión de fichas con el agente de personaje; pasa a pedir turno a generation-gate | existente (modificado) |
| portrait-generation-service | Generación asíncrona de retratos en ComfyUI; ocupa generation-gate desde que crea el trabajo hasta que termina con éxito o error | existente (modificado) |
| story-modal | En los cuentos con escaleta, muestra story-pages-panel en la pestaña de páginas; en los antiguos mantiene la vista anterior | existente (modificado) |
| story-pages-panel | Vista de portada y páginas: contenido de la versión vigente, imagen importada solo en la versión aceptada, estado, histórico, avisos, acciones disponibles en cada estado y confirmación del descarte en cascada; ninguna edición manual | nuevo |
| story-pages-api | Cliente REST de las operaciones de portada y páginas de los cuentos con escaleta, incluida la lectura de la imagen importada de la versión aceptada | nuevo |
| websocket-store | Recibe los eventos de generación de portada y página además de los de retratos | existente (modificado) |

## Flujo principal

1. Con el guion aprobado, el usuario pide la portada en `story-pages-panel`, y `story-pages-api` envía la solicitud a `story-page-service`.
2. `story-page-service` comprueba que el cuento tiene escaleta, que el guion está aprobado y que no hay portada. Después pide turno a `generation-gate`, indicando que es una generación de escritor de ese cuento. Si se concede, responde que la generación ha empezado y `story-page-notifier` publica el inicio.
3. `story-page-service` lee de `story-outline-repository` el título y la sinopsis, toma la instantánea del protagonista del cuento y pide a `writer-agent` la escena de portada (DEC-010).
4. Con la escena recibida, `story-page-service` crea la portada pendiente, con el título del guion sin cambios y sin imagen. Guarda la escena como primera versión del histórico en `story-page-version-repository`, marcada como vigente y sin aviso. Libera `generation-gate` y `story-page-notifier` publica la versión presentada. `websocket-store` la recibe y `story-pages-panel` recarga la portada.
5. El usuario acepta la portada. `story-page-service` comprueba que no hay una generación de ese cuento en curso, acepta la versión vigente sin imagen y elimina el resto del histórico. No inicia ninguna generación.
6. El usuario pide la página N. `story-page-service` comprueba que la portada está aceptada (si N = 1) o que la página N-1 lo está, que la página N no existe y que N no supera la escaleta. Después pide turno a `generation-gate` y responde que la generación ha empezado.
7. `story-page-service` reúne el contexto: el beat N, el título, la sinopsis y el reparto episódico (de `story-outline-repository`), la instantánea del protagonista del cuento y el texto aceptado de las páginas 1 a N-1. Con ese contexto pide a `writer-agent` la página N.
8. `writer-agent` devuelve el texto narrativo y la descripción de escena. `story-page-service` pasa el texto a `page-length-validator`.
9. Si el texto cumple el límite, `story-page-service` crea la página N pendiente, vinculada al beat N y sin imagen. Guarda esa única versión, sin aviso, como vigente, libera `generation-gate` y notifica la versión presentada.
10. Para un cambio con hint, `story-page-service` rechaza un hint vacío y, con turno concedido, envía a `writer-agent` la versión vigente como base, junto con el hint y el mismo contexto. Si el escritor acepta el hint, la nueva versión pasa por la validación de longitud y se añade al histórico como vigente.
11. Si el hint es sobre una portada o página aceptada, la nueva versión se guarda como propuesta en el histórico de la reapertura. La versión aceptada no cambia y conserva su imagen importada si la tiene. Los hints siguientes usan como base la propuesta vigente.
12. El usuario puede marcar como vigente cualquier versión del histórico (en una reapertura, solo entre las propuestas). `story-page-service` lo registra si no hay una generación de ese cuento en curso.
13. Al aceptar la propuesta vigente de una página reabierta K con páginas posteriores, `story-page-service` indica cuántas páginas se eliminarán. Con la confirmación del usuario desde `story-pages-panel`, sustituye la versión aceptada de K por la propuesta, que queda sin imagen, y la imagen importada anterior de K deja de estar asociada. Elimina el histórico de K y elimina del cuento las páginas posteriores a K con sus históricos y su imagen importada. No regenera nada. En la portada reabierta no se elimina ninguna página y la nueva versión aceptada también queda sin imagen.
14. Al aceptar la última página de la escaleta, `story-page-service` marca el cuento como completo, con todas las páginas aceptadas y listo para la etapa de audio, y ya no ofrece generar más páginas.
15. Al abrir las páginas, `story-pages-panel` muestra en cada portada o página el contenido de su versión vigente. Solo en la versión aceptada, y si existe, muestra la imagen importada, que obtiene a través de `story-pages-api` y `story-page-service`. Las versiones pendientes y las propuestas de una reapertura se muestran siempre sin imagen (DEC-012).
16. Cualquier otra generación de la aplicación (guion o revisión del director, validación de beat, agente de personaje, retratos) pide turno a `generation-gate` antes de empezar y lo libera al terminar.

## Flujos alternativos y de fallo

### ALT-001 Página 1 pedida con la portada sin aceptar

- **Condición:** Se pide generar la página 1 y la portada no existe o está pendiente de aceptación.
- **Comportamiento:** Se rechaza la operación sin pedir turno ni llamar al escritor y se indica que primero hay que aceptar la portada.
- **Responsable:** story-page-service
- **Resultado:** No se inicia ninguna generación y el cuento no cambia.
- **Cubre:** REQ-002

### ALT-002 Página N pedida con la página N-1 sin aceptar

- **Condición:** Se pide generar la página N y la página N-1 no existe o está pendiente, también cuando su versión vigente tiene aviso de número de frases.
- **Comportamiento:** Se rechaza la operación e indica que primero hay que aceptar la página N-1.
- **Responsable:** story-page-service
- **Resultado:** No se inicia ninguna generación y el cuento no cambia.
- **Cubre:** REQ-010, REQ-028

### ALT-003 Generación en curso en la aplicación

- **Condición:** Llega una solicitud de generación de cualquier tipo (portada o página, guion o revisión del director, validación de beat, agente de personaje, retratos) mientras `generation-gate` tiene otra en curso, también si esa otra está en una regeneración automática por longitud.
- **Comportamiento:** `generation-gate` niega el turno y el servicio que lo pidió rechaza la solicitud e indica que hay una generación en curso. El turno solo se libera cuando la generación en curso termina, con éxito o con fallo.
- **Responsable:** generation-gate
- **Resultado:** La solicitud no tiene efecto. La siguiente que llegue con la puerta libre se inicia.
- **Cubre:** REQ-032

### ALT-004 Aceptar, descartar o elegir versión durante una generación del cuento

- **Condición:** Se pide aceptar, descartar propuestas o marcar una versión como vigente en un cuento cuya portada o alguna de cuyas páginas se está generando.
- **Comportamiento:** `story-page-service` pregunta a `generation-gate` si la generación en curso es de ese cuento. Si lo es, rechaza la operación e indica que hay una generación en curso.
- **Responsable:** story-page-service
- **Resultado:** El histórico, la versión vigente y el estado no cambian. Cuando la generación termina, la operación vuelve a estar disponible.
- **Cubre:** REQ-033

### ALT-005 Hint vacío

- **Condición:** El usuario pide un cambio con un hint vacío o formado solo por espacios.
- **Comportamiento:** Se rechaza la solicitud e indica que el hint es obligatorio, sin pedir turno ni llamar al escritor.
- **Responsable:** story-page-service
- **Resultado:** La portada o la página no cambia.
- **Cubre:** REQ-013

### ALT-006 Hint rechazado por el escritor

- **Condición:** `writer-agent` responde que el hint se sale del beat o incumple las reglas de edad, y da un motivo.
- **Comportamiento:** No se hace ninguna regeneración por longitud, no se añade ninguna versión y se libera `generation-gate`. `story-page-notifier` publica el rechazo con su motivo.
- **Responsable:** story-page-service
- **Resultado:** El usuario ve el motivo. El histórico y la versión vigente siguen como estaban.
- **Cubre:** REQ-014, REQ-035

### ALT-007 Texto fuera del límite de longitud

- **Condición:** `page-length-validator` indica que el texto generado tiene menos de 1 frase o más de 4, o más de 66 palabras. Se aplica a la generación inicial, al cambio con hint, a la reapertura y a la regeneración sin hint de una página; no se aplica a la portada (DEC-010).
- **Comportamiento:** Sin intervención del usuario y sin liberar `generation-gate`, se pide otra generación a `writer-agent` con la misma solicitud, hasta 3 en total contando la primera. Mientras tanto no se presenta ni se guarda ninguna versión. La primera que cumple el límite termina la solicitud y es la única que entra en el histórico.
- **Responsable:** story-page-service
- **Resultado:** La versión que cumple el límite se presenta como vigente, sin aviso.
- **Cubre:** REQ-007, REQ-025, REQ-034

### ALT-008 Tres generaciones fuera del límite

- **Condición:** Ninguna de las 3 generaciones de una solicitud cumple el límite de longitud.
- **Comportamiento:** Las 3 versiones entran en el histórico (o en el de propuestas, si es una reapertura), cada una con su aviso según `page-length-validator`: exceso de palabras, número de frases o ambos. La tercera queda vigente y se libera `generation-gate`. Se notifica como versión presentada, no como fallo.
- **Responsable:** story-page-service
- **Resultado:** La página queda pendiente con las 3 versiones y sus avisos. Las acciones disponibles dependen del aviso de la vigente.
- **Cubre:** REQ-020, REQ-026, REQ-034

### ALT-009 Aceptación bloqueada por número de frases

- **Condición:** Se pide aceptar una página cuya versión vigente tiene aviso de número de frases, solo o junto con el de exceso de palabras.
- **Comportamiento:** Se rechaza la aceptación e indica que hay que elegir otra versión válida, pedir un cambio con hint o regenerar sin hint. Si la vigente solo tiene aviso de exceso de palabras, se acepta tal cual.
- **Responsable:** story-page-service
- **Resultado:** La página sigue pendiente con el mismo histórico.
- **Cubre:** REQ-027, REQ-028

### ALT-010 Regenerar sin hint cuando no procede

- **Condición:** Se pide regenerar sin hint una página cuya versión vigente no tiene aviso de número de frases.
- **Comportamiento:** Se rechaza la operación sin llamar al escritor. `story-pages-panel` no ofrece esa acción.
- **Responsable:** story-page-service
- **Resultado:** La página no cambia.
- **Cubre:** REQ-029

### ALT-011 Fallo de generación

- **Condición:** El modelo del escritor no está disponible, `writer-agent` devuelve un error o devuelve una respuesta sin texto narrativo o sin descripción de escena. Puede ocurrir en cualquiera de las generaciones de la solicitud, también en una regeneración automática.
- **Comportamiento:** Toda la solicitud se trata como fallida (DEC-009): no se guarda ninguna versión, tampoco las ya obtenidas fuera del límite en esa misma solicitud. Se libera `generation-gate` y `story-page-notifier` publica el fallo.
- **Responsable:** story-page-service
- **Resultado:** El usuario ve un mensaje de error. Si era la generación inicial, la portada o la página no existe. Si no, conserva su histórico, su versión vigente y, si está aceptada, su imagen importada. El usuario puede volver a pedir la misma operación.
- **Cubre:** REQ-005, REQ-020, REQ-035

### ALT-012 Aceptar una página reabierta con páginas posteriores

- **Condición:** El usuario pide aceptar la propuesta vigente de una página reabierta K y existen páginas posteriores a K.
- **Comportamiento:** `story-page-service` indica cuántas páginas se eliminarán y no aplica la aceptación sin una confirmación explícita, que `story-pages-panel` pide al usuario. Si el usuario cancela, no se envía nada. Si confirma, se aplica el flujo principal (paso 13).
- **Responsable:** story-page-service
- **Resultado:** Si cancela, la propuesta sigue pendiente y las páginas posteriores permanecen con su imagen importada. Si confirma, K queda aceptada con la propuesta y sin imagen, las páginas posteriores desaparecen junto con su imagen importada y la siguiente página que se puede generar es K+1, sin generación automática y sin imagen al generarse.
- **Cubre:** REQ-016, REQ-017, REQ-018, REQ-041

### ALT-013 Descartar las propuestas de una reapertura

- **Condición:** El usuario descarta las propuestas de una portada o página reabierta, aunque la propuesta vigente tenga aviso.
- **Comportamiento:** Se elimina todo el histórico de propuestas de esa reapertura. La versión aceptada y su imagen importada no se tocan.
- **Responsable:** story-page-service
- **Resultado:** La portada o la página muestra su versión aceptada anterior, con su imagen si la tenía, y las páginas posteriores siguen aceptadas.
- **Cubre:** REQ-030, REQ-031

### ALT-014 Cuento completo

- **Condición:** Se pide generar una página cuando todas las páginas de la escaleta están aceptadas, o una página con número mayor que el de la escaleta.
- **Comportamiento:** Se rechaza la operación.
- **Responsable:** story-page-service
- **Resultado:** El cuento sigue marcado como completo y listo para la etapa de audio, y no se ofrece generar más páginas.
- **Cubre:** REQ-021

## Interfaces entre componentes

| Origen | Destino | Qué se comunica | Protocolo |
|---|---|---|---|
| story-pages-api | story-page-service | Pedir portada o página N, pedir cambio con hint (también sobre aceptadas), regenerar sin hint, marcar vigente, aceptar (con confirmación del descarte en cascada), descartar propuestas, consultar portada, páginas, históricos y avisos, y obtener la imagen importada de la versión aceptada | openapi |
| story-page-service | generation-gate | Pedir turno indicando el tipo y el cuento, liberarlo al terminar, y consultar si la generación en curso es de un cuento concreto | - |
| story-service | generation-gate | Pedir y liberar turno al generar o revisar el guion | - |
| beat-editor | generation-gate | Pedir y liberar turno al validar con el director una edición de beat | - |
| character-catalog-service | generation-gate | Pedir y liberar turno al crear un personaje con el agente; pedir turno al solicitar retratos | - |
| character-review-service | generation-gate | Pedir y liberar turno al solicitar una revisión al agente de personaje | - |
| portrait-generation-service | generation-gate | Mantener el turno durante el trabajo de retratos y liberarlo cuando termina con éxito o error | - |
| story-page-service | story-outline-repository | Lectura del título, la sinopsis, el reparto episódico y el beat N del guion aprobado | - |
| story-page-service | story-page-version-repository | Alta de versiones con aviso, marca de vigente, lectura del histórico, borrado del histórico al aceptar o descartar, y borrado de los históricos de páginas eliminadas | - |
| story-page-service | page-length-validator | Texto narrativo generado; devuelve si cumple y qué aviso corresponde | - |
| story-page-service | writer-agent | Petición de escena de portada, de página N con su contexto, o de nueva versión con base vigente y hint; devuelve el contenido o el rechazo motivado | ollama |
| writer-model-provisioner | Ollama | Registro del modelo del escritor al arrancar si no existe | http |
| story-page-service | story-page-notifier | Inicio, versión presentada, hint rechazado con motivo y fallo de una generación de portada o página | - |
| story-page-notifier | websocket-adapter | Eventos de generación de portada y página del cuento | websocket |
| websocket-adapter | websocket-store | Difusión de los eventos de generación de portada y página | websocket |
| beat-editor | story-page-service | Orden de eliminar las páginas invalidadas, con sus históricos | - |
| story-modal | story-pages-panel | Apertura de la pestaña de páginas en los cuentos con escaleta | - |
| story-pages-panel | story-pages-api | Acciones del usuario sobre portada y páginas y lectura de la imagen importada de la versión aceptada | - |
| websocket-store | story-pages-panel | Aviso de que hay que recargar una portada o una página | - |

## Modelo de datos lógico

- `Story` (existente): cuento. Relaciones: tiene una `StoryOutline` y varias `StoryPage`. Su estado gana la situación "todas las páginas aceptadas y listo para audio" (REQ-021). El marcador de versión de guion decide entre el flujo nuevo y el antiguo.
- `StoryOutline` y `StoryBeat` (existentes): proporcionan el título, la sinopsis, el reparto episódico y el beat N. Sin cambios.
- `StoryActorSnapshot` (existente): instantánea del protagonista guardada en el cuento. Es la que recibe el escritor (REQ-004).
- `StoryPage` (existente, modificada): portada (página 0) o página N de un cuento con escaleta. Tiene un estado (pendiente de aceptación o aceptada) y, si está aceptada, el contenido aceptado (texto narrativo y escena, o título y escena en la portada). Relaciones: pertenece a un `Story`; la página N se vincula con el `StoryBeat` N; tiene cero o varias `PageVersion`; su contenido aceptado puede tener como mucho una `ImportedImage`.
- `PageVersion` (nueva): versión generada de una portada o página que aún no se ha aceptado. Contiene su texto narrativo y su escena, su aviso de longitud (ninguno, exceso de palabras, número de frases o ambos; en la portada, siempre ninguno) y la marca de vigente; en cada `StoryPage` hay como mucho una vigente. Nunca tiene imagen. Si la `StoryPage` está pendiente, sus versiones forman el histórico. Si está aceptada, forman el histórico de propuestas de la reapertura y el contenido aceptado queda fuera. Relaciones: pertenece a una `StoryPage`. Se eliminan todas al aceptar (la vigente pasa a ser el contenido aceptado), al descartar las propuestas o al eliminar la página. No hay límite de número.
- `ImportedImage` (existente o futura, fuera del alcance de esta feature): imagen de portada o de página creada fuera de la aplicación e importada. Esta feature solo la lee para mostrarla y deja de asociarla cuando cambia el contenido aceptado o se elimina la página. Relaciones: pertenece al contenido aceptado de una `StoryPage`.
- `StoryPageReview` (existente): se conserva solo para los cuentos antiguos sin escaleta.
- `GenerationTurn` (nuevo, no persistido): generación en curso en la aplicación, con su tipo y, si es del escritor, su cuento. Como mucho hay uno.

## Decisiones

### DEC-001 Histórico de versiones como entidad propia

- **Decisión:** Las versiones pendientes de portada y páginas de los cuentos con escaleta se guardan como `PageVersion` asociadas a `StoryPage`, con aviso y marca de vigente. El mismo concepto sirve para el histórico de una página pendiente y para las propuestas de una reapertura, según el estado de la página. `StoryPageReview` queda para los cuentos antiguos.
- **Alternativas descartadas:** Reutilizar `StoryPageReview` con varias revisiones pendientes. Su ciclo de vida actual descarta la revisión pendiente anterior al pedir otra, y modela una propuesta frente a una base, no una elección libre entre N versiones. Cambiarlo alteraría los cuentos antiguos (REQ-023). Guardar el histórico dentro de la propia página como lista embebida también se descarta, porque complica marcar una vigente y borrar el resto.
- **Cubre:** REQ-012, REQ-015, REQ-023, REQ-034, REQ-036, REQ-037, REQ-038, REQ-039

### DEC-002 Puerta de generación única, en memoria del servidor

- **Decisión:** Un componente `generation-gate` concede un único turno de generación a toda la aplicación y conoce el tipo de generación y, si procede, su cuento. Lo consultan todos los servicios que lanzan generaciones: escritor, director (guion, revisión global y validación de beat, según DEC-008), agente de personaje y retratos. El turno se libera siempre al terminar, con éxito o con fallo. Vive en memoria del servidor: al arrancar está libre, y los retratos que quedaron a medias ya los marca como fallidos `portrait-reconciliation-service`. El bloqueo por personaje de `portrait-lock-service` se mantiene, porque protege otras operaciones (renombrado, revisión).
- **Alternativas descartadas:** Persistir el turno en base de datos, como `PortraitGenerationJob`. Exige reconciliar al arrancar cada tipo de generación y no aporta nada con una sola instancia. Mantener un bloqueo por tipo, como el actual de retratos, no impide que se ejecuten a la vez generaciones de tipos distintos, que es lo que pide REQ-032. Encolar las solicitudes en lugar de rechazarlas contradice REQ-032.
- **Cubre:** REQ-032, REQ-033

### DEC-003 Generación de portada y páginas asíncrona con notificación por WebSocket

- **Decisión:** La solicitud de generación (inicial, con hint, de reapertura o sin hint) responde en cuanto el turno queda concedido. El resultado (versión presentada, hint rechazado o fallo) llega por el canal WebSocket existente, siguiendo el patrón de los retratos. La interfaz recarga entonces el estado desde la API.
- **Alternativas descartadas:** Mantener la llamada síncrona actual. Con hasta 3 generaciones del modelo local por solicitud, la petición puede durar mucho y quedar cortada, y no encaja con SUP-006.
- **Cubre:** REQ-020, REQ-022, REQ-025

### DEC-004 Validación de longitud en el backend dentro de la misma generación

- **Decisión:** `page-length-validator` cuenta de forma determinista las frases y el total de palabras del texto narrativo. `story-page-service` repite la petición al escritor dentro del mismo turno hasta cumplir el límite o llegar a 3 generaciones. Las instrucciones del Modelfile del escritor recogen además el límite, la prohibición de subordinadas encadenadas y el vocabulario para 3 a 4 años, que no se validan automáticamente.
- **Alternativas descartadas:** Pedir al modelo que se autoevalúe, porque no es fiable para contar. Validar en la interfaz no permite regenerar sin intervención del usuario y deja la regla fuera del servidor. Liberar el turno entre regeneraciones contradice REQ-032.
- **Cubre:** REQ-007, REQ-008, REQ-009, REQ-025, REQ-026

### DEC-005 Modelfile del escritor como recurso del backend con su propio aprovisionador

- **Decisión:** El Modelfile del escritor pasa a ser un recurso del backend, junto al del director. Un `writer-model-provisioner` análogo a `director-model-provisioner` registra el modelo en Ollama al arrancar si no existe. El Modelfile se retira de `agents/` y el script de carga deja de referenciarlo.
- **Alternativas descartadas:** Generalizar `director-model-provisioner` en un aprovisionador común. Es razonable, pero toca un componente estable de otra feature, y REQ-024 pide el mismo patrón, no unificar. Mantenerlo en `agents/` incumple REQ-024.
- **Cubre:** REQ-024

### DEC-006 Adaptador propio del escritor

- **Decisión:** Se extrae la invocación del escritor de `story-page-service` a un adaptador `writer-agent`, como `director-agent`. Sus operaciones son: escena de portada, página N con contexto, y nueva versión con base, hint y aceptación o rechazo motivado. La regeneración sin hint usa la operación de página N.
- **Alternativas descartadas:** Seguir llamando al cliente de chat desde el servicio de aplicación. Mezcla prompts con reglas de negocio (histórico, longitud, turno) y dificulta probar el bucle de regeneración.
- **Cubre:** REQ-001, REQ-003, REQ-004, REQ-005, REQ-006, REQ-012, REQ-014, REQ-029

### DEC-007 Página pedida por número y flujo bifurcado por versión de guion

- **Decisión:** En los cuentos con escaleta, el usuario pide explícitamente la portada o la página N. El servicio valida que N es la siguiente permitida en lugar de deducirla. Los cuentos sin escaleta siguen por el flujo actual (deducción de la página siguiente y `StoryPageReview`). La única diferencia para ellos es que también pasan por `generation-gate`.
- **Alternativas descartadas:** Deducir siempre la página siguiente. No permite dar el rechazo concreto de REQ-002 y REQ-010 ni distinguir la generación de la regeneración. Migrar los cuentos antiguos al nuevo modelo contradice REQ-023.
- **Cubre:** REQ-002, REQ-003, REQ-010, REQ-023, REQ-032

### DEC-008 La validación de una edición directa de beat cuenta como generación (confirmada por el usuario)

- **Decisión:** La validación con el director de una edición directa de beat en `beat-editor` es una generación del director a efectos de REQ-032: pide turno a `generation-gate`, se rechaza si hay otra generación en curso y bloquea las demás mientras dura.
- **Alternativas descartadas:** Dejarla fuera de la puerta por ser una validación corta. Usa el mismo modelo en la única GPU, que es el motivo de SUP-005, y podría coincidir con otra generación.
- **Cubre:** REQ-032

### DEC-009 Un error en cualquier generación hace fallar la solicitud entera (confirmada por el usuario)

- **Decisión:** Si falla cualquiera de las hasta 3 generaciones de una solicitud, también una regeneración automática por longitud, la solicitud entera falla según REQ-020: no se guarda ninguna versión, tampoco las ya obtenidas fuera del límite, y la portada o la página conserva su estado anterior.
- **Alternativas descartadas:** Guardar con aviso las versiones ya obtenidas fuera del límite y tratar la solicitud como terminada. Mezcla un resultado parcial con un fallo, y REQ-026 solo prevé añadir versiones con aviso tras completar las 3 generaciones.
- **Cubre:** REQ-020, REQ-025, REQ-026, REQ-035

### DEC-010 Portada sin validación de longitud y con contexto propio (confirmada por el usuario)

- **Decisión:** La portada no tiene texto narrativo, así que nunca se le aplica la validación de longitud (REQ-007, REQ-025 a REQ-028): sus versiones nunca llevan aviso y su aceptación nunca se bloquea por número de frases. Para generarla, el escritor recibe el título, la sinopsis y la instantánea del protagonista del guion aprobado.
- **Alternativas descartadas:** Validar la longitud de la descripción de escena de la portada, que REQ-007 excluye expresamente. Entregar a la portada el mismo contexto que a una página (beats y reparto episódico), que no aporta nada a una escena de portada y alarga la petición.
- **Cubre:** REQ-001, REQ-007, REQ-011, REQ-028

### DEC-011 Regla de conteo de frases delegada en la capa técnica (confirmada por el usuario)

- **Decisión:** El diseño fija que una frase es un fragmento del texto narrativo terminado en un signo de cierre de oración. La regla exacta para diálogos, puntos suspensivos y signos dobles la fija y la prueba la capa técnica dentro de `page-length-validator`, con los casos de REQ-007 como referencia mínima.
- **Alternativas descartadas:** Fijar en el diseño una regla completa de segmentación, que es un detalle de implementación y exigiría decidir casos que los requisitos no definen.
- **Cubre:** REQ-007, REQ-026, REQ-028

### DEC-012 Imagen importada asociada solo al contenido aceptado

- **Decisión:** La imagen importada, cuando existe, pertenece al contenido aceptado de la `StoryPage`, nunca a una `PageVersion`. Por eso las versiones pendientes y las propuestas de una reapertura se muestran siempre sin imagen. Al aceptar una nueva versión, el contenido aceptado se sustituye y queda sin imagen, y la imagen anterior deja de estar asociada. Al eliminar una página por el descarte en cascada, su imagen desaparece del cuento con ella. Descartar propuestas no toca el contenido aceptado ni su imagen. La gestión física del fichero de imagen la define la importación, fuera de alcance.
- **Alternativas descartadas:** Asociar la imagen a cada versión y mostrar solo la de la aceptada. Exige copiar o mover la imagen entre versiones y contradice que las versiones nunca tengan imagen (REQ-005, REQ-040). Conservar la imagen anterior al aceptar una nueva versión contradice REQ-041.
- **Cubre:** REQ-015, REQ-016, REQ-022, REQ-030, REQ-031, REQ-037, REQ-038, REQ-040, REQ-041

## Riesgos

- **El modelo no se ajusta al límite de longitud o reescribe el beat.** Mitigación: instrucciones explícitas en el Modelfile del escritor (DEC-004), regeneración automática y avisos visibles. La fidelidad al beat (REQ-006) y la sintaxis (REQ-008) dependen del modelo y de la revisión del adulto.
- **El recuento de frases es ambiguo con diálogos o puntos suspensivos.** Un error en el recuento puede bloquear una aceptación válida. Mitigación: la capa técnica fija y prueba la regla de segmentación con casos de diálogo (DEC-011).
- **El servidor se reinicia durante una generación.** No se guarda ninguna versión y la puerta queda libre, pero la interfaz no recibe el evento de fin. Mitigación: `story-pages-panel` recarga el estado al reconectar `websocket-store`, para que el usuario pueda volver a pedir la operación.
- **Se olvida un punto de entrada de generación.** Una llamada a un modelo que no pide turno rompe REQ-032. Mitigación: la tabla de componentes enumera todos los servicios afectados, y se prueba cada uno con la puerta ocupada.
- **Una generación larga bloquea toda la aplicación.** Con 3 intentos por solicitud o con un lote de retratos, el resto de acciones de generación quedan rechazadas durante minutos. Es lo que deciden REQ-032 y SUP-005. El rechazo indica claramente que hay una generación en curso.
- **No hay forma de que exista una imagen importada mientras la importación esté fuera de alcance.** Los criterios con imagen de REQ-015, REQ-016, REQ-022, REQ-030, REQ-031, REQ-038, REQ-040 y REQ-041 solo se pueden probar con datos preparados. Mitigación: la capa técnica prepara datos de prueba con imagen asociada y la futura importación debe respetar DEC-012.

## Trazabilidad

| Requisito | Componentes | Decisiones |
|---|---|---|
| REQ-001 | story-page-service, writer-agent, story-outline-repository, story-page-version-repository | DEC-006, DEC-010 |
| REQ-002 | story-page-service, story-pages-panel | DEC-007 |
| REQ-003 | story-page-service, writer-agent, story-outline-repository | DEC-006, DEC-007 |
| REQ-004 | story-page-service, story-outline-repository, writer-agent | DEC-006 |
| REQ-005 | writer-agent, story-page-service | DEC-006 |
| REQ-006 | writer-agent, writer-model-provisioner | DEC-006 |
| REQ-007 | page-length-validator, story-page-service | DEC-004, DEC-010, DEC-011 |
| REQ-008 | writer-agent, writer-model-provisioner | DEC-004 |
| REQ-009 | writer-agent, writer-model-provisioner | DEC-004 |
| REQ-010 | story-page-service, story-pages-panel | DEC-007 |
| REQ-011 | story-page-service, story-page-version-repository, story-pages-panel | DEC-001, DEC-010 |
| REQ-012 | story-page-service, writer-agent, story-page-version-repository | DEC-001, DEC-006 |
| REQ-013 | story-page-service, story-pages-panel | - |
| REQ-014 | writer-agent, story-page-service, story-page-notifier | DEC-006 |
| REQ-015 | story-page-service, writer-agent, story-page-version-repository, story-pages-panel | DEC-001, DEC-012 |
| REQ-016 | story-page-service, story-page-version-repository | DEC-012 |
| REQ-017 | story-page-service, story-pages-panel | - |
| REQ-018 | story-page-service | - |
| REQ-019 | story-pages-panel, story-pages-api, story-page-service | - |
| REQ-020 | story-page-service, story-page-notifier, websocket-adapter, websocket-store, story-pages-panel | DEC-003, DEC-009 |
| REQ-021 | story-page-service, story-pages-panel | - |
| REQ-022 | story-pages-panel, story-pages-api, story-page-service, story-modal, websocket-store, story-page-notifier, websocket-adapter | DEC-003, DEC-012 |
| REQ-023 | story-page-service, story-modal | DEC-001, DEC-007 |
| REQ-024 | writer-model-provisioner, writer-agent | DEC-005 |
| REQ-025 | story-page-service, page-length-validator, writer-agent, generation-gate | DEC-003, DEC-004, DEC-009 |
| REQ-026 | story-page-service, page-length-validator, story-page-version-repository | DEC-004, DEC-009, DEC-011 |
| REQ-027 | story-page-service, story-pages-panel | - |
| REQ-028 | story-page-service, story-pages-panel | DEC-010, DEC-011 |
| REQ-029 | story-page-service, writer-agent, story-pages-panel | DEC-006 |
| REQ-030 | story-page-service, story-page-version-repository, story-pages-panel | DEC-001, DEC-012 |
| REQ-031 | story-page-service, writer-agent, story-page-version-repository, story-pages-panel | DEC-001, DEC-012 |
| REQ-032 | generation-gate, story-page-service, story-service, beat-editor, character-catalog-service, character-review-service, portrait-generation-service | DEC-002, DEC-007, DEC-008 |
| REQ-033 | story-page-service, generation-gate | DEC-002 |
| REQ-034 | story-page-service, story-page-version-repository | DEC-001 |
| REQ-035 | story-page-service, story-page-version-repository | DEC-001, DEC-009 |
| REQ-036 | story-page-service, story-page-version-repository, story-pages-panel | DEC-001 |
| REQ-037 | story-page-service, story-page-version-repository | DEC-001, DEC-012 |
| REQ-038 | story-page-service, story-page-version-repository, story-pages-panel | DEC-001, DEC-012 |
| REQ-039 | story-page-version-repository | DEC-001 |
| REQ-040 | story-pages-panel, story-pages-api, story-page-service | DEC-012 |
| REQ-041 | story-page-service, story-pages-panel | DEC-012 |

## Contratos afectados

| Protocolo | Contrato | Cambio |
|---|---|---|
| openapi | docs/contracts/openapi/schemas/story-page.yaml | Estado de la página (pendiente o aceptada), beat asociado, contenido aceptado con la indicación de si tiene imagen importada, histórico de versiones o de propuestas con aviso de longitud y vigente, y acciones disponibles; la portada sin texto narrativo y sin aviso |
| openapi | docs/contracts/openapi/schemas (nuevo esquema de versión de página) | Versión de portada o página con su texto, su escena, su aviso de longitud y la marca de vigente, sin imagen |
| openapi | docs/contracts/openapi/schemas/enums/story-status.yaml | Nuevo estado: todas las páginas aceptadas y listo para la etapa de audio |
| openapi | docs/contracts/openapi/paths/story (nuevos paths de portada y páginas) | Pedir portada o página N (respuesta de generación iniciada), cambio con hint, regeneración sin hint, marcar vigente, aceptar con confirmación del descarte en cascada y número de páginas a eliminar, descartar propuestas y obtener la imagen importada de la versión aceptada; respuestas de rechazo por portada o página anterior sin aceptar, aviso de número de frases, hint vacío, generación en curso y cuento completo |
| openapi | docs/contracts/openapi/openapi.yaml | Registrar los nuevos paths y esquemas de portada y páginas |
| openapi | docs/contracts/openapi/paths/story/generation-service.yaml, review-service.yaml, beat-edit-service.yaml | Nueva respuesta de rechazo por generación en curso en la aplicación |
| openapi | docs/contracts/openapi/paths/character/catalog-characters-service.yaml, character-review-request-service.yaml, character-portrait-generate-service.yaml | Nueva respuesta de rechazo por generación en curso en la aplicación |
| websocket | docs/contracts/websocket (nuevo contrato de eventos de generación de portada y página) | Eventos de inicio, versión presentada, hint rechazado con motivo y fallo, con el cuento y la página a los que afectan; storyteller-server -> storyteller-app por /storyteller |

## Cambios sugeridos al mapa

- ~ Componente: `story-page-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageService.java | Portada y páginas por número con histórico, longitud, reapertura, cascada e imagen aceptada | story-outline-repository, writer-agent, page-length-validator, story-page-version-repository, generation-gate, story-page-notifier`
- + Componente: `writer-agent | api/src/main/java/es/vargontoc/storyteller/infrastructure/adapters/out/external/ | Adaptador Ollama del escritor: portada, página desde beat y hint motivado | Spring AI, Ollama`
- + Componente: `writer-model-provisioner | api/src/main/java/es/vargontoc/storyteller/infrastructure/config/ | Registra en Ollama el modelo del escritor desde su Modelfile del backend | Ollama, writer-agent`
- + Componente: `page-length-validator | api/src/main/java/es/vargontoc/storyteller/ | Cuenta frases y palabras del texto narrativo y clasifica avisos | -`
- + Componente: `story-page-version-repository | api/src/main/java/es/vargontoc/storyteller/infrastructure/adapters/out/persistence/ | Persiste versiones pendientes y propuestas de portada y páginas | JPA`
- + Componente: `generation-gate | api/src/main/java/es/vargontoc/storyteller/application/service/ | Turno único de generación para toda la aplicación, en memoria | -`
- + Componente: `story-page-notifier | api/src/main/java/es/vargontoc/storyteller/application/service/ | Publica eventos de generación de portada y página por WebSocket | websocket-adapter`
- ~ Componente: `story-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryService.java | Casos de uso del guion; pide turno a generation-gate al generar o revisar | director-agent, story-outline-repository, story-review-repository, character-catalog-service, generation-gate`
- ~ Componente: `beat-editor | api/src/main/java/es/vargontoc/storyteller/application/service/BeatEditorService.java | Edición directa de beat con turno de generación e invalidación de páginas | director-agent, story-outline-repository, story-page-service, generation-gate`
- ~ Componente: `character-catalog-service | api/src/main/java/es/vargontoc/storyteller/application/service/CharacterCatalogService.java | Casos de uso del catálogo; altas con agente y retratos piden turno | character-agent, character-repository, character-validation, character-image-condition-port, portrait-lock-service, portrait-generation-service, portrait-selection-service, portrait-candidate-store, generation-gate`
- ~ Componente: `character-review-service | api/src/main/java/es/vargontoc/storyteller/application/service/CharacterReviewService.java | Revisiones del catálogo con agente; pide turno a generation-gate | character-agent, character-repository, character-review-repository, character-image-condition-port, portrait-lock-service, portrait-fork-service, portrait-invalidation-service, catalog-character-usage-adapter, generation-gate`
- ~ Componente: `portrait-generation-service | api/src/main/java/es/vargontoc/storyteller/application/service/PortraitGenerationService.java | Generación asíncrona de retratos ocupando generation-gate hasta terminar | portrait-generation-state-store, portrait-candidate-store, portrait-comfyui-adapter, portrait-websocket-notifier, character-repository, generation-gate`
- ~ Componente: `websocket-adapter | api/src/main/java/es/vargontoc/storyteller/infrastructure/adapters/in/websocket/WebsocketAdapter.java | Difusor /storyteller: notificaciones, retratos y generación de páginas | Spring WebSocket`
- + Componente: `story-page-repository | <ruta de StoryPageRepository> | Persistencia de portada y páginas del cuento | JPA`
- + Componente: `story-pages-panel | app/src/components/ | Vista de portada y páginas con histórico, avisos, imagen aceptada y acciones | story-pages-api, websocket-store, ConfirmModal`
- + Componente: `story-pages-api | app/src/api/ | Cliente REST de portada y páginas de cuentos con escaleta | httpClient`
- ~ Componente: `story-modal | app/src/components/StoryModal.vue | Modal del cuento: guion con escaleta, páginas nuevas o vista antigua | story-script-panel, story-script-api, story-pages-panel`
- ~ Componente: `websocket-store | app/src/stores/websocket.ts | Store /storyteller con eventos de retratos y de generación de páginas | Pinia`
- + Contrato: `openapi | docs/contracts/openapi/paths/story/<paths de portada y páginas> | storyteller-app -> storyteller-server`
- + Contrato: `websocket | docs/contracts/websocket/<eventos de generación de portada y página> | storyteller-server -> storyteller-app`
- + Convención: `generación: toda llamada a un modelo local (Ollama o ComfyUI) pide turno a generation-gate`
- + Convención: `imágenes de página: la aplicación no genera imágenes; solo muestra la importada de la versión aceptada`
