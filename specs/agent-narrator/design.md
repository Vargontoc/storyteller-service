# Diseño: agent-narrator

Estado: aprobado
Revisión: 1
Basado en: requirements@1

## Resumen

Se añade al servidor una biblioteca de voces con la voz del sistema tomada de la configuración y las voces subidas, y una orquestación de narración asíncrona que reserva el turno de `generation-gate`. Esa orquestación pide al narrador (Ollama) un guion de audio del cuento entero, comprueba que no altera las palabras y sintetiza con Chatterbox Multilingual V3 un audio por página interior, que solo se publica si salen bien todas. Las reglas de onomatopeyas se incorporan a las instrucciones del escritor. Las operaciones que reabren o eliminan páginas interiores borran el audio del cuento.

## Supuestos sobre el sistema actual

- El mapa no contiene ningún componente de narración ni de síntesis de voz, aunque el producto indica que FUN-004 (audio por página en cuentos antiguos) ya está implementado y que existe un Modelfile `narrator`. Se asume que, si ese código existe, sigue atendiendo a los cuentos antiguos sin cambios (fuera de alcance) y que los componentes nuevos de este diseño solo actúan en cuentos con escaleta. Si la capa técnica encuentra un adaptador de Chatterbox o un agente narrador reutilizables, puede reutilizarlos siempre que no cambie el comportamiento de los cuentos antiguos.
- `story-page-service` es el único punto por el que se reabren, cambian o eliminan páginas interiores de un cuento con escaleta, incluida la eliminación de páginas invalidadas que pide `beat-editor`.
- `generation-gate` concede un único turno para toda la aplicación y lo pueden pedir componentes de distintas funcionalidades (como ya hacen `story-page-service` y `portrait-generation-service`).
- `page-length-validator` cuenta como palabra cualquier palabra del texto, así que una onomatopeya (por ejemplo «¡Guau!») cuenta como una palabra sin tratamiento especial.
- El contenedor de Chatterbox Multilingual V3 acepta por cada petición un texto, un idioma, un audio de referencia y parámetros de expresividad (exageración, cfg, temperatura), tal como recoge `docs/product/producto.md` (SUP-007).
- La voz del sistema configurada es una voz en español (SUP-006): no se le configura idioma y se trata como español.

## Componentes

| Componente | Responsabilidad | Existente o nuevo |
|---|---|---|
| writer-agent | Redacta portada y páginas; sus instrucciones incorporan las reglas de onomatopeyas (máximo una, solo con sonido natural del beat, forma estándar y pronunciable, ninguna en portada) | existente |
| writer-model-provisioner | Registra el Modelfile del escritor, que pasa a incluir las reglas de onomatopeyas | existente |
| page-length-validator | Calcula el aviso de longitud contando la onomatopeya como una palabra más, sin cambios | existente |
| story-page-service | Además de lo actual, rechaza reabrir o cambiar páginas interiores mientras el cuento se narra y, al reabrir o eliminar páginas interiores de un cuento con audio, pide a `story-narration-service` que borre el audio y la voz guardada | existente |
| beat-editor | Su aviso de invalidación de páginas indica también que se borrará el audio cuando el cuento lo tenga | existente |
| story-page-repository | Proporciona el estado de las páginas interiores y su texto aceptado, sin cambios | existente |
| generation-gate | Concede y libera el turno único también para la narración | existente |
| websocket-adapter | Difunde también los eventos de narración | existente |
| voice-language-catalog | Lista fija de idiomas asignables a una voz, limitada a los que admite Chatterbox Multilingual V3, con el español como valor por defecto | nuevo |
| voice-audio-inspector | Valida un audio de voz subido: formato real WAV o MP3 y duración entre 5 y 30 segundos | nuevo |
| system-voice-provider | Lee de la configuración de la instalación el nombre y el audio de referencia de la voz del sistema y comprueba si son válidos | nuevo |
| voice-library-service | Casos de uso de la biblioteca: listado (voz del sistema más voces subidas), subida con validaciones, borrado de voces subidas y voces elegibles para narrar | nuevo |
| voice-repository | Persiste los metadatos de las voces subidas (nombre, idioma, declaración de permiso) | nuevo |
| voice-sample-store | Guarda, recupera y elimina el audio de referencia de las voces subidas | nuevo |
| narrator-agent | Agente Ollama que recibe el texto aceptado de todas las páginas interiores y devuelve el guion de audio con tono y énfasis por página, sin cambiar las palabras ni atender al número de palabras | nuevo |
| narrator-model-provisioner | Registra en Ollama al arrancar el Modelfile del narrador si no existe | nuevo |
| audio-script-validator | Comprueba de forma determinista que el guion de audio de cada página contiene exactamente las palabras del texto aceptado, en el mismo orden | nuevo |
| speech-synthesis-adapter | Sintetiza en Chatterbox Multilingual V3 el audio de un fragmento con el idioma español, el audio de referencia de la voz y los parámetros de expresividad | nuevo |
| story-narration-service | Orquesta narrar y renarrar: comprueba si se puede, reserva el turno, descarta el audio previo, ejecuta narrador, validación y síntesis en segundo plano, publica el audio solo si sale bien todo, guarda la voz, notifica y libera el turno | nuevo |
| narration-repository | Persiste el estado de narración de cada cuento, la voz con la que se narró y el audio de cada página interior | nuevo |
| narration-notifier | Publica los eventos de inicio, éxito y error de la narración | nuevo |
| narration-reconciliation-service | Al arrancar, trata como fallidas las narraciones que quedaron en curso, elimina su audio parcial y lo notifica | nuevo |
| voice-library-view | Vista de la biblioteca de voces: lista con nombre, idioma y marca de voz del sistema, aviso si la voz del sistema no está disponible, formulario de subida y borrado | nuevo |
| voice-library-api | Cliente REST de la biblioteca de voces y del catálogo de idiomas | nuevo |
| story-narration-panel | En los cuentos con escaleta: acción de narrar o renarrar (habilitada o no, con su motivo), elección de voz, aviso de sustitución, indicador de narración en curso, voz con la que se narró y resultado | nuevo |
| story-narration-api | Cliente REST para iniciar la narración, consultar su estado y descargar el audio de una página | nuevo |
| story-pages-panel | Además de lo actual: reproductor del audio de la página que se ve, aviso de borrado de audio al reabrir y bloqueo de cambios en páginas durante la narración | existente |
| story-modal | Integra `story-narration-panel` solo en los cuentos con escaleta | existente |
| story-script-panel | Su aviso de invalidación de páginas al editar un beat indica también el borrado del audio | existente |
| websocket-store | Difunde también los eventos de narración y vuelve a cargar el estado al reconectar | existente |

## Flujo principal

Subida de una voz:

1. `voice-library-view` abre el formulario con el idioma español preseleccionado, de entre los que da `voice-language-catalog` a través de `voice-library-api`.
2. El adulto indica nombre, idioma y fichero y marca la declaración de permiso. `voice-library-api` lo envía a `voice-library-service`.
3. `voice-library-service` comprueba que hay nombre, idioma del catálogo y declaración marcada, y que el nombre no coincide con el de otra voz subida ni con el de la voz del sistema (`system-voice-provider`). `voice-audio-inspector` valida el formato y la duración.
4. Si todo es válido, `voice-sample-store` guarda el audio y `voice-repository` guarda la voz. La biblioteca la muestra con su idioma.

Narración de un cuento con escaleta:

1. `story-narration-panel` consulta el estado de narración del cuento: si todas las páginas interiores están aceptadas, si hay narración en curso, si tiene audio y con qué voz. Solo habilita narrar o renarrar cuando todas las páginas interiores están aceptadas y no hay narración en curso.
2. Al pulsar narrar, el panel pide a `voice-library-service` las voces elegibles: la voz del sistema si es válida y las voces subidas en español. Preselecciona la voz del sistema. Si el cuento ya tiene audio, el panel muestra antes el aviso de que se sustituirá todo el audio y pide confirmación.
3. Al confirmar, `story-narration-service` comprueba que el cuento tiene escaleta, que todas sus páginas interiores están aceptadas y que la voz elegida sigue siendo elegible. Después pide el turno a `generation-gate`.
4. Con el turno concedido, `story-narration-service` borra en `narration-repository` el audio y la voz anteriores, si los había, marca el cuento como narrándose, toma el audio de referencia de la voz (de `voice-sample-store` o de `system-voice-provider`), avisa con `narration-notifier` del inicio y responde al adulto que la narración ha empezado.
5. En segundo plano, `story-narration-service` envía a `narrator-agent` el texto aceptado de todas las páginas interiores, en orden. El narrador devuelve un guion de audio por página, con fragmentos de la página y la expresividad de cada uno.
6. `audio-script-validator` comprueba, página a página, que los fragmentos juntos reproducen exactamente las palabras del texto aceptado.
7. Para cada página interior, `story-narration-service` pide a `speech-synthesis-adapter` la síntesis de sus fragmentos en español con la voz elegida y une el resultado en un único audio de página. Mientras dura, ese audio no se publica.
8. Cuando todas las páginas tienen audio, `story-narration-service` registra en una sola operación en `narration-repository` el audio de cada página interior y el nombre de la voz, y marca la narración como terminada. Descarta el guion de audio sin enseñarlo.
9. `narration-notifier` avisa del éxito y `generation-gate` libera el turno.
10. `story-narration-panel` muestra «narrado con <voz>», y `story-pages-panel` ofrece reproducir solo el audio de la página que se está viendo, sin pasar a la siguiente.

Generación de páginas con onomatopeyas:

1. `story-page-service` pide la página a `writer-agent` como hasta ahora. Las instrucciones del escritor, que registra `writer-model-provisioner`, incluyen las reglas de onomatopeyas de PDEC-030 aplicadas al beat de esa página. La portada se pide con la regla de no incluir onomatopeyas.
2. `page-length-validator` calcula el aviso de longitud con la onomatopeya contada como una palabra.

## Flujos alternativos y de fallo

### ALT-001 Voz subida no válida

- **Condición:** el fichero no es WAV ni MP3, dura menos de 5 o más de 30 segundos, falta el nombre o el idioma, la declaración de permiso no está marcada o el nombre ya existe (incluido el de la voz del sistema).
- **Comportamiento:** se rechaza la subida con un aviso que indica el motivo: formatos admitidos, rango de 5 a 30 segundos, idioma obligatorio, permiso obligatorio, nombre obligatorio o nombre duplicado.
- **Responsable:** voice-library-service
- **Resultado:** la biblioteca no cambia y no se guarda ningún audio.
- **Cubre:** REQ-006, REQ-007, REQ-008, REQ-009, REQ-010

### ALT-002 Voz del sistema ausente o inválida

- **Condición:** la configuración no define la voz del sistema o su audio de referencia no existe o no es válido.
- **Comportamiento:** la aplicación sigue funcionando. La biblioteca muestra un aviso de voz del sistema no disponible y la voz no se ofrece al narrar. En ese caso no hay voz preseleccionada y el adulto elige entre las voces subidas en español.
- **Responsable:** system-voice-provider
- **Resultado:** la biblioteca muestra el aviso y la lista de voces elegibles no incluye la voz del sistema.
- **Cubre:** REQ-011, REQ-012, REQ-019

### ALT-003 Sin voces elegibles

- **Condición:** el adulto pide narrar o renarrar y no hay ninguna voz elegible: la voz del sistema no está disponible y no hay voces subidas en español.
- **Comportamiento:** se rechaza la narración con un aviso de que no hay voces en español disponibles.
- **Responsable:** story-narration-service
- **Resultado:** no empieza la narración y el audio previo, si lo hay, no cambia.
- **Cubre:** REQ-020

### ALT-004 Otra generación en curso

- **Condición:** el adulto confirma narrar o renarrar y `generation-gate` no concede el turno.
- **Comportamiento:** se rechaza con el aviso habitual de generación en curso, antes de tocar el audio.
- **Responsable:** story-narration-service
- **Resultado:** no empieza la narración y el audio y la voz guardada siguen intactos.
- **Cubre:** REQ-021, REQ-022

### ALT-005 Condiciones del cuento no cumplidas al confirmar

- **Condición:** al confirmar, el cuento no tiene escaleta, alguna página interior no está aceptada, ya se está narrando o la voz elegida ya no es elegible (por ejemplo, se borró mientras el diálogo estaba abierto).
- **Comportamiento:** se rechaza con un aviso que indica el motivo (faltan páginas por aceptar, narración en curso o voz no disponible).
- **Responsable:** story-narration-service
- **Resultado:** no se pide el turno, no empieza la narración y el audio no cambia.
- **Cubre:** REQ-016, REQ-017, REQ-018, REQ-020, REQ-027

### ALT-006 Fallo del narrador, del guion de audio o de la síntesis

- **Condición:** el narrador falla o no responde con un guion utilizable, `audio-script-validator` detecta que el guion de alguna página añade, quita, reordena o sustituye palabras, o la síntesis de cualquier página falla.
- **Comportamiento:** la narración termina con error. Se elimina el audio parcial que se hubiera sintetizado, el cuento queda sin audio ni voz guardada (también en una renarración, SUP-003), se avisa del error y se libera el turno. No hay reintento automático: el adulto puede volver a narrar.
- **Responsable:** story-narration-service
- **Resultado:** el cuento muestra el aviso de error, ninguna página tiene audio, el indicador de narración en curso desaparece y narrar vuelve a estar habilitado.
- **Cubre:** REQ-024, REQ-028, REQ-032

### ALT-007 Reinicio de la aplicación durante la narración

- **Condición:** la aplicación se reinicia con una narración en curso. El turno en memoria se pierde y el trabajo en segundo plano no sigue.
- **Comportamiento:** al arrancar, la narración que quedó en curso se trata como fallida, igual que en ALT-006.
- **Responsable:** narration-reconciliation-service
- **Resultado:** el cuento queda sin audio, con aviso de error y con narrar habilitado. El turno está libre.
- **Cubre:** REQ-021, REQ-027, REQ-028

### ALT-008 Cambios en páginas durante la narración

- **Condición:** mientras un cuento se narra, el adulto intenta reabrir o cambiar una de sus páginas interiores o editar un beat que invalida páginas.
- **Comportamiento:** se rechaza con un aviso de narración en curso (SUP-004).
- **Responsable:** story-page-service
- **Resultado:** las páginas y la narración en curso no cambian.
- **Cubre:** REQ-027

### ALT-009 Reapertura o invalidación de páginas en un cuento con audio

- **Condición:** el adulto reabre o cambia una página interior, o edita un beat que elimina páginas interiores, en un cuento con audio.
- **Comportamiento:** el aviso de confirmación indica, además del descarte en cascada habitual, que se borrará todo el audio del cuento. Si el adulto confirma, se borran el audio de todas las páginas y la voz guardada junto con la operación de páginas. Si cancela, no cambia nada.
- **Responsable:** story-page-service
- **Resultado:** tras confirmar, ninguna página tiene audio, el cuento no indica voz y narrar queda deshabilitado hasta que todas las páginas interiores vuelvan a estar aceptadas. Tras cancelar, la página no se reabre y el audio sigue intacto.
- **Cubre:** REQ-033, REQ-034

### ALT-010 Borrado de una voz usada

- **Condición:** el adulto borra una voz subida que se usó para narrar uno o más cuentos, o que usa una narración en curso.
- **Comportamiento:** la voz se retira de la biblioteca y se elimina su audio de referencia. Los cuentos narrados conservan su audio y el nombre guardado. Una narración en curso sigue con el audio de referencia que tomó al empezar. El adulto no puede borrar la voz del sistema: la biblioteca no ofrece esa opción y el servidor rechaza el intento con un aviso.
- **Responsable:** voice-library-service
- **Resultado:** la voz desaparece de la biblioteca y de las voces elegibles, y los cuentos narrados con ella siguen reproduciéndose e indicando su nombre.
- **Cubre:** REQ-013, REQ-014, REQ-015, REQ-016

## Interfaces entre componentes

| Origen | Destino | Qué se comunica | Protocolo |
|---|---|---|---|
| voice-library-api | voice-library-service | Listar la biblioteca y los idiomas, subir una voz con su audio y declaración, borrar una voz subida y consultar las voces elegibles | openapi |
| voice-library-service | voice-audio-inspector | Validar el formato y la duración de un audio | - |
| voice-library-service | system-voice-provider | Obtener el nombre y la disponibilidad de la voz del sistema | - |
| voice-library-service | voice-repository, voice-sample-store | Guardar, listar y borrar voces y su audio de referencia | - |
| voice-library-service | voice-language-catalog | Comprobar que el idioma está entre los admitidos | - |
| story-narration-api | story-narration-service | Iniciar narración o renarración con una voz, consultar el estado de narración de un cuento y descargar el audio de una página interior | openapi |
| story-narration-service | generation-gate | Pedir y liberar el turno único | - |
| story-narration-service | voice-library-service | Comprobar que la voz es elegible y obtener su audio de referencia | - |
| story-narration-service | story-page-repository | Leer el estado de las páginas interiores y su texto aceptado | - |
| story-narration-service | narrator-agent | Enviar el texto aceptado de todas las páginas interiores y recibir el guion de audio por página | - |
| story-narration-service | audio-script-validator | Comprobar que el guion de audio conserva las palabras de cada página | - |
| story-narration-service | speech-synthesis-adapter | Pedir la síntesis de cada fragmento con idioma, voz y expresividad | - |
| story-narration-service | narration-repository | Registrar estado, voz guardada y audio por página, y borrarlos | - |
| story-narration-service | narration-notifier | Avisar del inicio, el éxito o el error de la narración | - |
| narration-notifier | websocket-adapter | Difundir los eventos de narración | websocket |
| websocket-store | story-narration-panel, story-pages-panel | Avisar de los eventos de narración para recargar el estado | - |
| story-page-service | story-narration-service | Consultar si el cuento se está narrando o tiene audio, y pedir el borrado del audio al reabrir o eliminar páginas interiores | - |
| story-pages-api | story-page-service | La vista de páginas indica si el cuento tiene audio (para el aviso de reapertura) y si se está narrando | openapi |
| narration-reconciliation-service | narration-repository, narration-notifier | Al arrancar, cerrar como fallidas las narraciones en curso y avisarlo | websocket |

## Modelo de datos lógico

- Voz subida: voz que sube el adulto, con nombre único, idioma declarado y declaración de permiso. Relaciones: tiene un audio de referencia. No tiene vínculo persistente con las narraciones, que guardan una copia del nombre.
- Audio de referencia de voz: el fichero de muestra de una voz subida. Relaciones: pertenece a una voz subida y se borra con ella.
- Voz del sistema: entrada de la biblioteca que se construye con la configuración de la instalación y no se persiste. Relaciones: ninguna.
- Idioma de voz: valor del catálogo fijo de idiomas que admite el motor. Relaciones: lo usa cada voz subida.
- Narración del cuento: estado de narración de un cuento con escaleta (en curso, terminada o con error) y nombre de la voz con la que se narró. Relaciones: pertenece a un cuento (cero o una por cuento) y agrupa los audios de página.
- Audio de página: audio narrado de una página interior. Relaciones: pertenece a una narración del cuento y a una página interior. La portada nunca tiene audio de página.
- Guion de audio: resultado transitorio del narrador (fragmentos y expresividad por página). Solo vive durante la narración y no se persiste ni se muestra.

## Decisiones

### DEC-001 Idiomas asignables limitados a los del motor

- **Decisión:** el idioma de una voz se elige de un catálogo fijo en el servidor con los idiomas que admite Chatterbox Multilingual V3. El español es el valor preseleccionado, y la app obtiene la lista del servidor.
- **Alternativas descartadas:** texto libre, porque permitiría idiomas que el motor no sintetiza y haría ambiguo el filtro de REQ-020. Solo español, porque el requisito pide declarar el idioma y mostrarlo, y las voces en otros idiomas tienen sentido como entradas no elegibles. Lista fija en la app, porque duplica en el cliente la validación que hace el servidor.
- **Cubre:** REQ-008, REQ-020

### DEC-002 El narrador anota fragmentos y no reescribe el texto, con comprobación determinista

- **Decisión:** el narrador recibe el cuento entero y devuelve, para cada página, la división de su texto aceptado en fragmentos con la expresividad de cada uno (tono y énfasis, expresados en los parámetros de Chatterbox: exageración, cfg y temperatura). La síntesis se hace por fragmentos y se une en un audio por página. Antes de sintetizar, `audio-script-validator` comprueba que los fragmentos reproducen las palabras del texto aceptado, sin tener en cuenta la puntuación ni las mayúsculas. Si no las reproducen, la narración falla (ALT-006). El narrador no tiene en cuenta el número de palabras.
- **Alternativas descartadas:** que el narrador devuelva el texto reescrito con marcas, que se sintetizaría tal cual, porque no garantiza REQ-024. Un único ajuste de expresividad por página sin fragmentos, porque no permite el énfasis dentro de la página. Si el guion no es válido, sintetizar el texto sin anotar, porque sería una política de recuperación que los requisitos no definen.
- **Cubre:** REQ-023, REQ-024, REQ-026

### DEC-003 Audio publicado solo al terminar con éxito

- **Decisión:** el audio de las páginas se sintetiza aparte y no se publica en el cuento hasta que todas las páginas interiores tienen audio. Entonces se registra junto con la voz en una sola operación. El audio previo se descarta justo después de conseguir el turno.
- **Alternativas descartadas:** guardar y mostrar el audio página a página, porque dejaría audio parcial si algo falla (SUP-002). Conservar el audio previo hasta que termine la renarración, porque contradice SUP-003.
- **Cubre:** REQ-022, REQ-026, REQ-028, REQ-032

### DEC-004 Narración asíncrona con turno único y notificación por WebSocket

- **Decisión:** la narración sigue el patrón de retratos y páginas: se pide el turno al confirmar, se responde enseguida que ha empezado, el trabajo se hace en segundo plano, el resultado se notifica por WebSocket y el estado se persiste para mostrar el indicador al consultar o reconectar. El turno se libera al terminar con éxito o con error.
- **Alternativas descartadas:** una petición síncrona que espere a que termine toda la síntesis, porque es larga y no permite mostrar el indicador ni bloquear de forma fiable.
- **Cubre:** REQ-021, REQ-027, REQ-028

### DEC-005 La aplicación es la dueña de la biblioteca de voces

- **Decisión:** las voces subidas y su audio de referencia se guardan en la aplicación, y el audio de referencia se pasa al motor en cada síntesis. La voz del sistema se lee de la configuración de la instalación (nombre y audio de referencia) y no se persiste. Al borrar una voz subida se eliminan su registro y su audio de referencia, y cada narración guarda una copia del nombre de la voz.
- **Alternativas descartadas:** usar la biblioteca de voces del contenedor de Chatterbox, porque desdobla la fuente de verdad y complica los nombres únicos, la voz del sistema y el borrado. Un borrado lógico de las voces, porque REQ-015 se cumple con la copia del nombre y no hace falta conservar la muestra.
- **Cubre:** REQ-010, REQ-011, REQ-012, REQ-013, REQ-014, REQ-015, REQ-029

### DEC-006 Reglas de onomatopeyas en las instrucciones del escritor

- **Decisión:** las reglas de PDEC-030 (máximo una por página, solo si el beat tiene un sonido natural, forma estándar y pronunciable, ninguna en la portada) se incorporan al Modelfile y a las instrucciones de página y portada del escritor. No se añade una validación nueva ni se cambia `page-length-validator`. Si el resultado no cumple, el adulto lo corrige con el hint de página que ya existe.
- **Alternativas descartadas:** un validador determinista con una lista cerrada de onomatopeyas y regeneración automática, porque exige un léxico cerrado difícil de mantener, introduce una política de reintento que los requisitos no definen y podría cambiar la validación de longitud, que queda fuera de alcance.
- **Cubre:** REQ-001, REQ-002, REQ-003, REQ-004, REQ-005

### DEC-007 Borrado del audio ligado a las operaciones de páginas

- **Decisión:** `story-page-service` es el único que decide borrar el audio. Lo hace en la misma operación en la que reabre o elimina páginas interiores de un cuento con audio, incluidas las que invalida `beat-editor`, y rechaza esas operaciones mientras el cuento se narra. La disponibilidad de narrar se calcula siempre a partir del estado actual de las páginas interiores.
- **Alternativas descartadas:** que la narración detecte después los cambios de las páginas y borre el audio por su cuenta, porque deja un tiempo con audio que no corresponde al texto y reparte la responsabilidad.
- **Cubre:** REQ-018, REQ-027, REQ-033, REQ-034

## Riesgos

- El narrador puede devolver con frecuencia guiones que no conservan las palabras, lo que haría fallar la narración. Para mitigarlo, el Modelfile del narrador se limita a dividir y anotar, la comparación no tiene en cuenta la puntuación ni las mayúsculas y el fallo se avisa con claridad para volver a narrar.
- Unir fragmentos sintetizados por separado puede dejar cortes o cambios bruscos de timbre dentro de una página. Para mitigarlo, el narrador debe usar pocos fragmentos, que coincidan con frases completas.
- Ollama y Chatterbox compiten por la VRAM (12 GB). Para mitigarlo, las fases van en orden: primero el narrador con el cuento entero y después la síntesis, nunca a la vez.
- El escritor puede no cumplir las reglas de onomatopeyas, porque no hay una validación determinista. Para mitigarlo, las reglas son explícitas y van ligadas al beat, y el adulto puede corregir con un hint.
- Se puede configurar como voz del sistema un nombre que ya tiene una voz subida. Para mitigarlo, `system-voice-provider` lo detecta al arrancar y lo avisa en la biblioteca, y se rechazan las subidas nuevas con ese nombre.
- Puede haber código de FUN-004 sin mapear que choque con los componentes nuevos. Para mitigarlo, la capa técnica debe revisar si existe y conservar el comportamiento de los cuentos antiguos.

## Trazabilidad

| Requisito | Componentes | Decisiones |
|---|---|---|
| REQ-001 | writer-agent, writer-model-provisioner | DEC-006 |
| REQ-002 | writer-agent, writer-model-provisioner | DEC-006 |
| REQ-003 | writer-agent, writer-model-provisioner | DEC-006 |
| REQ-004 | writer-agent, writer-model-provisioner | DEC-006 |
| REQ-005 | page-length-validator, writer-agent | DEC-006 |
| REQ-006 | voice-audio-inspector, voice-library-service, voice-library-view | - |
| REQ-007 | voice-audio-inspector, voice-library-service, voice-library-view | - |
| REQ-008 | voice-language-catalog, voice-library-service, voice-library-view, voice-library-api | DEC-001 |
| REQ-009 | voice-library-service, voice-library-view | - |
| REQ-010 | voice-library-service, voice-repository, system-voice-provider | DEC-005 |
| REQ-011 | voice-library-service, system-voice-provider, voice-repository, voice-library-view, voice-library-api | DEC-005 |
| REQ-012 | system-voice-provider, voice-library-service | DEC-005 |
| REQ-013 | voice-library-service, voice-library-view | DEC-005 |
| REQ-014 | voice-library-service, voice-repository, voice-sample-store, voice-library-view | DEC-005 |
| REQ-015 | voice-library-service, narration-repository | DEC-005 |
| REQ-016 | voice-library-service, story-narration-service, story-narration-panel | DEC-005 |
| REQ-017 | story-narration-service, story-modal, story-narration-panel | - |
| REQ-018 | story-narration-service, story-narration-panel, story-page-repository | DEC-007 |
| REQ-019 | story-narration-panel, voice-library-service, system-voice-provider | - |
| REQ-020 | voice-library-service, voice-language-catalog, story-narration-service, story-narration-panel | DEC-001 |
| REQ-021 | story-narration-service, generation-gate, narration-reconciliation-service | DEC-004 |
| REQ-022 | story-narration-service, generation-gate | DEC-003, DEC-004 |
| REQ-023 | narrator-agent, narrator-model-provisioner, story-narration-service, speech-synthesis-adapter | DEC-002 |
| REQ-024 | narrator-agent, audio-script-validator, story-narration-service | DEC-002 |
| REQ-025 | story-narration-service, narrator-agent | DEC-002 |
| REQ-026 | story-narration-service, speech-synthesis-adapter, narration-repository | DEC-002, DEC-003 |
| REQ-027 | story-narration-service, narration-repository, narration-notifier, websocket-adapter, websocket-store, story-narration-panel, story-page-service | DEC-004, DEC-007 |
| REQ-028 | story-narration-service, narration-repository, narration-notifier, narration-reconciliation-service, generation-gate, story-narration-panel | DEC-003, DEC-004 |
| REQ-029 | story-narration-service, narration-repository, story-narration-panel | DEC-005 |
| REQ-030 | story-pages-panel, story-narration-api, narration-repository | - |
| REQ-031 | story-narration-panel, story-narration-service, voice-library-service | - |
| REQ-032 | story-narration-panel, story-narration-service | DEC-003 |
| REQ-033 | story-page-service, story-narration-service, beat-editor, story-pages-panel, story-script-panel | DEC-007 |
| REQ-034 | story-page-service, story-narration-service, story-narration-panel | DEC-007 |

No hay requisitos no funcionales (`NFR-xxx`) en requirements.md.

## Contratos afectados

| Protocolo | Contrato | Cambio |
|---|---|---|
| openapi | docs/contracts/openapi/openapi.yaml | Registrar las operaciones nuevas de biblioteca de voces y de narración |
| openapi | docs/contracts/openapi/paths/voice/ (nuevo) | Crear las operaciones de la biblioteca de voces: listado con la voz del sistema y su disponibilidad, catálogo de idiomas, subida de voz con audio, idioma, nombre y declaración de permiso (con los rechazos de ALT-001) y borrado de una voz subida (con el rechazo para la voz del sistema) |
| openapi | docs/contracts/openapi/paths/story/ (narración, nuevo) | Crear las operaciones para iniciar la narración o renarración con una voz (rechazos de ALT-003, ALT-004 y ALT-005), consultar el estado de narración de un cuento y descargar el audio de una página interior |
| openapi | docs/contracts/openapi/schemas/ (voz, biblioteca, idioma, narración, nuevos) | Crear los esquemas de voz, biblioteca de voces, idioma de voz, estado de narración (en curso, terminada, con error, sin audio, motivo de deshabilitado y voz usada) |
| openapi | docs/contracts/openapi/schemas/story-pages-view.yaml, page-detail.yaml | Añadir si el cuento tiene audio, si se está narrando y si cada página interior tiene audio |
| openapi | docs/contracts/openapi/paths/story/ (reapertura y cambio de página), beat-edit-service.yaml | Documentar que se borra el audio del cuento al reabrir, cambiar o invalidar páginas interiores y el nuevo rechazo por narración en curso |
| websocket | docs/contracts/websocket/websocket.md | Añadir los eventos de narración de un cuento: inicio, éxito con la voz usada y error |

## Cambios sugeridos al mapa

- `+ voice-language-catalog | api/src/main/java/es/vargontoc/storyteller/domain/service/VoiceLanguageCatalog.java | Idiomas asignables a una voz, limitados a Chatterbox Multilingual V3 | -`
- `+ voice-audio-inspector | api/src/main/java/es/vargontoc/storyteller/infrastructure/adapters/out/external/VoiceAudioInspector.java | Valida formato WAV o MP3 y duración de 5 a 30 segundos | -`
- `+ system-voice-provider | api/src/main/java/es/vargontoc/storyteller/infrastructure/config/SystemVoiceProvider.java | Voz del sistema desde la configuración de la instalación y su validez | -`
- `+ voice-library-service | api/src/main/java/es/vargontoc/storyteller/application/service/VoiceLibraryService.java | Biblioteca de voces: listado, subida validada, borrado y voces elegibles | voice-repository, voice-sample-store, voice-audio-inspector, system-voice-provider, voice-language-catalog`
- `+ voice-repository | api/src/main/java/es/vargontoc/storyteller/infrastructure/adapters/out/persistence/VoiceRepositoryAdapter.java | Persistencia de voces subidas con nombre único, idioma y permiso | JPA`
- `+ voice-sample-store | api/src/main/java/es/vargontoc/storyteller/infrastructure/adapters/out/persistence/VoiceSampleStoreAdapter.java | Almacena y elimina el audio de referencia de voces subidas | -`
- `+ narrator-agent | api/src/main/java/es/vargontoc/storyteller/infrastructure/adapters/out/external/SpringAiNarratorAgent.java | Agente Ollama que anota tono y énfasis por fragmentos sin alterar texto | Spring AI, Ollama`
- `+ narrator-model-provisioner | api/src/main/java/es/vargontoc/storyteller/infrastructure/config/NarratorModelProvisioner.java | Registra en Ollama el Modelfile del narrador al arrancar | Ollama, narrator-agent`
- `+ audio-script-validator | api/src/main/java/es/vargontoc/storyteller/domain/service/AudioScriptValidator.java | Comprueba que el guion de audio conserva las palabras de cada página | -`
- `+ speech-synthesis-adapter | api/src/main/java/es/vargontoc/storyteller/infrastructure/adapters/out/external/ChatterboxSpeechSynthesisAdapter.java | Sintetiza fragmentos en Chatterbox Multilingual V3 con voz y expresividad | Chatterbox`
- `+ story-narration-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryNarrationService.java | Orquesta narrar y renarrar con turno, narrador, validación, síntesis y publicación atómica | generation-gate, voice-library-service, story-page-repository, narrator-agent, audio-script-validator, speech-synthesis-adapter, narration-repository, narration-notifier`
- `+ narration-repository | api/src/main/java/es/vargontoc/storyteller/infrastructure/adapters/out/persistence/StoryNarrationRepositoryAdapter.java | Estado de narración, voz usada y audio por página interior | JPA`
- `+ narration-notifier | api/src/main/java/es/vargontoc/storyteller/application/service/StoryNarrationNotifier.java | Publica eventos de inicio, éxito y error de narración | websocket-adapter`
- `+ narration-reconciliation-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryNarrationReconciliationService.java | Al arrancar, cierra como fallidas las narraciones en curso | narration-repository, narration-notifier`
- `~ story-page-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageService.java | Además: bloquea cambios durante la narración y borra audio al reabrir o invalidar | story-page-repository, story-outline-repository, story-page-version-repository, writer-agent, page-length-validator, generation-gate, story-page-notifier, story-narration-service`
- `~ writer-agent | api/src/main/java/es/vargontoc/storyteller/infrastructure/adapters/out/external/SpringAiWriterAgent.java | Escritor Ollama: portada y páginas con reglas de onomatopeyas | Spring AI, Ollama`
- `~ websocket-adapter | api/src/main/java/es/vargontoc/storyteller/infrastructure/adapters/in/websocket/WebsocketAdapter.java | Difusor /storyteller: notificaciones, retratos, páginas y narración | Spring WebSocket`
- `+ voice-library-view | app/src/views/VoiceLibraryView.vue | Biblioteca de voces: listado, aviso de voz del sistema, subida y borrado | voice-library-api, ConfirmModal`
- `+ voice-library-api | app/src/api/voices.ts | Cliente REST de biblioteca de voces e idiomas | httpClient`
- `+ story-narration-panel | app/src/components/StoryNarrationPanel.vue | Narrar o renarrar: voz, aviso de sustitución, indicador y resultado | story-narration-api, voice-library-api, websocket-store, ConfirmModal`
- `+ story-narration-api | app/src/api/storyNarration.ts | Cliente REST de narración: iniciar, estado y audio por página | httpClient`
- `~ story-pages-panel | app/src/components/StoryPagesPanel.vue | Además: reproduce el audio de la página, avisa del borrado y bloquea durante la narración | story-pages-api, story-narration-api, websocket-store, ConfirmModal`
- `~ story-modal | app/src/components/StoryModal.vue | Además: integra el panel de narración solo en cuentos con escaleta | story-script-panel, story-script-api, story-pages-panel, story-narration-panel`
- `~ websocket-store | app/src/stores/websocket.ts | Además: difunde los eventos de narración de cuento | Pinia`
- `+ openapi | docs/contracts/openapi/paths/voice/ | storyteller-app -> storyteller-server`
- `+ openapi | docs/contracts/openapi/paths/story/ (narración) | storyteller-app -> storyteller-server`
- `~ storyteller-server | api/ | Lógica de negocio, orquestación de agentes, persistencia y API REST/WS | Java 21, Spring Boot 4.1.1, Spring AI 2.0.0, JPA, Liquibase` (el mapa dice 4.1.0 y `.sdd/project.md` dice 4.1.1)
- Desactualizado: el mapa no recoge los componentes de narración y audio de FUN-004 (cuentos antiguos) ni el Modelfile `narrator` que cita `.sdd/project.md`. Hay que añadirlos si existen.
