# Tareas: agent-narrator

Estado: aprobado
Revisión: 1
Basado en: requirements@1, design@1

## Resumen

Primero se revisa el código de FUN-004 que se puede reutilizar. Después van las reglas de onomatopeyas del escritor, la biblioteca de voces (dominio, persistencia y servicio), las piezas de narración (repositorio, narrador, validador del guion, síntesis, notificación, orquestación y reconciliación) y los cambios en las operaciones de páginas. Al final se integra el cliente (APIs, store, vistas y paneles) y se formalizan los contratos según la estrategia `implementation-first`. Son 42 tareas.

## Tareas

### [x] TASK-001 Verificar piezas de FUN-004 reutilizables

- **Cubre:** REQ-017, REQ-023, REQ-026
- **Componente:** código de narración y síntesis de voz de FUN-004 (cuentos antiguos) y Modelfile `narrator` | no consta en el mapa
- **Depende de:** -
- **Alcance:** comprobar en el código si ya existen un adaptador de Chatterbox, un agente narrador o un Modelfile `narrator` de FUN-004, y decidir si `speech-synthesis-adapter`, `narrator-agent` y `narrator-model-provisioner` los reutilizan sin cambiar el comportamiento de los cuentos antiguos.
- **Hecho cuando:** queda anotado en la propia tarea, para TASK-013, TASK-014 y TASK-016, qué piezas existen, dónde están y si se reutilizan o no, y las pruebas actuales de los cuentos antiguos siguen pasando.
- **Decisión TASK-001:** no existen `narrator.modelfile`, provisionador ni puerto `NarratorAgent` (el agente antiguo es un `ChatClient` dentro de `AudioStoryGenerationAdapter`). TASK-013 y TASK-014: se crean piezas nuevas, con modelo/propiedad distinta (p. ej. `app.agents.audio-narrator`), sin tocar el flujo antiguo. TASK-016: se reutiliza el contrato `AudioGeneratorPort` y `AudioGenerationRequest` como base, pero con adaptador/petición nuevos (idioma, muestra, errores explícitos). `AudioGeneratorAdapter`, `PageAudioAssetGenerator` y `AudioStoryGenerationAdapter` quedan intactos.
- **Pruebas automáticas:**
  - Las pruebas existentes de narración y audio de los cuentos antiguos, si las hay, pasan sin cambios.
- **Prueba manual:**
  1. Abrir un cuento antiguo sin escaleta que tenga audio de FUN-004 y reproducir una página: se oye igual que antes y no aparece la narración de esta funcionalidad.

### [x] TASK-002 Reglas de onomatopeyas en el Modelfile del escritor

- **Cubre:** REQ-001, REQ-002, REQ-003, REQ-004, DEC-006
- **Componente:** writer-model-provisioner | api/src/main/java/es/vargontoc/storyteller/infrastructure/config/WriterModelProvisioner.java
- **Depende de:** -
- **Alcance:** el Modelfile del escritor que registra el provisionador incluye las reglas de PDEC-030: como máximo una onomatopeya por página, solo si el beat tiene un sonido natural en la escena, en forma estándar y pronunciable del español, y ninguna en la portada.
- **Hecho cuando:** el Modelfile registrado contiene las cuatro reglas y el provisionador sigue registrando el modelo al arrancar si no existe.
- **Pruebas automáticas:**
  - El recurso del Modelfile del escritor contiene las cuatro reglas de onomatopeyas.
  - Si el modelo no existe en Ollama al arrancar, se registra con el Modelfile actualizado. Si ya existe, no se vuelve a registrar (comportamiento actual).
- **Prueba manual:** No aplica: el efecto en el texto generado se comprueba en la prueba manual de TASK-003.

### [x] TASK-003 Instrucciones de página y portada con reglas de onomatopeyas

- **Cubre:** REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, DEC-006
- **Componente:** writer-agent | api/src/main/java/es/vargontoc/storyteller/infrastructure/adapters/out/external/SpringAiWriterAgent.java
- **Depende de:** TASK-002
- **Alcance:** las instrucciones de página interior aplican las reglas de onomatopeyas al beat de esa página, y las de portada prohíben las onomatopeyas, sin cambiar el límite de palabras ni la validación de longitud.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-001 a REQ-004 y las instrucciones enviadas al modelo incluyen las reglas ligadas al beat (página) o la prohibición (portada).
- **Pruebas automáticas:**
  - La petición de una página interior incluye las reglas de onomatopeyas y el beat de esa página.
  - La petición de la portada incluye la regla de no incluir onomatopeyas.
  - Las instrucciones mantienen el mismo objetivo de palabras que antes.
- **Prueba manual:**
  1. Generar un cuento con escaleta en el que un beat tenga un sonido natural (por ejemplo, un perro que ladra) y otro sea silencioso (por ejemplo, mirar las estrellas).
  2. Comprobar que cada página interior tiene como mucho una onomatopeya, que la página silenciosa no tiene ninguna y que la de sonido, si tiene, corresponde a ese sonido.
  3. Comprobar que las onomatopeyas son formas reconocibles («guau», «pum»), sin letras repetidas alargadas ni símbolos.
  4. Generar la portada y comprobar que no incluye ninguna onomatopeya.

### [x] TASK-004 Onomatopeya contada como palabra en el aviso de longitud

- **Cubre:** REQ-005, DEC-006
- **Componente:** page-length-validator | api/src/main/java/es/vargontoc/storyteller/domain/service/PageLengthValidator.java
- **Depende de:** -
- **Alcance:** confirmar con pruebas que el validador, sin cambios, cuenta una onomatopeya como una palabra más.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-005 y el validador no ha cambiado de comportamiento.
- **Pruebas automáticas:**
  - Un texto con «¡Guau!» cuenta esa onomatopeya como una palabra.
  - Un texto con onomatopeya que llega justo al límite vigente de palabras no da aviso de palabras.
  - El mismo texto con una palabra más da el aviso de demasiadas palabras, igual que sin onomatopeya.
- **Prueba manual:** No aplica: el comportamiento es determinista y queda cubierto por las pruebas automáticas.

### [x] TASK-005 Catálogo de idiomas de voz

- **Cubre:** REQ-008, REQ-020, DEC-001
- **Componente:** voice-language-catalog | api/src/main/java/es/vargontoc/storyteller/application/service/VoiceLanguageCatalog.java (y api/src/main/java/es/vargontoc/storyteller/domain/model/VoiceLanguage.java)
- **Depende de:** -
- **Alcance:** lista fija de idiomas que se pueden asignar a una voz, limitada a los que admite Chatterbox Multilingual V3, con el español como valor por defecto.
- **Hecho cuando:** el catálogo devuelve los idiomas admitidos, indica el español como valor por defecto y permite comprobar si un idioma pertenece al catálogo.
- **Pruebas automáticas:**
  - El catálogo incluye el español y lo marca como valor por defecto.
  - Un idioma del catálogo se reconoce como admitido.
  - Un idioma que no está en el catálogo o un valor vacío no se reconocen como admitidos.
- **Prueba manual:** No aplica: no tiene interfaz propia. Se ve en la prueba manual de TASK-030.

### [x] TASK-006 Inspección del audio de voz subido

- **Cubre:** REQ-006, REQ-007, ALT-001
- **Componente:** voice-audio-inspector | api/src/main/java/es/vargontoc/storyteller/application/service/VoiceAudioInspector.java
- **Depende de:** -
- **Alcance:** validar que un audio subido es realmente WAV o MP3 y que dura entre 5 y 30 segundos, ambos incluidos.
- **Hecho cuando:** el inspector distingue el formato real y la duración e indica el motivo del rechazo (formato o duración).
- **Pruebas automáticas:**
  - Un WAV válido de 10 segundos se acepta.
  - Un MP3 válido de 10 segundos se acepta.
  - Un fichero OGG se rechaza por formato.
  - Un documento de texto con extensión de audio se rechaza por formato.
  - Un audio de 4 segundos se rechaza por duración.
  - Un audio de 31 segundos se rechaza por duración.
  - Un audio de 5 segundos y otro de 30 segundos se aceptan.
- **Prueba manual:** No aplica: no tiene interfaz propia. Se ve en la prueba manual de TASK-030.

### [x] TASK-007 Persistencia de voces subidas

- **Cubre:** REQ-009, REQ-010, REQ-011, REQ-014, DEC-005
- **Componente:** voice-repository | api/src/main/java/es/vargontoc/storyteller/application/ports/out/persistence/VoiceRepository.java (adaptador `infrastructure/adapters/out/persistence/VoiceRepositoryAdapter.java`, entidad `infrastructure/persistence/VoiceJpaEntity.java`, mapper `infrastructure/mappers/VoiceMapper.java`, modelo `domain/model/Voice.java`, migración `api/src/main/resources/db/changelog/migrations/026_create_voice.xml`)
- **Depende de:** -
- **Alcance:** guardar, listar, buscar por nombre y borrar voces subidas con su nombre único, su idioma y su declaración de permiso.
- **Hecho cuando:** las voces se guardan con sus tres datos, se listan, se pueden buscar por nombre, se borran y la persistencia impide dos voces con el mismo nombre.
- **Pruebas automáticas:**
  - Una voz guardada se lista con su nombre, su idioma y su declaración de permiso.
  - Guardar una segunda voz con un nombre ya usado se rechaza y la primera no cambia.
  - Buscar por un nombre existente devuelve la voz y por uno inexistente no devuelve nada.
  - Una voz borrada deja de listarse.
- **Prueba manual:** No aplica: no tiene interfaz propia. Se ve en la prueba manual de TASK-030.

### [x] TASK-008 Almacén del audio de referencia de voces subidas

- **Cubre:** REQ-014, DEC-005
- **Componente:** voice-sample-store | api/src/main/java/es/vargontoc/storyteller/application/ports/out/VoiceSampleStore.java (adaptador `infrastructure/storage/VoiceSampleStoreAdapter.java`)
- **Depende de:** -
- **Alcance:** guardar, recuperar y eliminar el audio de referencia de una voz subida.
- **Hecho cuando:** el audio guardado se recupera idéntico y, una vez eliminado, ya no se puede recuperar.
- **Pruebas automáticas:**
  - Guardar un audio y recuperarlo devuelve el mismo contenido.
  - Tras eliminarlo, al recuperarlo se indica que no existe.
- **Prueba manual:** No aplica: no tiene interfaz propia. Se ve en la prueba manual de TASK-030.

### [x] TASK-009 Voz del sistema desde la configuración

- **Cubre:** REQ-010, REQ-011, REQ-012, REQ-019, DEC-005, ALT-002
- **Componente:** system-voice-provider | api/src/main/java/es/vargontoc/storyteller/application/service/SystemVoiceProvider.java (propiedades `infrastructure/config/SystemVoiceProperties.java`, prefijo `app.system-voice` en `application.yaml`)
- **Depende de:** TASK-006, TASK-007
- **Alcance:** leer de la configuración de la instalación el nombre y el audio de referencia de la voz del sistema, sin un valor por defecto en el código, comprobar si están disponibles y válidos, y detectar al arrancar si el nombre coincide con el de una voz subida.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-012 y el proveedor indica el nombre, si la voz está disponible y, si no lo está, el motivo.
- **Pruebas automáticas:**
  - Con un nombre y un audio de referencia válidos configurados (valores de prueba), la voz está disponible con ese nombre.
  - Con la configuración cambiada a otro nombre y otro audio, al reiniciar el contexto la voz aparece con el nuevo nombre.
  - Sin voz del sistema configurada, la voz no está disponible y la aplicación arranca (ALT-002).
  - Con un audio de referencia que no existe o que no es válido, la voz no está disponible y la aplicación arranca (ALT-002).
  - Si el nombre configurado coincide con el de una voz subida, se detecta al arrancar y queda disponible para el aviso en la biblioteca.
- **Prueba manual:**
  1. Arrancar la aplicación sin configurar la voz del sistema: arranca sin error.

### [x] TASK-010 Listado de la biblioteca y subida de voces

- **Cubre:** REQ-006, REQ-007, REQ-008, REQ-009, REQ-010, REQ-011, REQ-012, DEC-001, DEC-005, ALT-001, ALT-002
- **Componente:** voice-library-service | api/src/main/java/es/vargontoc/storyteller/application/service/VoiceLibraryService.java (puerto `application/ports/in/generator/VoiceLibraryUseCase.java`, comando `domain/request/UploadVoiceCommand.java`, respuestas `domain/response/VoiceLibrary.java` y `VoiceEntry.java`, causas `domain/enums/VoiceRejectionReason.java`, error `shared/exceptions/VoiceRejectedException.java`)
- **Depende de:** TASK-005, TASK-006, TASK-007, TASK-008, TASK-009
- **Alcance:** listar la voz del sistema (con su disponibilidad) y las voces subidas con nombre, idioma y marca de voz del sistema, y subir una voz validando nombre, idioma, permiso, unicidad del nombre, formato y duración.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-006 a REQ-012 en el servidor. Las subidas rechazadas no guardan ni la voz ni su audio.
- **Pruebas automáticas:**
  - Con la voz del sistema disponible y dos voces subidas, el listado devuelve las tres con nombre e idioma y la del sistema marcada como tal.
  - Con la voz del sistema no disponible, el listado indica que no está disponible y el motivo (ALT-002).
  - Si el nombre de la voz del sistema coincide con el de una voz subida, el listado lo indica.
  - Una subida válida en WAV o MP3, con nombre, idioma del catálogo y permiso marcado, guarda la voz y su audio y la voz aparece en el listado con su idioma.
  - Subida en OGG: se rechaza con el aviso de formatos admitidos (ALT-001).
  - Subida de 4 o de 31 segundos: se rechaza con el aviso de rango de 5 a 30 segundos (ALT-001).
  - Subida sin idioma o con un idioma que no está en el catálogo: se rechaza con aviso de idioma obligatorio (ALT-001).
  - Subida sin declaración de permiso: se rechaza con aviso (ALT-001).
  - Subida sin nombre: se rechaza con aviso de nombre obligatorio (ALT-001).
  - Subida con el nombre de otra voz subida o con el de la voz del sistema: se rechaza con aviso de nombre duplicado (ALT-001).
  - En todos los rechazos, el listado no cambia y no queda ningún audio guardado.
- **Prueba manual:** No aplica: se prueba desde la vista en TASK-030.

### [x] TASK-011 Borrado de voces y voces elegibles para narrar

- **Cubre:** REQ-013, REQ-014, REQ-015, REQ-016, REQ-019, REQ-020, DEC-001, DEC-005, ALT-002, ALT-010
- **Componente:** voice-library-service | api/src/main/java/es/vargontoc/storyteller/application/service/VoiceLibraryService.java (amplía TASK-010; puerto `application/ports/in/generator/VoiceLibraryUseCase.java`, motivos nuevos `SYSTEM_VOICE_NOT_DELETABLE` y `VOICE_NOT_FOUND` en `domain/enums/VoiceRejectionReason.java`, `VoiceRepository.findById`)
- **Depende de:** TASK-010
- **Alcance:** borrar una voz subida (su registro y su audio de referencia), rechazar el borrado de la voz del sistema y devolver las voces elegibles para narrar (la voz del sistema si está disponible y las voces subidas en español), con su audio de referencia.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-013, REQ-014 y REQ-020 en el servidor, y una voz borrada ya no es elegible.
- **Pruebas automáticas:**
  - Borrar una voz subida la retira del listado y elimina su audio de referencia.
  - Intentar borrar la voz del sistema se rechaza con aviso y la voz sigue en el listado.
  - Borrar una voz subida no modifica ninguna narración guardada (sin vínculo persistente).
  - Con una voz subida en inglés y otra en español, solo la española y la voz del sistema son elegibles.
  - Con la voz del sistema no disponible, no se incluye entre las elegibles (ALT-002).
  - Una voz borrada no está entre las elegibles.
  - Se obtiene el audio de referencia de una voz elegible, tanto subida como del sistema.
- **Prueba manual:** No aplica: se prueba desde la vista en TASK-030 y el panel en TASK-031.

### [x] TASK-012 Persistencia de la narración del cuento

- **Cubre:** REQ-015, REQ-026, REQ-027, REQ-028, REQ-029, REQ-033, DEC-003, DEC-005
- **Componente:** narration-repository | api/src/main/java/es/vargontoc/storyteller/application/ports/out/persistence/NarrationRepository.java (adaptador `infrastructure/adapters/out/persistence/NarrationRepositoryAdapter.java`, entidades `infrastructure/persistence/StoryNarrationJpaEntity.java` y `StoryNarrationAudioJpaEntity.java`, mapper `infrastructure/mappers/StoryNarrationMapper.java`, modelo `domain/model/StoryNarration.java`, migración `api/src/main/resources/db/changelog/migrations/027_create_story_narration.xml`)
- **Depende de:** -
- **Alcance:** guardar el estado de narración de cada cuento (en curso, terminada o con error), la copia del nombre de la voz usada y el audio de cada página interior, con publicación de audios y voz en una sola operación y borrado de todo ello.
- **Hecho cuando:** el estado, la voz y los audios se registran, se consultan y se borran juntos, sin audio de portada.
- **Pruebas automáticas:**
  - Marcar un cuento como narrándose y consultarlo devuelve el estado en curso.
  - Publicar los audios de 8 páginas interiores y la voz en una sola operación deja los 8 audios y la voz. Si la operación falla, no queda ningún audio ni voz.
  - No se puede registrar audio para la portada.
  - Borrar el audio de un cuento elimina todos sus audios de página y la voz guardada.
  - Marcar la narración con error deja el cuento sin audio ni voz.
  - La voz guardada es una copia del nombre y se sigue consultando aunque la voz subida ya no exista.
- **Prueba manual:** No aplica: no tiene interfaz propia.

### [x] TASK-013 Modelfile y provisionador del narrador

- **Cubre:** REQ-023, REQ-024, DEC-002
- **Componente:** narrator-model-provisioner | api/src/main/java/es/vargontoc/storyteller/infrastructure/config/NarratorModelProvisioner.java (Modelfile `api/src/main/resources/prompts/narrator.modelfile`, propiedad `app.agents.audio-narrator`)
- **Depende de:** TASK-001
- **Alcance:** registrar en Ollama al arrancar, si no existe, el modelo del narrador con un Modelfile que se limita a dividir el texto aceptado en pocos fragmentos de frases completas y anotar su tono y énfasis, sin cambiar palabras ni atender al número de palabras. Se reutiliza el Modelfile `narrator` existente si TASK-001 lo decide así.
- **Hecho cuando:** al arrancar sin el modelo, este queda registrado con esas instrucciones. Si ya existe, no se vuelve a registrar.
- **Pruebas automáticas:**
  - Si el modelo del narrador no existe al arrancar, se registra.
  - Si ya existe, no se vuelve a registrar.
  - El Modelfile indica que no se añaden, quitan, reordenan ni sustituyen palabras y que se devuelven fragmentos con expresividad por página.
- **Prueba manual:** No aplica: el efecto se comprueba en la prueba manual de TASK-031.

### [x] TASK-014 Agente narrador

- **Cubre:** REQ-023, REQ-024, REQ-025, DEC-002, ALT-006
- **Componente:** narrator-agent | api/src/main/java/es/vargontoc/storyteller/infrastructure/adapters/out/external/SpringAiNarratorAgent.java (puerto `application/ports/out/external/NarratorAgent.java`, modelos `domain/model/PageAudioScript.java` y `AudioFragment.java`)
- **Depende de:** TASK-013
- **Alcance:** enviar a Ollama el texto aceptado de todas las páginas interiores, en orden, y devolver un guion de audio por página con sus fragmentos y la expresividad de cada uno (exageración, cfg y temperatura). Se reutiliza el agente narrador existente si TASK-001 lo decide así.
- **Hecho cuando:** ante una respuesta válida, el agente devuelve un guion por cada página interior con fragmentos y parámetros, y ante una respuesta no utilizable o un fallo indica un error.
- **Pruebas automáticas:**
  - Con 8 páginas interiores, la petición incluye los 8 textos en orden y no incluye la portada.
  - Una respuesta válida se convierte en 8 guiones, cada uno con sus fragmentos y parámetros de expresividad.
  - Una respuesta mal formada, sin alguna página o sin parámetros se indica como error (ALT-006).
  - Si Ollama no responde o falla, se indica como error (ALT-006).
- **Prueba manual:** No aplica: se comprueba en la prueba manual de TASK-031.

### [x] TASK-015 Validador del guion de audio

- **Cubre:** REQ-024, DEC-002, ALT-006
- **Componente:** audio-script-validator | api/src/main/java/es/vargontoc/storyteller/domain/service/AudioScriptValidator.java
- **Depende de:** -
- **Alcance:** comprobar de forma determinista, página a página, que los fragmentos del guion juntos reproducen exactamente las palabras del texto aceptado en el mismo orden, sin tener en cuenta la puntuación ni las mayúsculas.
- **Hecho cuando:** el validador acepta los guiones que conservan las palabras e indica qué página no las conserva.
- **Pruebas automáticas:**
  - Fragmentos que reproducen el texto exacto: válido.
  - Fragmentos con distinta puntuación o mayúsculas: válido.
  - Página con la onomatopeya conservada («¡Guau!»): válido. Sin la onomatopeya: no válido.
  - Una palabra añadida: no válido.
  - Una palabra quitada: no válido.
  - Dos palabras reordenadas: no válido.
  - Una palabra sustituida: no válido.
  - Si falla una página de 8, se indica cuál.
- **Prueba manual:** No aplica: el comportamiento es determinista y queda cubierto por las pruebas automáticas.

### [x] TASK-016 Adaptador de síntesis de Chatterbox

- **Cubre:** REQ-023, REQ-026, DEC-002, ALT-006
- **Componente:** speech-synthesis-adapter | api/src/main/java/es/vargontoc/storyteller/infrastructure/adapters/out/external/ChatterboxSpeechSynthesisAdapter.java (puerto `application/ports/out/external/SpeechSynthesizer.java`, petición `domain/request/SpeechSynthesisRequest.java`)
- **Depende de:** TASK-001
- **Alcance:** pedir a Chatterbox Multilingual V3 el audio de un fragmento con idioma español, el audio de referencia de la voz y los parámetros de expresividad. Se reutiliza el adaptador existente si TASK-001 lo decide así, sin cambiar los cuentos antiguos.
- **Hecho cuando:** el adaptador envía texto, idioma español, audio de referencia y parámetros, devuelve el audio sintetizado e indica un error si el motor falla.
- **Pruebas automáticas:**
  - La petición al motor lleva el texto del fragmento, el idioma español, el audio de referencia y los parámetros de expresividad recibidos.
  - Una respuesta correcta devuelve el audio.
  - Un error del motor, un tiempo agotado o una respuesta vacía se indican como error (ALT-006).
- **Prueba manual:**
  1. Con el contenedor de Chatterbox en marcha, sintetizar un fragmento corto con una voz de prueba y escuchar que el audio está en español y con esa voz.

### [x] TASK-017 Notificador de eventos de narración

- **Cubre:** REQ-027, REQ-028, REQ-029, DEC-004
- **Componente:** narration-notifier | api/src/main/java/es/vargontoc/storyteller/application/service/NarrationNotifier.java (puerto `application/ports/out/NarrationEventPublisher.java`, adaptador `infrastructure/adapters/out/WebsocketNarrationEventPublisher.java`, evento `domain/events/NarrationEvent.java`, tipo `domain/enums/NarrationEventType.java`)
- **Depende de:** -
- **Alcance:** publicar los eventos de narración de un cuento: inicio, éxito con la voz usada y error.
- **Hecho cuando:** cada evento se publica con el identificador del cuento y su tipo, y el de éxito lleva además el nombre de la voz.
- **Pruebas automáticas:**
  - El evento de inicio se publica con el cuento.
  - El evento de éxito se publica con el cuento y el nombre de la voz.
  - El evento de error se publica con el cuento.
- **Prueba manual:** No aplica: se comprueba en la integración de TASK-018.

### [x] TASK-018 Integración: notificador de narración con websocket-adapter

- **Cubre:** REQ-027, REQ-028, DEC-004
- **Componente:** narration-notifier | api/src/main/java/es/vargontoc/storyteller/application/service/NarrationNotifier.java y websocket-adapter | api/src/main/java/es/vargontoc/storyteller/infrastructure/adapters/in/websocket/WebsocketAdapter.java
- **Depende de:** TASK-017
- **Alcance:** difundir por el canal `/storyteller` los eventos de narración, junto con los actuales de retratos y páginas.
- **Hecho cuando:** un cliente suscrito a `/storyteller` recibe los eventos de inicio, éxito y error de narración, y los demás eventos siguen llegando igual.
- **Pruebas automáticas:**
  - Un cliente suscrito recibe el evento de inicio, el de éxito (con la voz) y el de error de una narración.
  - Los eventos de retratos y de páginas se siguen difundiendo sin cambios.
- **Prueba manual:** No aplica: se comprueba en la prueba manual de TASK-031.

### [x] TASK-019 Estado de narración del cuento y borrado del audio a petición

- **Cubre:** REQ-017, REQ-018, REQ-027, REQ-029, REQ-033, REQ-034, DEC-007
- **Componente:** story-narration-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryNarrationService.java (puerto `application/ports/in/generator/StoryNarrationUseCase.java`, respuesta `domain/response/NarrationState.java`, motivo `domain/enums/NarrationDisabledReason.java`)
- **Depende de:** TASK-012
- **Alcance:** consultar el estado de narración de un cuento (si tiene escaleta, si narrar está habilitado y por qué no, si está narrándose, si tiene audio y con qué voz), calculado siempre a partir del estado actual de las páginas interiores, y ofrecer a `story-page-service` las consultas de narración en curso o audio y el borrado del audio y la voz.
- **Hecho cuando:** se cumplen en el servidor los criterios de aceptación de REQ-018 y REQ-034, y el borrado a petición deja el cuento sin audio ni voz.
- **Pruebas automáticas:**
  - Un cuento sin escaleta no ofrece narración.
  - Un cuento con escaleta y 7 de 8 páginas interiores aceptadas: narrar está deshabilitado con el motivo de que faltan páginas por aceptar.
  - Con las 8 aceptadas y la portada pendiente: narrar está habilitado.
  - Con una narración en curso: se indica en curso y narrar está deshabilitado.
  - Con audio: se indica la voz con la que se narró.
  - Tras pedir el borrado del audio, el cuento no tiene audio ni voz, y narrar sigue deshabilitado mientras falten páginas por aceptar y vuelve a habilitarse cuando todas lo están.
- **Prueba manual:** No aplica: se prueba desde el panel en TASK-031.

### [x] TASK-020 Inicio de la narración y la renarración

- **Cubre:** REQ-016, REQ-017, REQ-018, REQ-020, REQ-021, REQ-022, REQ-027, REQ-031, REQ-032, DEC-003, DEC-004, ALT-003, ALT-004, ALT-005
- **Componente:** story-narration-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryNarrationService.java; nuevos: NarrationExecutor (api/src/main/java/es/vargontoc/storyteller/application/ports/out/NarrationExecutor.java), PendingNarrationExecutor (api/src/main/java/es/vargontoc/storyteller/infrastructure/adapters/out/PendingNarrationExecutor.java, provisional hasta TASK-021), NarrationRejectedException (api/src/main/java/es/vargontoc/storyteller/shared/exceptions/NarrationRejectedException.java), NarrationRejectionReason (api/src/main/java/es/vargontoc/storyteller/domain/enums/NarrationRejectionReason.java)
- **Depende de:** TASK-011, TASK-012, TASK-017, TASK-019
- **Alcance:** al confirmar, comprobar escaleta, páginas interiores aceptadas, que no se está narrando y que la voz es elegible. Después, pedir el turno a `generation-gate`, borrar el audio y la voz anteriores, marcar el cuento como narrándose, tomar el audio de referencia de la voz, notificar el inicio y responder que la narración ha empezado.
- **Hecho cuando:** se cumplen en el servidor los criterios de aceptación de REQ-021 (primer criterio) y REQ-022, y cada rechazo deja el audio sin cambios.
- **Pruebas automáticas:**
  - Con condiciones cumplidas y turno libre: se concede el turno, se borra el audio previo, el cuento queda narrándose, se notifica el inicio y se responde que la narración ha empezado.
  - Con la narración en curso, otra generación (por ejemplo, de retratos) se rechaza con el aviso habitual de generación en curso.
  - Renarrar con la misma voz o con otra voz elegible se acepta igual que narrar.
  - Con otra generación en curso: se rechaza con el aviso de generación en curso, sin tocar el audio ni la voz guardada (ALT-004).
  - Sin voces elegibles: se rechaza con el aviso de que no hay voces en español disponibles y el audio previo no cambia (ALT-003).
  - Cuento sin escaleta, con páginas interiores sin aceptar o ya narrándose: se rechaza con el motivo y no se pide el turno (ALT-005).
  - Voz borrada o en un idioma que no es el español: se rechaza con el aviso de voz no disponible y no se pide el turno (ALT-005).
- **Prueba manual:** No aplica: se prueba desde el panel en TASK-031.

### [x] TASK-021 Ejecución de la narración en segundo plano

- **Cubre:** REQ-021, REQ-023, REQ-024, REQ-025, REQ-026, REQ-028, REQ-029, REQ-032, DEC-002, DEC-003, DEC-004, ALT-006, ALT-010
- **Componente:** story-narration-service | api/src/main/java/es/vargontoc/storyteller/application/service/BackgroundNarrationExecutor.java (implementa `NarrationExecutor`; sustituye y borra `PendingNarrationExecutor`)
- **Depende de:** TASK-014, TASK-015, TASK-016, TASK-020
- **Alcance:** en segundo plano, enviar al narrador el texto aceptado de todas las páginas interiores, validar el guion, sintetizar cada página por fragmentos en español con la voz elegida y unirlos en un audio por página. Solo cuando todas las páginas tienen audio, publicar los audios y la voz en una sola operación, descartar el guion, notificar el éxito y liberar el turno. Ante un fallo, dejar el cuento sin audio, notificar el error y liberar el turno.
- **Hecho cuando:** se cumplen en el servidor los criterios de aceptación de REQ-024 (segundo criterio), REQ-026, REQ-028 y REQ-029, el segundo criterio de REQ-021 y el segundo de REQ-032, y el texto aceptado de las páginas no cambia.
- **Pruebas automáticas:**
  - Con 8 páginas interiores y todo correcto: hay 8 audios, ninguno de portada, la voz queda guardada, se notifica el éxito con la voz y el turno queda libre.
  - Se envían al motor todos los fragmentos de cada página, con idioma español, el audio de referencia elegido y la expresividad del guion.
  - Mientras dura la síntesis, el cuento no muestra audio parcial.
  - El guion de audio no se persiste ni aparece en el estado de narración.
  - El texto aceptado de las páginas es idéntico antes y después de narrar.
  - Si el narrador falla o responde un guion no utilizable: termina con error, sin audio, con aviso de error y con el turno libre (ALT-006).
  - Si el validador detecta palabras alteradas en una página: termina con error sin sintetizar (ALT-006).
  - Si la síntesis falla en la página 4 de 8: no queda audio en ninguna página, se elimina el audio parcial, se notifica el error, el turno queda libre y se puede volver a narrar (ALT-006).
  - Una renarración que falla deja el cuento sin audio ni voz, no con el audio anterior (SUP-003).
  - Si la voz subida se borra durante la narración, la narración sigue con el audio de referencia que tomó al empezar y guarda su nombre (ALT-010).
  - No hay reintento automático tras un error.
- **Prueba manual:** No aplica: se prueba de extremo a extremo en TASK-031.

### [x] TASK-022 Descarga del audio de una página interior

- **Cubre:** REQ-015, REQ-030
- **Componente:** story-narration-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryNarrationService.java (operación `getPageAudio` en el puerto `application/ports/in/generator/StoryNarrationUseCase.java`)
- **Depende de:** TASK-012
- **Alcance:** devolver el audio narrado de una página interior concreta de un cuento con audio.
- **Hecho cuando:** se obtiene solo el audio de la página pedida, y una página sin audio o la portada dan una respuesta de no encontrado.
- **Pruebas automáticas:**
  - En un cuento narrado, pedir la página 2 devuelve solo su audio.
  - Pedir la portada o una página de un cuento sin audio responde que no existe audio.
  - Tras borrar la voz subida con la que se narró, el audio de cada página se sigue obteniendo.
- **Prueba manual:** No aplica: se prueba desde el reproductor en TASK-033.

### [x] TASK-023 Reconciliación de narraciones al arrancar

- **Cubre:** REQ-021, REQ-027, REQ-028, DEC-004, ALT-007
- **Componente:** narration-reconciliation-service | api/src/main/java/es/vargontoc/storyteller/application/service/NarrationReconciliationService.java (disparo `infrastructure/config/NarrationReconciliationRunner.java`)
- **Depende de:** TASK-012, TASK-017
- **Alcance:** al arrancar, tratar como fallidas las narraciones que quedaron en curso, eliminar su audio parcial y notificar el error.
- **Hecho cuando:** tras un arranque con una narración en curso, el cuento queda sin audio, con error y con narrar habilitado, y el turno está libre.
- **Pruebas automáticas:**
  - Con una narración en curso persistida, al arrancar pasa a error, sin audio ni voz, y se notifica el error.
  - Las narraciones terminadas no cambian al arrancar.
  - Tras la reconciliación, una nueva generación obtiene el turno.
- **Prueba manual:**
  1. Iniciar la narración de un cuento y reiniciar el servidor antes de que termine.
  2. Abrir el cuento: no hay indicador de narración en curso, se ve el aviso de error, ninguna página tiene audio y narrar está habilitado.

### [x] TASK-024 Bloqueo de páginas durante la narración y borrado del audio al reabrir

- **Cubre:** REQ-027, REQ-033, REQ-034, DEC-007, ALT-008, ALT-009
- **Componente:** story-page-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageService.java
- **Depende de:** TASK-019
- **Alcance:** rechazar reabrir, cambiar o eliminar páginas interiores mientras el cuento se narra. Al reabrir, cambiar o eliminar páginas interiores (también las que invalida `beat-editor`) de un cuento con audio, pedir el borrado del audio y la voz en la misma operación.
- **Hecho cuando:** se cumplen en el servidor los criterios de aceptación de REQ-033 (segundo y tercer criterio) y REQ-034, y las operaciones rechazadas no cambian ni las páginas ni la narración.
- **Pruebas automáticas:**
  - Reabrir o cambiar una página interior de un cuento que se está narrando: se rechaza con aviso de narración en curso y nada cambia (ALT-008).
  - Eliminar páginas invalidadas de un cuento que se está narrando: se rechaza con el mismo aviso (ALT-008).
  - Reabrir la página 5 de un cuento con audio: se reabre con el descarte en cascada habitual, se borran todos los audios y la voz, y narrar queda deshabilitado (ALT-009).
  - Eliminar páginas invalidadas de un cuento con audio: se borran también todos los audios y la voz (ALT-009).
  - Si falla la operación de páginas, el audio no se borra.
  - Reabrir una página de un cuento sin audio se comporta como hasta ahora.
- **Prueba manual:** No aplica: se prueba desde la vista en TASK-034.

### [x] TASK-025 Aviso de borrado de audio al invalidar páginas por un beat

- **Cubre:** REQ-033, DEC-007, ALT-008, ALT-009
- **Componente:** beat-editor | api/src/main/java/es/vargontoc/storyteller/application/service/BeatEditorService.java
- **Depende de:** TASK-024
- **Alcance:** el aviso de invalidación de páginas al editar un beat indica también que se borrará el audio cuando el cuento lo tenga, y la edición se rechaza mientras el cuento se narra.
- **Hecho cuando:** el aviso de invalidación incluye el borrado del audio solo en cuentos con audio y, al confirmar, el audio queda borrado.
- **Pruebas automáticas:**
  - Editar un beat que invalida páginas en un cuento con audio: el aviso indica que se borrará el audio.
  - El mismo caso en un cuento sin audio: el aviso es el de siempre.
  - Confirmar la edición en un cuento con audio deja el cuento sin audio ni voz.
  - Editar un beat que invalida páginas mientras el cuento se narra se rechaza con aviso de narración en curso (ALT-008).
- **Prueba manual:** No aplica: se prueba desde el panel del guion en TASK-035.

### [x] TASK-026 Integración: vista de páginas con audio y narración en curso

- **Cubre:** REQ-027, REQ-030, REQ-033
- **Componente:** story-pages-api | app/src/api/storyPages.ts y story-page-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageService.java
- **Depende de:** TASK-019, TASK-024
- **Alcance:** la vista de páginas y el detalle de página que recibe el cliente indican si el cuento tiene audio, si se está narrando y si cada página interior tiene audio, y el cliente recibe el rechazo por narración en curso.
- **Hecho cuando:** el cliente obtiene esos tres datos en la vista y el detalle de página, y recibe el rechazo de narración en curso al reabrir o cambiar.
- **Pruebas automáticas:**
  - En un cuento narrado, la vista indica que tiene audio y el detalle de cada página interior indica que tiene audio. La portada no.
  - En un cuento narrándose, la vista indica que está en narración.
  - El rechazo por narración en curso llega al cliente con su aviso.
- **Prueba manual:** No aplica: se prueba desde la vista en TASK-033 y TASK-034.

### [x] TASK-027 Integración: cliente de la biblioteca de voces con el servidor

- **Cubre:** REQ-006, REQ-007, REQ-008, REQ-009, REQ-010, REQ-011, REQ-013, REQ-014, REQ-019, REQ-020, DEC-001, ALT-001, ALT-002
- **Componente:** voice-library-api | app/src/api/voices.ts y voice-library-service | por definir (analista técnico)
- **Depende de:** TASK-010, TASK-011
- **Alcance:** el cliente REST lista la biblioteca y los idiomas, sube una voz con su audio, idioma, nombre y declaración, borra una voz subida y consulta las voces elegibles, y recibe los rechazos con su motivo.
- **Hecho cuando:** cada operación del cliente llega al servicio y devuelve su resultado o el aviso de rechazo correspondiente.
- **Pruebas automáticas:**
  - El listado devuelve las voces con nombre, idioma, marca de voz del sistema y disponibilidad de la voz del sistema.
  - El catálogo de idiomas devuelve los idiomas con el español como valor por defecto.
  - Una subida válida devuelve la voz creada.
  - Cada rechazo de ALT-001 llega al cliente con su motivo.
  - El borrado de la voz del sistema llega como rechazo con aviso.
  - Las voces elegibles excluyen las borradas y las que no están en español.
- **Prueba manual:** No aplica: se prueba desde la vista en TASK-030.

### [x] TASK-028 Integración: cliente de narración con el servidor

- **Cubre:** REQ-018, REQ-021, REQ-022, REQ-026, REQ-029, REQ-030, REQ-031, ALT-003, ALT-004, ALT-005
- **Componente:** story-narration-api | api/src/main/java/es/vargontoc/storyteller/infrastructure/adapters/in/rest/web/StoryNarrationController.java y story-narration-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryNarrationService.java
- **Depende de:** TASK-019, TASK-020, TASK-021, TASK-022
- **Alcance:** el cliente REST inicia la narración o la renarración con una voz, consulta el estado de narración de un cuento y descarga el audio de una página interior, y recibe los rechazos con su motivo.
- **Hecho cuando:** cada operación del cliente llega al servicio y devuelve su resultado o el aviso de rechazo correspondiente.
- **Pruebas automáticas:**
  - Iniciar con una voz elegible devuelve que la narración ha empezado.
  - Los rechazos de ALT-003, ALT-004 y ALT-005 llegan con su motivo.
  - El estado devuelve si narrar está habilitado, el motivo si no lo está, si está en curso, si hay audio, la voz usada y el resultado del último intento.
  - La descarga del audio de una página devuelve ese audio, y la de una página sin audio responde que no existe.
- **Prueba manual:** No aplica: se prueba desde el panel en TASK-031.

### [x] TASK-029 Eventos de narración en el store de WebSocket

- **Cubre:** REQ-027, REQ-028, DEC-004
- **Componente:** websocket-store | app/src/stores/websocket.ts
- **Depende de:** TASK-018, TASK-023
- **Alcance:** recibir y difundir los eventos de narración (inicio, éxito y error) y avisar de que hay que recargar el estado al reconectar.
- **Hecho cuando:** los suscriptores reciben los eventos de narración y el aviso de recarga tras una reconexión.
- **Pruebas automáticas:**
  - Un evento de inicio, éxito o error de narración se difunde a los suscriptores con su cuento.
  - Tras reconectar, los suscriptores reciben el aviso para recargar el estado.
  - Los eventos de retratos y de páginas se siguen difundiendo igual.
- **Prueba manual:** No aplica: se prueba desde el panel en TASK-031.

### [x] TASK-030 Vista de la biblioteca de voces

- **Cubre:** REQ-006, REQ-007, REQ-008, REQ-009, REQ-010, REQ-011, REQ-012, REQ-013, REQ-014, ALT-001, ALT-002, ALT-010
- **Componente:** voice-library-view | app/src/views/VoiceLibraryView.vue
- **Depende de:** TASK-027
- **Alcance:** mostrar la biblioteca con nombre, idioma y marca de voz del sistema, el aviso de voz del sistema no disponible, el formulario de subida (nombre, idioma con español preseleccionado, fichero y casilla de permiso) con sus avisos de rechazo, y el borrado de las voces subidas, sin opción de borrado para la voz del sistema.
- **Hecho cuando:** se cumplen en la vista los criterios de aceptación de REQ-006 a REQ-014.
- **Pruebas automáticas:**
  - El formulario recién abierto muestra el español seleccionado.
  - Se muestra cada voz con su nombre y su idioma, y la del sistema identificada como tal.
  - La voz del sistema no muestra la opción de borrar y las subidas sí.
  - Con la voz del sistema no disponible, se muestra el aviso (ALT-002).
  - Un rechazo de subida muestra el aviso del motivo y la lista no cambia (ALT-001).
  - Tras borrar una voz con confirmación, deja de aparecer en la lista.
- **Prueba manual:**
  1. Abrir la biblioteca de voces: aparece la voz del sistema con el nombre configurado, identificada como tal y sin opción de borrar.
  2. Abrir el formulario de subida: el idioma muestra español.
  3. Subir un MP3 de 10 segundos con nombre «Abuela», idioma español y la casilla de permiso marcada: aparece en la lista con su idioma.
  4. Intentar subir otra voz llamada «Abuela»: aviso de nombre duplicado.
  5. Intentar subir un OGG, un audio de 4 segundos, uno de 31 segundos, uno sin la casilla de permiso y uno sin nombre: cada intento muestra su aviso y la lista no cambia.
  6. Subir un WAV de exactamente 5 segundos y otro de 30 segundos: se aceptan.
  7. Borrar «Abuela»: desaparece de la lista.
  8. Quitar la voz del sistema de la configuración y reiniciar: la biblioteca muestra el aviso de voz del sistema no disponible.

### [x] TASK-031 Panel de narración del cuento

- **Cubre:** REQ-016, REQ-018, REQ-019, REQ-020, REQ-021, REQ-022, REQ-023, REQ-024, REQ-025, REQ-026, REQ-027, REQ-028, REQ-029, REQ-031, REQ-032, REQ-034, ALT-002, ALT-003, ALT-004, ALT-005, ALT-006
- **Componente:** story-narration-panel | app/src/components/StoryNarrationPanel.vue
- **Depende de:** TASK-027, TASK-028, TASK-029
- **Alcance:** mostrar la acción de narrar o renarrar (habilitada o no, con su motivo), la elección de voz entre las elegibles con la voz del sistema preseleccionada si está disponible, el aviso de sustitución con confirmación al renarrar, el indicador de narración en curso, la voz con la que se narró y el resultado. Recarga el estado ante los eventos de narración o la reconexión.
- **Hecho cuando:** se cumplen en el panel los criterios de aceptación de REQ-016, REQ-018 a REQ-020, REQ-022 y REQ-027 a REQ-032, y el guion de audio no se muestra nunca.
- **Pruebas automáticas:**
  - Con páginas interiores sin aceptar, narrar está deshabilitado con la indicación de que faltan páginas.
  - Al pulsar narrar, se muestran solo las voces elegibles con la voz del sistema seleccionada.
  - Con la voz del sistema no disponible, no hay voz preseleccionada y se elige entre las voces subidas en español (ALT-002).
  - Al renarrar se muestra el aviso de sustitución. Si se cancela, no se inicia nada.
  - Durante la narración se ve el indicador y narrar y renarrar están deshabilitados.
  - Tras el evento de éxito se muestra «narrado con <voz>». Tras el de error, el aviso de error y narrar habilitado.
  - Un rechazo (generación en curso, sin voces en español, voz no disponible o faltan páginas) muestra su aviso (ALT-003, ALT-004, ALT-005).
  - El guion de audio no aparece en el panel.
- **Prueba manual:**
  1. Abrir un cuento con escaleta con una página interior sin aceptar: narrar está deshabilitado e indica que faltan páginas por aceptar.
  2. Aceptar todas las páginas interiores (con la portada pendiente): narrar se habilita.
  3. Pulsar narrar: se ven la voz del sistema seleccionada y las voces subidas en español, pero no las de otros idiomas ni las borradas.
  4. Confirmar con la voz del sistema: aparece el indicador de narración en curso. Mientras tanto, pedir retratos de un personaje: aviso de generación en curso.
  5. Al terminar: se ve «narrado con <voz del sistema>», sin el guion de audio, y el texto de las páginas no ha cambiado.
  6. Escuchar dos páginas de tono distinto: el audio está en español, con la voz elegida, dice exactamente el texto (incluida la onomatopeya) y la expresividad no es idéntica.
  7. Pedir renarrar, cancelar el aviso: el audio y la voz siguen igual. Repetir, confirmar con «Abuela»: al terminar, el cuento indica «Abuela».
  8. Con una generación de retratos en curso, confirmar la narración: aviso de generación en curso y el audio previo sigue.
  9. Detener el contenedor de Chatterbox y narrar: aviso de error, ninguna página con audio y narrar habilitado.

### [x] TASK-032 Panel de narración solo en cuentos con escaleta

- **Cubre:** REQ-017
- **Componente:** story-modal | app/src/components/StoryModal.vue
- **Depende de:** TASK-031
- **Alcance:** integrar el panel de narración solo en los cuentos con escaleta, sin cambiar la vista de los cuentos antiguos.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-017.
- **Pruebas automáticas:**
  - Un cuento con escaleta muestra el panel de narración.
  - Un cuento antiguo sin escaleta no lo muestra y conserva su vista actual.
- **Prueba manual:**
  1. Abrir un cuento antiguo: no aparece la narración de esta funcionalidad y su vista no cambia.
  2. Abrir un cuento con escaleta: aparece el panel de narración.

### [x] TASK-033 Reproductor del audio de la página que se ve

- **Cubre:** REQ-015, REQ-030
- **Componente:** story-pages-panel | app/src/components/StoryPagesPanel.vue
- **Depende de:** TASK-026, TASK-028, TASK-029
- **Alcance:** en las páginas interiores con audio, ofrecer reproducir solo el audio de la página que se está viendo, sin pasar a la siguiente al terminar, y ocultarlo en la portada y en los cuentos sin audio.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-030.
- **Pruebas automáticas:**
  - En una página interior con audio se muestra el reproductor y carga el audio de esa página.
  - Al terminar el audio no se reproduce el de la página siguiente.
  - En la portada y en los cuentos sin audio no se muestra el reproductor.
  - Tras un evento de éxito de narración, el reproductor aparece sin recargar a mano.
- **Prueba manual:**
  1. En un cuento narrado, ir a la página 2 y pulsar reproducir: suena solo la página 2 y, al terminar, no empieza la 3.
  2. Ir a la portada: no hay reproductor.
  3. Borrar en la biblioteca la voz con la que se narró y volver al cuento: el audio de cada página se sigue reproduciendo y el cuento sigue indicando el nombre de la voz.

### [ ] TASK-034 Aviso de borrado de audio y bloqueo de páginas durante la narración

- **Cubre:** REQ-027, REQ-033, REQ-034, ALT-008, ALT-009
- **Componente:** story-pages-panel | app/src/components/StoryPagesPanel.vue
- **Depende de:** TASK-026, TASK-029, TASK-033
- **Alcance:** al reabrir o cambiar una página interior de un cuento con audio, añadir al aviso de descarte en cascada que se borrará todo el audio, y bloquear esos cambios mientras el cuento se narra.
- **Hecho cuando:** se cumplen en la vista los criterios de aceptación de REQ-033 y REQ-034.
- **Pruebas automáticas:**
  - Con audio, el aviso al reabrir indica el descarte en cascada y el borrado de todo el audio.
  - Sin audio, el aviso es el de siempre.
  - Al cancelar, no se envía ninguna operación.
  - Mientras el cuento se narra, reabrir y cambiar están bloqueados o muestran el aviso de narración en curso (ALT-008).
- **Prueba manual:**
  1. En un cuento narrado, pedir reabrir la página 5: el aviso indica el descarte en cascada y el borrado de todo el audio. Cancelar: la página no se reabre y el audio sigue.
  2. Repetir y confirmar: ninguna página tiene audio, el cuento ya no indica voz y narrar está deshabilitado.
  3. Volver a aceptar las páginas 5 a 8: narrar vuelve a estar habilitado.
  4. Durante una narración, intentar reabrir una página: se impide con el aviso de narración en curso.

### [ ] TASK-035 Aviso de borrado de audio al editar un beat

- **Cubre:** REQ-033, ALT-008, ALT-009
- **Componente:** story-script-panel | app/src/components/StoryScriptPanel.vue
- **Depende de:** TASK-025
- **Alcance:** el aviso de invalidación de páginas al editar un beat indica también el borrado del audio cuando el cuento lo tiene, y se muestra el rechazo por narración en curso.
- **Hecho cuando:** el aviso muestra el borrado del audio solo en cuentos con audio y, al confirmar, el cuento queda sin audio.
- **Pruebas automáticas:**
  - Con audio, el aviso de invalidación menciona el borrado del audio.
  - Sin audio, el aviso es el de siempre.
  - El rechazo por narración en curso muestra su aviso (ALT-008).
- **Prueba manual:**
  1. En un cuento narrado, editar un beat que invalida páginas: el aviso indica que se borrará el audio. Confirmar: el cuento queda sin audio ni voz.

### [ ] TASK-036 Contrato: openapi - operaciones de la biblioteca de voces

- **Cubre:** REQ-006, REQ-007, REQ-008, REQ-009, REQ-010, REQ-011, REQ-013, REQ-014, REQ-020, DEC-001, DEC-005
- **Componente:** voice-library-service | por definir (analista técnico). Contrato en `docs/contracts/openapi/paths/voice/`, con el nombre del fichero por definir (analista técnico).
- **Depende de:** TASK-027
- **Alcance:** crear las operaciones de la biblioteca de voces: listado con la voz del sistema y su disponibilidad, catálogo de idiomas, subida con audio, idioma, nombre y declaración de permiso (con los rechazos de ALT-001), voces elegibles y borrado de una voz subida (con el rechazo para la voz del sistema).
- **Hecho cuando:** el contrato existe en `docs/contracts/openapi/paths/voice/` y describe las operaciones tal como quedaron implementadas.
- **Pruebas automáticas:**
  - El contrato es válido según OpenAPI.
  - Recoge las respuestas de rechazo de formato, duración, idioma, permiso, nombre obligatorio, nombre duplicado y borrado de la voz del sistema.
- **Prueba manual:**
  1. Comprobar que cada operación de la biblioteca aparece en el contrato y coincide con la implementación.

### [ ] TASK-037 Contrato: openapi - operaciones de narración

- **Cubre:** REQ-016, REQ-017, REQ-018, REQ-020, REQ-021, REQ-022, REQ-026, REQ-029, REQ-030, REQ-031, DEC-003, DEC-004
- **Componente:** story-narration-service | por definir (analista técnico). Contrato en `docs/contracts/openapi/paths/story/`, con el nombre del fichero por definir (analista técnico).
- **Depende de:** TASK-028
- **Alcance:** crear las operaciones para iniciar la narración o renarración con una voz (con los rechazos de ALT-003, ALT-004 y ALT-005), consultar el estado de narración y descargar el audio de una página interior.
- **Hecho cuando:** el contrato existe en `docs/contracts/openapi/paths/story/` y describe las operaciones tal como quedaron implementadas.
- **Pruebas automáticas:**
  - El contrato es válido según OpenAPI.
  - Recoge los rechazos de generación en curso, sin voces en español, voz no disponible, faltan páginas por aceptar, narración en curso y cuento sin escaleta, y la respuesta de audio inexistente.
- **Prueba manual:**
  1. Comprobar que cada operación de narración aparece en el contrato y coincide con la implementación.

### [ ] TASK-038 Contrato: openapi - esquemas de voz, biblioteca, idioma y narración

- **Cubre:** REQ-008, REQ-011, REQ-018, REQ-027, REQ-029, DEC-001, DEC-005
- **Componente:** voice-library-service | por definir (analista técnico) y story-narration-service | por definir (analista técnico). Contrato en `docs/contracts/openapi/schemas/`, con los nombres de los ficheros por definir (analista técnico).
- **Depende de:** TASK-027, TASK-028
- **Alcance:** crear los esquemas de voz, biblioteca de voces, idioma de voz y estado de narración (en curso, terminada, con error, sin audio, motivo de deshabilitado y voz usada).
- **Hecho cuando:** los esquemas existen en `docs/contracts/openapi/schemas/` y describen los datos tal como quedaron implementados.
- **Pruebas automáticas:**
  - Los esquemas son válidos según OpenAPI.
  - El estado de narración recoge todos sus valores y el motivo de deshabilitado.
- **Prueba manual:**
  1. Comprobar que cada campo devuelto por las operaciones de voz y narración aparece en los esquemas.

### [ ] TASK-039 Contrato: openapi - audio y narración en la vista de páginas

- **Cubre:** REQ-027, REQ-030, REQ-033
- **Componente:** story-page-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageService.java. Contrato en `docs/contracts/openapi/schemas/story-pages-view.yaml` y `docs/contracts/openapi/schemas/page-detail.yaml`.
- **Depende de:** TASK-026
- **Alcance:** añadir si el cuento tiene audio, si se está narrando y si cada página interior tiene audio.
- **Hecho cuando:** los dos esquemas recogen los nuevos datos tal como quedaron implementados.
- **Pruebas automáticas:**
  - Los esquemas son válidos según OpenAPI.
- **Prueba manual:**
  1. Comprobar que los tres datos nuevos aparecen en los esquemas y coinciden con la respuesta real.

### [ ] TASK-040 Contrato: openapi - borrado de audio y rechazo por narración en páginas y beats

- **Cubre:** REQ-027, REQ-033, REQ-034, DEC-007
- **Componente:** story-page-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageService.java. Contrato en `docs/contracts/openapi/paths/story/` (ficheros de reapertura y cambio de página, por definir (analista técnico)) y `docs/contracts/openapi/paths/story/beat-edit-service.yaml`.
- **Depende de:** TASK-024, TASK-025
- **Alcance:** documentar que se borra el audio del cuento al reabrir, cambiar o invalidar páginas interiores, y el nuevo rechazo por narración en curso.
- **Hecho cuando:** los contratos de reapertura, cambio de página y edición de beat describen el borrado del audio y el rechazo tal como quedaron implementados.
- **Pruebas automáticas:**
  - Los contratos son válidos según OpenAPI.
  - Recogen la respuesta de rechazo por narración en curso.
- **Prueba manual:**
  1. Comprobar que las operaciones de reapertura, cambio de página y edición de beat describen el borrado del audio y el rechazo, y que coinciden con la implementación.

### [ ] TASK-041 Contrato: openapi - registro de las operaciones de voces y narración

- **Cubre:** REQ-011, REQ-026, REQ-030
- **Componente:** voice-library-service | por definir (analista técnico) y story-narration-service | por definir (analista técnico). Contrato en `docs/contracts/openapi/openapi.yaml`.
- **Depende de:** TASK-036, TASK-037, TASK-038
- **Alcance:** registrar en el contrato principal las operaciones nuevas de la biblioteca de voces y de la narración.
- **Hecho cuando:** `docs/contracts/openapi/openapi.yaml` referencia todas las operaciones y esquemas nuevos.
- **Pruebas automáticas:**
  - El contrato completo, con sus referencias, es válido según OpenAPI.
- **Prueba manual:**
  1. Comprobar que cada operación nueva de voces y de narración aparece registrada en `openapi.yaml`.

### [ ] TASK-042 Contrato: websocket - eventos de narración

- **Cubre:** REQ-027, REQ-028, REQ-029, DEC-004
- **Componente:** websocket-adapter | api/src/main/java/es/vargontoc/storyteller/infrastructure/adapters/in/websocket/WebsocketAdapter.java. Contrato en `docs/contracts/websocket/websocket.md`.
- **Depende de:** TASK-018, TASK-023
- **Alcance:** añadir los eventos de narración de un cuento: inicio, éxito con la voz usada y error (también el que se emite al reconciliar al arrancar).
- **Hecho cuando:** `docs/contracts/websocket/websocket.md` describe los tres eventos tal como quedaron implementados.
- **Pruebas automáticas:** No aplica: el contrato es un documento Markdown sin formato validable.
- **Prueba manual:**
  1. Comprobar que los tres eventos aparecen en el contrato con sus datos y coinciden con los que emite el servidor.

## Cobertura

| Requisito | Tareas |
|---|---|
| REQ-001 | TASK-002, TASK-003 |
| REQ-002 | TASK-002, TASK-003 |
| REQ-003 | TASK-002, TASK-003 |
| REQ-004 | TASK-002, TASK-003 |
| REQ-005 | TASK-003, TASK-004 |
| REQ-006 | TASK-006, TASK-010, TASK-027, TASK-030, TASK-036 |
| REQ-007 | TASK-006, TASK-010, TASK-027, TASK-030, TASK-036 |
| REQ-008 | TASK-005, TASK-010, TASK-027, TASK-030, TASK-036, TASK-038 |
| REQ-009 | TASK-007, TASK-010, TASK-027, TASK-030, TASK-036 |
| REQ-010 | TASK-007, TASK-009, TASK-010, TASK-027, TASK-030, TASK-036 |
| REQ-011 | TASK-007, TASK-009, TASK-010, TASK-027, TASK-030, TASK-036, TASK-038, TASK-041 |
| REQ-012 | TASK-009, TASK-010, TASK-030 |
| REQ-013 | TASK-011, TASK-027, TASK-030, TASK-036 |
| REQ-014 | TASK-007, TASK-008, TASK-011, TASK-027, TASK-030, TASK-036 |
| REQ-015 | TASK-011, TASK-012, TASK-022, TASK-033 |
| REQ-016 | TASK-011, TASK-020, TASK-031, TASK-037 |
| REQ-017 | TASK-001, TASK-019, TASK-020, TASK-032, TASK-037 |
| REQ-018 | TASK-019, TASK-020, TASK-028, TASK-031, TASK-037, TASK-038 |
| REQ-019 | TASK-009, TASK-011, TASK-027, TASK-031 |
| REQ-020 | TASK-005, TASK-011, TASK-020, TASK-027, TASK-031, TASK-036, TASK-037 |
| REQ-021 | TASK-020, TASK-021, TASK-023, TASK-028, TASK-031, TASK-037 |
| REQ-022 | TASK-020, TASK-028, TASK-031, TASK-037 |
| REQ-023 | TASK-001, TASK-013, TASK-014, TASK-016, TASK-021, TASK-031 |
| REQ-024 | TASK-013, TASK-014, TASK-015, TASK-021, TASK-031 |
| REQ-025 | TASK-014, TASK-021, TASK-031 |
| REQ-026 | TASK-001, TASK-012, TASK-016, TASK-021, TASK-028, TASK-031, TASK-037, TASK-041 |
| REQ-027 | TASK-012, TASK-017, TASK-018, TASK-019, TASK-020, TASK-023, TASK-024, TASK-026, TASK-029, TASK-031, TASK-034, TASK-038, TASK-039, TASK-040, TASK-042 |
| REQ-028 | TASK-012, TASK-017, TASK-018, TASK-021, TASK-023, TASK-029, TASK-031, TASK-042 |
| REQ-029 | TASK-012, TASK-017, TASK-019, TASK-021, TASK-028, TASK-031, TASK-037, TASK-038, TASK-042 |
| REQ-030 | TASK-022, TASK-026, TASK-028, TASK-033, TASK-037, TASK-039, TASK-041 |
| REQ-031 | TASK-020, TASK-028, TASK-031, TASK-037 |
| REQ-032 | TASK-020, TASK-021, TASK-031 |
| REQ-033 | TASK-012, TASK-019, TASK-024, TASK-025, TASK-026, TASK-034, TASK-035, TASK-039, TASK-040 |
| REQ-034 | TASK-019, TASK-024, TASK-031, TASK-034, TASK-040 |
