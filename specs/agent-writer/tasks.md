# Tareas: agent-writer

Estado: aprobado
Revisión: 1
Basado en: requirements@5, design@2

## Resumen

El trabajo empieza por las piezas base del servidor: la puerta de generación, el validador de longitud, el histórico de versiones, el modelo de página, el escritor y su Modelfile, y el notificador. Después vienen los casos de uso de `story-page-service`, uno por tarea, y la incorporación de la puerta a los servicios de generación existentes. Le siguen la interfaz (cliente, store, panel y modal) y, al final, las tareas de contratos, que se formalizan después de implementar. En total son 43 tareas.

## Tareas

### [ ] TASK-001 Puerta de generación única en memoria

- **Cubre:** REQ-032, REQ-033, DEC-002, ALT-003
- **Componente:** generation-gate | por definir (analista técnico)
- **Depende de:** -
- **Alcance:** Componente que concede un único turno de generación para toda la aplicación, registra su tipo y, si es del escritor, su cuento, lo libera al terminar y responde si la generación en curso es de un cuento concreto.
- **Hecho cuando:** con un turno concedido se niega cualquier otro, sea del tipo que sea; al liberarlo se concede el siguiente; se puede consultar si la generación en curso es de un cuento dado, y al arrancar el servidor la puerta está libre.
- **Pruebas automáticas:**
  - Puerta libre y se pide turno de escritor para el cuento A: se concede.
  - Turno de escritor del cuento A en curso y se pide turno de escritor para el cuento B: se niega.
  - Turno de retratos en curso y se pide turno de escritor: se niega.
  - Turno de director en curso y se pide turno de agente de personaje: se niega.
  - Turno liberado tras terminar con éxito y se pide otro: se concede.
  - Turno liberado tras terminar con fallo y se pide otro: se concede.
  - Turno de escritor del cuento A en curso: la consulta "¿es del cuento A?" responde sí y "¿es del cuento B?" responde no.
  - Turno de retratos en curso: la consulta "¿es del cuento A?" responde no.
  - Dos peticiones de turno simultáneas con la puerta libre: solo se concede una.
  - Instancia recién creada: la puerta está libre.
- **Prueba manual:** No aplica: no tiene comportamiento observable por sí solo; se verifica de forma manual en las tareas que lo integran (TASK-010, TASK-011 y TASK-024 a TASK-028).

### [ ] TASK-002 Validador de longitud del texto narrativo

- **Cubre:** REQ-007, REQ-026, REQ-028, DEC-004, DEC-011
- **Componente:** page-length-validator | por definir (analista técnico)
- **Depende de:** -
- **Alcance:** Componente determinista que cuenta las frases y las palabras del texto narrativo y lo clasifica como sin aviso, exceso de palabras, número de frases fuera de 1 a 4 o ambos.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-007 y la regla de segmentación de frases para diálogos, puntos suspensivos y signos dobles está fijada y probada según DEC-011.
- **Pruebas automáticas:**
  - 3 frases y 64 palabras: sin aviso.
  - 2 frases y 67 palabras: aviso de exceso de palabras.
  - 5 frases y 40 palabras: aviso de número de frases.
  - 1 frase de 25 palabras: sin aviso.
  - 4 frases y 66 palabras (límite exacto): sin aviso.
  - 5 frases y 70 palabras: avisos de número de frases y de exceso de palabras.
  - Texto sin ningún signo de cierre de oración o vacío (0 frases): aviso de número de frases.
  - Texto con diálogo entre rayas y comillas que contiene signos de cierre: el recuento sigue la regla fijada por la capa técnica.
  - Texto con puntos suspensivos y con signos dobles (¿?, ¡!): el recuento sigue la regla fijada y no cuenta frases de más.
- **Prueba manual:** No aplica: es un componente interno sin interfaz propia; su efecto visible se comprueba de forma manual en TASK-014.

### [ ] TASK-003 Histórico de versiones de portada y página

- **Cubre:** REQ-034, REQ-036, REQ-037, REQ-038, REQ-039, DEC-001
- **Componente:** story-page-version-repository | por definir (analista técnico)
- **Depende de:** -
- **Alcance:** Persistencia de `PageVersion` (texto narrativo, escena, aviso de longitud y marca de vigente, sin imagen) asociada a una `StoryPage`, con alta, cambio de vigente, lectura del histórico y borrado de todas las versiones de una página.
- **Hecho cuando:** se pueden guardar y leer las versiones de una página con su aviso, en cada página hay como mucho una vigente, se borran todas a la vez y no hay límite de número de versiones.
- **Pruebas automáticas:**
  - Alta de una versión marcada como vigente en una página sin versiones: el histórico tiene una versión y es la vigente.
  - Alta de una segunda versión vigente: el histórico tiene dos y solo la nueva es vigente.
  - Cambio de vigente a la primera de tres versiones: solo la primera es vigente y se conservan las tres.
  - Alta de versiones con cada tipo de aviso (ninguno, exceso de palabras, número de frases, ambos): se leen con el mismo aviso.
  - Borrado del histórico de una página: no queda ninguna versión de esa página y las de otras páginas no cambian.
  - Alta de 11 versiones en una página: se leen las 11 y cualquiera se puede marcar como vigente.
- **Prueba manual:** No aplica: es persistencia interna; se verifica a través de la interfaz en TASK-032.

### [ ] TASK-004 Estado, contenido aceptado e imagen importada de la página

- **Cubre:** REQ-005, REQ-022, REQ-040, REQ-041, DEC-012
- **Componente:** story-page-repository | no consta en el mapa
- **Depende de:** -
- **Alcance:** Modificar `StoryPage` y su persistencia para los cuentos con escaleta: estado (pendiente de aceptación o aceptada), vínculo con el beat N (la portada es la página 0), contenido aceptado (texto y escena, o título y escena en la portada) y asociación opcional a una `ImportedImage` del contenido aceptado, que se puede leer y desasociar.
- **Hecho cuando:** una página de un cuento con escaleta se persiste y se lee con su estado, su beat, su contenido aceptado y, si la tiene, la referencia a su imagen importada; la asociación se puede eliminar sin tocar el resto del contenido, y las páginas de cuentos antiguos se siguen leyendo como antes.
- **Pruebas automáticas:**
  - Página pendiente guardada sin contenido aceptado: se lee como pendiente, vinculada a su beat y sin imagen.
  - Página aceptada con imagen importada preparada en los datos de prueba: se lee con su contenido aceptado y su imagen.
  - Desasociar la imagen de una página aceptada: se lee sin imagen y con el mismo contenido aceptado.
  - Portada aceptada: se lee con título y escena, sin texto narrativo.
  - Página de un cuento antiguo sin escaleta ya existente: se lee igual que antes del cambio.
- **Prueba manual:** No aplica: es persistencia interna; se verifica a través de la interfaz en TASK-031.

### [ ] TASK-005 Modelfile del escritor como recurso del backend y su aprovisionamiento

- **Cubre:** REQ-024, REQ-006, REQ-008, REQ-009, DEC-004, DEC-005
- **Componente:** writer-model-provisioner | por definir (analista técnico)
- **Depende de:** -
- **Alcance:** Mover el Modelfile del escritor a los recursos del backend, con instrucciones de fidelidad al beat, límite de frases y palabras, sin subordinadas encadenadas y con vocabulario para 3 a 4 años sin imitar el habla infantil, y registrar el modelo en Ollama al arrancar si no existe, con el mismo patrón que `director-model-provisioner`.
- **Hecho cuando:** se cumple el primer criterio de aceptación de REQ-024; al arrancar el backend con Ollama sin el modelo del escritor, este queda registrado desde el recurso del backend, y las instrucciones del Modelfile recogen las reglas de REQ-006, REQ-007, REQ-008 y REQ-009.
- **Pruebas automáticas:**
  - Arranque con el modelo del escritor ausente en Ollama: se pide su registro con el contenido del recurso del backend.
  - Arranque con el modelo ya registrado: no se vuelve a registrar.
  - El recurso del Modelfile del escritor existe en el classpath del backend y no está vacío.
- **Prueba manual:**
  1. Eliminar el modelo del escritor de Ollama y arrancar el backend: el modelo aparece en el listado de modelos de Ollama.
  2. Revisar el Modelfile del recurso: contiene las instrucciones de fidelidad al beat, de 1 a 4 frases y 60 palabras, sin subordinadas encadenadas y de vocabulario para 3 a 4 años sin habla infantil.

### [ ] TASK-006 Retirar el Modelfile del escritor de agents/

- **Cubre:** REQ-024, DEC-005
- **Componente:** agents/load_model.ps1 y Modelfile del escritor en agents/ | no consta en el mapa
- **Depende de:** TASK-005
- **Alcance:** Eliminar el Modelfile del escritor de `agents/` y quitar su referencia del script de carga.
- **Hecho cuando:** se cumple el segundo criterio de aceptación de REQ-024: el Modelfile del escritor ya no reside en `agents/` y el script de carga no lo referencia.
- **Pruebas automáticas:** No aplica: es la retirada de un fichero y de una referencia en un script de despliegue; se comprueba mediante revisión.
- **Prueba manual:**
  1. Revisar `agents/`: no hay Modelfile del escritor.
  2. Ejecutar el script de carga de `agents/`: termina sin errores y no intenta registrar el modelo del escritor.

### [ ] TASK-007 Adaptador del escritor

- **Cubre:** REQ-001, REQ-003, REQ-004, REQ-005, REQ-006, REQ-012, REQ-014, REQ-029, DEC-006, DEC-010, ALT-006, ALT-011
- **Componente:** writer-agent | por definir (analista técnico)
- **Depende de:** TASK-005
- **Alcance:** Adaptador del escritor en Ollama, extraído de `story-page-service`, con tres operaciones: escena de portada (con título, sinopsis e instantánea del protagonista), página N con su contexto (beat N, título, sinopsis, reparto episódico, instantánea y texto aceptado de las páginas anteriores) y nueva versión a partir de una base y un hint, que devuelve el contenido o un rechazo con motivo.
- **Hecho cuando:** cada operación envía al modelo del escritor el contexto indicado y devuelve la escena de portada, o el texto narrativo y la escena de la página, o el rechazo motivado del hint; una respuesta sin texto o sin escena, o un error del modelo, se devuelve como fallo.
- **Pruebas automáticas:**
  - Escena de portada: la petición incluye el título, la sinopsis y la instantánea, y no incluye beats ni reparto episódico; devuelve la escena.
  - Página 3 con las páginas 1 y 2 aceptadas: la petición incluye el beat 3, el título, la sinopsis, el reparto episódico, la instantánea y el texto de las páginas 1 y 2.
  - Página generada: devuelve el texto narrativo y la descripción de escena, sin imagen.
  - Nueva versión con hint aceptado: la petición incluye la versión base y el hint, y devuelve el nuevo texto y la nueva escena.
  - Hint rechazado por el modelo: devuelve el rechazo con el motivo del escritor.
  - Respuesta del modelo sin texto narrativo o sin descripción de escena: devuelve un fallo.
  - Modelo no disponible o error de Ollama: devuelve un fallo.
- **Prueba manual:**
  1. Con el modelo del escritor cargado, pedir una página desde un beat cuyo campo "quiénes aparecen" lista al protagonista y a un conejo blanco: el texto y la escena narran el hecho del beat, recogen su nota visual y no incluyen otros personajes con nombre.
  2. Pedir una nueva versión con el hint "que aparezca el lobo" sobre un beat sin lobo: se devuelve un rechazo con motivo.

### [ ] TASK-008 Notificador de eventos de generación de portada y página

- **Cubre:** REQ-014, REQ-020, REQ-022, DEC-003
- **Componente:** story-page-notifier | por definir (analista técnico)
- **Depende de:** -
- **Alcance:** Componente que publica los eventos de inicio, versión presentada, hint rechazado con motivo y fallo de una generación de portada o página, con el cuento y la página a los que afectan.
- **Hecho cuando:** cada uno de los cuatro eventos se publica con su tipo, su cuento, su página y, en el rechazo, el motivo.
- **Pruebas automáticas:**
  - Publicar inicio de la página 3 del cuento A: el evento lleva el tipo inicio, el cuento A y la página 3.
  - Publicar versión presentada de la portada: el evento identifica la portada.
  - Publicar hint rechazado: el evento lleva el motivo del escritor.
  - Publicar fallo: el evento lleva el tipo fallo y la página afectada.
- **Prueba manual:** No aplica: no tiene salida visible por sí solo; se verifica de forma manual en TASK-009 y TASK-034.

### [ ] TASK-009 Integración: difusión por /storyteller de los eventos de portada y página

- **Cubre:** REQ-020, REQ-022, DEC-003
- **Componente:** story-page-notifier | por definir (analista técnico); websocket-adapter | api/src/main/java/es/vargontoc/storyteller/infrastructure/adapters/in/websocket/WebsocketAdapter.java
- **Depende de:** TASK-008
- **Alcance:** Hacer que `websocket-adapter` difunda por el canal /storyteller los eventos de `story-page-notifier`, sin alterar las notificaciones generales ni los eventos de retratos.
- **Hecho cuando:** un evento publicado por `story-page-notifier` llega a un cliente suscrito a /storyteller con su tipo, su cuento y su página, y los eventos de retratos se siguen recibiendo igual.
- **Pruebas automáticas:**
  - Publicar un evento de versión presentada: el cliente suscrito recibe el evento con el cuento y la página.
  - Publicar un evento de retratos: se sigue recibiendo con su formato actual.
- **Prueba manual:**
  1. Conectar un cliente WebSocket a /storyteller y lanzar una generación de portada: llegan los eventos de inicio y de versión presentada.

### [ ] TASK-010 Bifurcación por escaleta y cuentos antiguos a través de la puerta

- **Cubre:** REQ-023, REQ-032, DEC-001, DEC-007, ALT-003
- **Componente:** story-page-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageService.java
- **Depende de:** TASK-001
- **Alcance:** Bifurcar las operaciones de página según el marcador de versión de guion: los cuentos sin escaleta siguen el flujo actual (deducción de la página siguiente y `StoryPageReview`), pero piden turno a `generation-gate` antes de generar y lo liberan al terminar.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-023, y una generación de un cuento antiguo se rechaza con la puerta ocupada y la libera al terminar con éxito o con fallo.
- **Pruebas automáticas:**
  - Cuento sin escaleta: generar página deduce la página siguiente y produce el mismo resultado que antes de esta feature.
  - Cuento sin escaleta con una revisión pendiente: pedir otra revisión sigue descartando la anterior, como antes.
  - Cuento sin escaleta con la puerta ocupada: la generación se rechaza indicando que hay una generación en curso.
  - Cuento sin escaleta cuya generación falla: la puerta queda libre.
  - Cuento con escaleta: las operaciones no usan el flujo antiguo ni `StoryPageReview`.
- **Prueba manual:**
  1. Abrir un cuento antiguo y generar una página: se comporta como antes.
  2. Con retratos en generación, generar una página de un cuento antiguo: se rechaza indicando que hay una generación en curso.

### [ ] TASK-011 Generar la escena de portada

- **Cubre:** REQ-001, REQ-020, REQ-032, REQ-034, DEC-003, DEC-006, DEC-010, ALT-003, ALT-011
- **Componente:** story-page-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageService.java
- **Depende de:** TASK-001, TASK-003, TASK-004, TASK-007, TASK-008, TASK-010
- **Alcance:** Caso de uso, expuesto por la API REST, que en un cuento con escaleta, guion aprobado y sin portada pide turno, responde que la generación ha empezado y, de forma asíncrona, pide al escritor la escena de portada y crea la portada pendiente con el título del guion, sin imagen y con la escena como primera versión vigente sin aviso.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-001; la respuesta llega en cuanto se concede el turno, la puerta se libera al terminar y se publican los eventos de inicio y de versión presentada o de fallo.
- **Pruebas automáticas:**
  - Cuento con guion aprobado y sin portada: la solicitud responde "generación iniciada" y al terminar la portada está pendiente, con el título idéntico al del guion, sin imagen y con una versión vigente sin aviso.
  - La escena de portada nunca pasa por la validación de longitud: una escena de más de 66 palabras se guarda sin aviso.
  - Guion no aprobado: se rechaza sin pedir turno.
  - Cuento que ya tiene portada: se rechaza sin pedir turno.
  - Puerta ocupada por otra generación: se rechaza indicando que hay una generación en curso.
  - El escritor falla: no se crea portada, se publica el fallo, la puerta queda libre y se puede volver a pedir.
  - Generación terminada con éxito: la puerta queda libre y se ha publicado la versión presentada.
- **Prueba manual:**
  1. Con un cuento con guion aprobado, pedir la portada por la API: la respuesta indica que la generación ha empezado.
  2. Consultar la portada al terminar: está pendiente, con el título del guion y la escena, sin imagen.

### [ ] TASK-012 Aceptar la portada o una página pendiente

- **Cubre:** REQ-011, REQ-027, REQ-028, REQ-033, REQ-037, REQ-041, DEC-001, DEC-002, DEC-010, DEC-012, ALT-004, ALT-009
- **Componente:** story-page-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageService.java
- **Depende de:** TASK-011
- **Alcance:** Caso de uso, expuesto por la API REST, que acepta la versión vigente de la portada o de una página pendiente como contenido aceptado sin imagen, elimina el resto del histórico y no inicia ninguna generación; rechaza la aceptación si la vigente tiene aviso de número de frases o si hay una generación en curso de ese cuento.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-011, REQ-027, los tres primeros de REQ-028, REQ-037, el segundo de REQ-041 y el primero de REQ-033.
- **Pruebas automáticas:**
  - Portada pendiente: al aceptarla queda aceptada sin imagen, sin histórico, y no se inicia ninguna generación.
  - Página 1 pendiente: al aceptarla queda aceptada y no se inicia la generación de la página 2.
  - Página 3 con tres versiones y la segunda vigente: queda aceptada con la segunda y sin histórico.
  - Vigente con solo aviso de exceso de palabras: se acepta con el texto sin cambios.
  - Vigente con aviso de número de frases: se rechaza indicando que hay que elegir otra versión válida, pedir un cambio con hint o regenerar sin hint, y la página sigue pendiente con el mismo histórico.
  - Vigente con avisos de número de frases y de exceso de palabras: se rechaza.
  - Generación en curso del mismo cuento: se rechaza indicando que hay una generación en curso y no cambia nada.
  - Generación en curso de otro cuento o de otro tipo: la aceptación se permite.
- **Prueba manual:**
  1. Aceptar una portada pendiente por la API y consultarla: está aceptada, sin imagen y sin histórico.

### [ ] TASK-013 Generar la página N desde su beat

- **Cubre:** REQ-002, REQ-003, REQ-004, REQ-005, REQ-010, REQ-028, REQ-034, DEC-003, DEC-006, DEC-007, ALT-001, ALT-002, ALT-014
- **Componente:** story-page-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageService.java
- **Depende de:** TASK-011, TASK-012
- **Alcance:** Caso de uso, expuesto por la API REST, que valida que N es la siguiente página permitida, pide turno, responde que la generación ha empezado y, de forma asíncrona, reúne el contexto (beat N, título, sinopsis y reparto episódico del guion aprobado, instantánea del protagonista guardada en el cuento y texto aceptado de las páginas 1 a N-1), pide la página al escritor y crea la página N pendiente, vinculada al beat N, sin imagen y con una versión vigente.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-002, REQ-003, REQ-004, REQ-005, REQ-010 y el cuarto de REQ-028.
- **Pruebas automáticas:**
  - Portada aceptada: pedir la página 1 inicia la generación y al terminar la página 1 está pendiente, vinculada al beat 1 y sin imagen.
  - Portada pendiente o inexistente: pedir la página 1 se rechaza indicando que primero hay que aceptar la portada, sin pedir turno ni llamar al escritor.
  - Página 2 pendiente: pedir la página 3 se rechaza indicando que primero hay que aceptar la página 2.
  - Página 2 pendiente con aviso de número de frases en la vigente: pedir la página 3 se rechaza.
  - Página 2 aceptada y sin página 3: pedir la página 3 genera solo la página 3.
  - Pedir una página que ya existe o que no es la siguiente: se rechaza.
  - Páginas 1 y 2 aceptadas: el escritor recibe el beat 3, el título, la sinopsis, el reparto episódico, la instantánea y el texto aceptado de 1 y 2.
  - Protagonista editado en el catálogo después de crear el cuento: el escritor recibe la instantánea del cuento.
  - Página 3 aceptada tras tener tres versiones: al generar la página 4, el escritor recibe el texto de la versión aceptada.
  - Puerta ocupada: se rechaza indicando que hay una generación en curso.
- **Prueba manual:**
  1. Con la portada aceptada, pedir la página 1 por la API: la respuesta indica que la generación ha empezado y al terminar la página 1 está pendiente, con texto y escena del beat 1.
  2. Pedir la página 2 sin haber aceptado la página 1: se rechaza con el motivo.

### [ ] TASK-014 Regeneración automática por longitud y versiones con aviso

- **Cubre:** REQ-007, REQ-020, REQ-025, REQ-026, REQ-032, REQ-034, REQ-035, DEC-004, DEC-009, ALT-007, ALT-008, ALT-011
- **Componente:** story-page-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageService.java
- **Depende de:** TASK-002, TASK-013
- **Alcance:** Aplicar a cada solicitud de generación de página el bucle de validación con `page-length-validator`: dentro del mismo turno, hasta 3 generaciones en total; guardar solo la primera que cumpla o, si ninguna cumple, las 3 con su aviso y la tercera como vigente; y tratar como fallo toda la solicitud si falla cualquiera de las generaciones.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-025 y REQ-026 y el tercero de REQ-020 para la generación inicial de una página, y el bucle queda disponible para el cambio con hint, la reapertura y la regeneración sin hint.
- **Pruebas automáticas:**
  - Primera generación con 70 palabras y segunda con 55 palabras y 3 frases: no hay tercera, el histórico solo contiene la segunda, que es la vigente sin aviso.
  - Primera generación que cumple: una sola llamada al escritor y una versión en el histórico.
  - Las 3 generaciones con más de 66 palabras y de 1 a 4 frases: no hay cuarta y el histórico contiene las 3 con aviso de exceso de palabras, con la tercera vigente.
  - Las 3 generaciones con 5 frases: la página queda pendiente con las 3 versiones con aviso de número de frases.
  - Primera con 5 frases y segunda y tercera con más de 66 palabras: avisos de número de frases, de exceso de palabras y de exceso de palabras, y la vigente es la tercera.
  - 3 generaciones fuera del límite: se publica versión presentada, no fallo.
  - Durante las regeneraciones, la puerta sigue ocupada: otra solicitud de generación se rechaza.
  - Durante las regeneraciones, consultar la página: no se presenta ninguna versión de esa solicitud.
  - La segunda generación falla después de una primera fuera del límite: no se guarda ninguna versión, la página no existe, se publica el fallo y la puerta queda libre.
- **Prueba manual:**
  1. Con un escritor de prueba que devuelva textos de más de 66 palabras, pedir una página: al terminar aparecen 3 versiones con aviso de exceso de palabras y la tercera vigente.

### [ ] TASK-015 Pedir un cambio con hint sobre una portada o página pendiente

- **Cubre:** REQ-012, REQ-013, REQ-014, REQ-020, REQ-025, REQ-034, REQ-035, REQ-039, DEC-001, DEC-006, DEC-009, ALT-005, ALT-006, ALT-011
- **Componente:** story-page-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageService.java
- **Depende de:** TASK-014
- **Alcance:** Caso de uso, expuesto por la API REST, que rechaza el hint vacío y, con turno concedido, envía al escritor la versión vigente como base junto con el hint y el contexto; si el escritor lo acepta, la nueva versión pasa por el bucle de longitud (salvo en la portada) y se añade al histórico como vigente; si lo rechaza, publica el motivo sin cambiar nada.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-012, REQ-013, REQ-014, REQ-034 (segundo y cuarto), REQ-035, REQ-039, el segundo de REQ-020 y el segundo de REQ-027.
- **Pruebas automáticas:**
  - Página 3 pendiente con el hint "que el conejo esté contento": se añade una versión completa con texto y escena, pendiente y vigente.
  - Página 3 con las versiones 1 y 2 y la 1 vigente: el escritor recibe la versión 1 como base.
  - Otro hint sobre la nueva versión: se añade otra versión completa.
  - Portada pendiente con una versión y un hint: el histórico de la portada tiene dos versiones, sin aviso.
  - Hint vacío o solo con espacios: se rechaza indicando que el hint es obligatorio, sin pedir turno ni llamar al escritor, y la página no cambia.
  - Hint rechazado por el escritor (fuera del beat o no apto para 3 a 4 años): se publica el rechazo con su motivo, no hay regeneración por longitud y el histórico y la vigente no cambian.
  - Fallo del escritor con el hint: se publica el fallo y el histórico y la vigente no cambian.
  - Nueva versión fuera del límite: se aplican las mismas regeneraciones automáticas de TASK-014.
  - Vigente con aviso de exceso de palabras y un hint: se genera una nueva versión.
  - 10 hints aceptados sobre una página con una versión: el histórico tiene 11 versiones.
  - Puerta ocupada: se rechaza indicando que hay una generación en curso.
- **Prueba manual:**
  1. Enviar por la API un hint sobre una página pendiente: al terminar, el histórico tiene una versión más y es la vigente.
  2. Enviar el hint "que aparezca el lobo" sobre una página cuyo beat no incluye al lobo: llega el rechazo con motivo y la página no cambia.

### [ ] TASK-016 Regenerar sin hint una página bloqueada por número de frases

- **Cubre:** REQ-029, REQ-025, REQ-026, REQ-034, DEC-006, ALT-010
- **Componente:** story-page-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageService.java
- **Depende de:** TASK-014
- **Alcance:** Caso de uso, expuesto por la API REST, que, solo si la versión vigente tiene aviso de número de frases, pide al escritor la página desde su beat con la operación de página N, aplica el bucle de longitud y añade el resultado al histórico como vigente.
- **Hecho cuando:** se cumple el primer criterio de aceptación de REQ-029 y el tercero de REQ-034, y la operación se rechaza sin llamar al escritor si la vigente no tiene aviso de número de frases.
- **Pruebas automáticas:**
  - Vigente con aviso de número de frases: se añade una nueva versión pendiente al histórico, que pasa a ser la vigente.
  - Nueva versión que cumple el límite: entra sin aviso.
  - Nuevas generaciones fuera del límite: se aplican las regeneraciones y los avisos de TASK-014.
  - Vigente sin aviso: se rechaza sin llamar al escritor.
  - Vigente con solo aviso de exceso de palabras y otra versión del histórico con aviso de número de frases: se rechaza.
  - Puerta ocupada: se rechaza indicando que hay una generación en curso.
- **Prueba manual:**
  1. Con una página cuya vigente tiene aviso de número de frases, regenerarla sin hint por la API: aparece una versión nueva vigente en el histórico.

### [ ] TASK-017 Marcar una versión como vigente

- **Cubre:** REQ-026, REQ-028, REQ-033, REQ-036, REQ-038, DEC-001, DEC-002, ALT-004
- **Componente:** story-page-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageService.java
- **Depende de:** TASK-003, TASK-012
- **Alcance:** Caso de uso, expuesto por la API REST, que marca como vigente una versión del histórico de la portada o de una página pendiente, o una de las propuestas de una reapertura, y lo rechaza si hay una generación en curso de ese cuento.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-036, el cuarto de REQ-026, el tercero de REQ-028, el tercero y el cuarto de REQ-033 y el segundo de REQ-038.
- **Pruebas automáticas:**
  - Página 3 con tres versiones y la tercera vigente: al marcar la primera, la página muestra su texto, su escena y su aviso, y el histórico conserva las tres.
  - Primera versión marcada como vigente y luego aceptada: la página queda aceptada con el texto de la primera.
  - Vigente con aviso de número de frases y se marca otra sin ese aviso: la aceptación pasa a estar permitida.
  - Generación en curso de ese cuento: se rechaza indicando que hay una generación en curso y la vigente no cambia.
  - Generación del cuento recién terminada: se permite marcar otra versión.
  - Página reabierta: solo se puede marcar una de sus propuestas; pedir marcar la versión aceptada se rechaza.
- **Prueba manual:**
  1. Marcar como vigente por la API la primera de tres versiones y consultar la página: muestra el contenido de la primera.

### [ ] TASK-018 Reabrir con hint la portada o una página aceptada

- **Cubre:** REQ-015, REQ-025, REQ-031, REQ-038, REQ-040, DEC-001, DEC-012
- **Componente:** story-page-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageService.java
- **Depende de:** TASK-015
- **Alcance:** Extender el cambio con hint a la portada y las páginas aceptadas: la nueva versión se guarda como propuesta en el histórico de la reapertura, la versión aceptada no cambia y conserva su imagen importada, y los hints siguientes usan como base la propuesta vigente.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-015, el primero de REQ-031 y el primero y el cuarto de REQ-038.
- **Pruebas automáticas:**
  - Páginas 1 a 4 aceptadas y un hint sobre la página 2: hay una propuesta pendiente de la página 2 y las páginas 3 y 4 siguen presentes.
  - Dos hints sobre la página 2 aceptada: hay dos propuestas y la versión aceptada conserva su texto original.
  - Otro hint con una propuesta pendiente: el escritor recibe la propuesta vigente como base.
  - Página 2 aceptada con imagen importada (datos preparados) y un hint: la versión aceptada sigue con su imagen y la propuesta no tiene imagen.
  - Hint sobre la portada aceptada con las páginas 1 a 3 aceptadas: hay una propuesta de portada pendiente.
  - Propuesta de página fuera del límite: se aplican las regeneraciones de TASK-014 y las versiones con aviso van al histórico de propuestas.
- **Prueba manual:**
  1. Enviar por la API un hint sobre una página aceptada: la página muestra la versión aceptada y aparte una propuesta pendiente.

### [ ] TASK-019 Aceptar una propuesta de reapertura con descarte en cascada

- **Cubre:** REQ-016, REQ-017, REQ-018, REQ-031, REQ-033, REQ-038, REQ-041, DEC-012, ALT-004, ALT-012
- **Componente:** story-page-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageService.java
- **Depende de:** TASK-017, TASK-018
- **Alcance:** Al aceptar la propuesta vigente de una página reabierta K con páginas posteriores, indicar cuántas se eliminarán y aplicar la aceptación solo con confirmación explícita: K queda aceptada con la propuesta sin imagen, se desasocia su imagen anterior, se borra su histórico y se eliminan las páginas posteriores con sus históricos y su imagen, sin regenerar nada; en la portada se sustituye la aceptada sin eliminar páginas.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-016, REQ-017, REQ-018, el segundo y el cuarto de REQ-031, el tercero de REQ-038 y el primero de REQ-041.
- **Pruebas automáticas:**
  - Páginas 1 a 4 y una propuesta para la 2, sin confirmación: la respuesta indica que se eliminarán 2 páginas y no cambia nada.
  - Misma situación con confirmación: la página 2 queda aceptada con la propuesta y sin histórico, las páginas 3 y 4 dejan de existir con sus históricos y la siguiente página permitida es la 3.
  - Tras el descarte: no se inicia ninguna generación; pedir la página 3 genera solo la página 3, sin imagen.
  - Página 2 con imagen importada: tras aceptar la propuesta queda sin imagen y la anterior deja de estar asociada.
  - Página 3 eliminada que tenía imagen importada: la imagen desaparece del cuento con la página.
  - Página reabierta sin páginas posteriores: se acepta sin pedir confirmación.
  - Portada reabierta con las páginas 1 a 3 aceptadas: al aceptar la propuesta, la portada queda aceptada con la nueva versión, sin imagen, sin aviso de descarte y con las 3 páginas aceptadas.
  - Generación en curso de ese cuento: se rechaza.
- **Prueba manual:**
  1. Con las páginas 1 a 4 aceptadas, reabrir la página 2 y aceptar la propuesta por la API sin confirmación: se informa de 2 páginas a eliminar.
  2. Repetir con confirmación: el cuento queda con las páginas 1 y 2, y no se inicia ninguna generación.

### [ ] TASK-020 Descartar las propuestas de una reapertura

- **Cubre:** REQ-030, REQ-031, REQ-033, DEC-001, DEC-012, ALT-004, ALT-013
- **Componente:** story-page-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageService.java
- **Depende de:** TASK-018
- **Alcance:** Caso de uso, expuesto por la API REST, que elimina todo el histórico de propuestas de una portada o página reabierta y conserva intactos su contenido aceptado, su imagen importada y las páginas posteriores; lo rechaza si hay una generación en curso de ese cuento.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-030, el tercero y el quinto de REQ-031 y el segundo de REQ-033.
- **Pruebas automáticas:**
  - Páginas 1 a 4 y tres propuestas para la 2: al descartar, la página 2 vuelve a su versión aceptada sin histórico y las páginas 3 y 4 siguen aceptadas.
  - Propuesta vigente con aviso de número de frases: se puede descartar y se conserva la versión aceptada.
  - Página 2 con imagen importada y propuestas: tras descartar, conserva su imagen.
  - Portada reabierta con imagen importada: tras descartar, conserva su versión aceptada y su imagen.
  - Generación con hint de esa propuesta en curso: se rechaza indicando que hay una generación en curso.
- **Prueba manual:**
  1. Reabrir una página con dos hints y descartar las propuestas por la API: la página muestra su versión aceptada y no tiene histórico.

### [ ] TASK-021 Cuento con todas las páginas aceptadas

- **Cubre:** REQ-021, ALT-014
- **Componente:** story-page-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageService.java
- **Depende de:** TASK-013
- **Alcance:** Al aceptar la última página de la escaleta, marcar y persistir el cuento en el nuevo estado de `Story` "todas las páginas aceptadas y listo para audio", y rechazar cualquier solicitud de generar más páginas o una página con número mayor que el de la escaleta.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-021.
- **Pruebas automáticas:**
  - Guion de 8 páginas con las 1 a 7 aceptadas: al aceptar la 8, el cuento pasa al estado de listo para audio.
  - Cuento completo: pedir otra página se rechaza y el cuento sigue completo.
  - Pedir una página con número mayor que el de la escaleta: se rechaza.
  - Aceptar una página que no es la última: el cuento no cambia de estado.
- **Prueba manual:**
  1. Aceptar la última página de un cuento y consultarlo: su estado indica que está listo para la etapa de audio.

### [ ] TASK-022 Consulta de portada y páginas con acciones e imagen de la versión aceptada

- **Cubre:** REQ-019, REQ-022, REQ-029, REQ-038, REQ-040, DEC-003, DEC-012
- **Componente:** story-page-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageService.java
- **Depende de:** TASK-004, TASK-016, TASK-019, TASK-020, TASK-021
- **Alcance:** Consulta, expuesta por la API REST, de la portada y las páginas de un cuento con escaleta (estado, beat, contenido de la vigente, contenido aceptado con indicación de imagen, histórico o propuestas con aviso y marca de vigente, y acciones disponibles en cada estado) y lectura de la imagen importada solo de la versión aceptada; sin ninguna operación de edición manual de texto o escena.
- **Hecho cuando:** la consulta devuelve para cada estado de REQ-022 las acciones que corresponden, las versiones pendientes y las propuestas nunca tienen imagen, la imagen solo se obtiene para el contenido aceptado y no existe ninguna operación para editar a mano el texto o la escena.
- **Pruebas automáticas:**
  - Pendiente sin aviso en la vigente: acciones de aceptar y de cambio con hint.
  - Vigente con aviso de exceso de palabras: acciones de aceptar y de cambio con hint, con el aviso.
  - Vigente con aviso de número de frases: acciones de cambio con hint y de regenerar sin hint, sin aceptar.
  - Vigente sin aviso de número de frases y otra versión con ese aviso: no se ofrece regenerar sin hint.
  - Pendiente con varias versiones: se devuelven todas con su aviso y se identifica la vigente.
  - Aceptada sin propuestas: estado aceptada, acción de cambio con hint y sin histórico.
  - Reabierta: versión aceptada separada de las propuestas y acción de descartar.
  - Última página existente aceptada y no última de la escaleta: se ofrece generar la siguiente; cuento completo: no se ofrece.
  - Página aceptada con imagen importada (datos preparados): se indica que tiene imagen y se puede obtener; página pendiente: ninguna versión tiene imagen.
  - Portada aceptada con imagen y reabierta: solo el contenido aceptado tiene imagen.
  - Pedir la imagen de una página sin imagen importada: se responde que no existe.
- **Prueba manual:**
  1. Con datos preparados de una página aceptada con imagen, consultar las páginas por la API: la página indica que tiene imagen y la imagen se descarga.
  2. Revisar la API de páginas: no hay ninguna operación para escribir texto o escena a mano.

### [ ] TASK-023 Integración: eliminación de páginas invalidadas con sus históricos

- **Cubre:** REQ-023, DEC-001
- **Componente:** beat-editor | api/src/main/java/es/vargontoc/storyteller/application/service/BeatEditorService.java; story-page-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageService.java
- **Depende de:** TASK-003, TASK-004
- **Alcance:** Hacer que la orden de `beat-editor` de eliminar las páginas invalidadas por una edición de beat elimine también sus históricos de versiones y propuestas en `story-page-service`.
- **Hecho cuando:** tras una edición directa de beat que invalida páginas, esas páginas y todas sus versiones dejan de existir y las páginas no invalidadas conservan su histórico.
- **Pruebas automáticas:**
  - Edición de beat que invalida las páginas 3 y 4, con versiones pendientes: se eliminan las páginas y sus versiones.
  - Página no invalidada con versiones: conserva su histórico.
- **Prueba manual:**
  1. Editar un beat de un cuento con páginas generadas y confirmar la invalidación: las páginas invalidadas desaparecen sin dejar versiones visibles.

### [ ] TASK-024 Turno de generación en la validación de beat

- **Cubre:** REQ-032, DEC-002, DEC-008, ALT-003
- **Componente:** beat-editor | api/src/main/java/es/vargontoc/storyteller/application/service/BeatEditorService.java
- **Depende de:** TASK-001, TASK-023
- **Alcance:** Hacer que la validación con el director de una edición directa de beat pida turno a `generation-gate`, se rechace con la puerta ocupada y la libere al terminar con éxito o con fallo.
- **Hecho cuando:** con otra generación en curso la edición se rechaza indicando que hay una generación en curso, y mientras dura la validación las demás generaciones se rechazan.
- **Pruebas automáticas:**
  - Puerta ocupada por una página en generación: la edición de beat se rechaza indicando que hay una generación en curso y el beat no cambia.
  - Validación en curso: otra solicitud de generación se rechaza.
  - Validación terminada con éxito o con fallo del director: la puerta queda libre.
- **Prueba manual:**
  1. Con una generación de página en curso, editar un beat: se rechaza indicando que hay una generación en curso.

### [ ] TASK-025 Turno de generación en el guion

- **Cubre:** REQ-032, DEC-002, ALT-003
- **Componente:** story-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryService.java
- **Depende de:** TASK-001
- **Alcance:** Hacer que la generación del guion y su revisión global pidan turno a `generation-gate`, se rechacen con la puerta ocupada y la liberen al terminar con éxito o con fallo.
- **Hecho cuando:** con otra generación en curso, la generación y la revisión de guion se rechazan indicando que hay una generación en curso, y mientras se genera un guion las demás generaciones se rechazan.
- **Pruebas automáticas:**
  - Puerta ocupada: la generación de guion se rechaza indicando que hay una generación en curso.
  - Puerta ocupada: la revisión global por hint se rechaza.
  - Generación de guion en curso: una solicitud al agente de personaje se rechaza.
  - Generación o revisión terminada con éxito o con fallo: la puerta queda libre.
- **Prueba manual:**
  1. Con retratos en generación, crear un cuento: se rechaza indicando que hay una generación en curso.

### [ ] TASK-026 Turno de generación en el alta de personajes y la solicitud de retratos

- **Cubre:** REQ-032, DEC-002, ALT-003
- **Componente:** character-catalog-service | api/src/main/java/es/vargontoc/storyteller/application/service/CharacterCatalogService.java
- **Depende de:** TASK-001
- **Alcance:** Hacer que el alta de personaje con el agente pida y libere turno, y que la solicitud de retratos pida turno y lo entregue a `portrait-generation-service`, rechazando ambas con la puerta ocupada.
- **Hecho cuando:** con otra generación en curso, el alta con agente y la solicitud de retratos se rechazan indicando que hay una generación en curso, y el alta libera la puerta al terminar.
- **Pruebas automáticas:**
  - Puerta ocupada: el alta con agente se rechaza indicando que hay una generación en curso.
  - Puerta ocupada: la solicitud de retratos se rechaza y no se crea ningún trabajo.
  - Alta terminada con éxito o con fallo del agente: la puerta queda libre.
  - Solicitud de retratos aceptada: la puerta queda ocupada por los retratos.
- **Prueba manual:**
  1. Con una página en generación, dar de alta un personaje en modo automático: se rechaza indicando que hay una generación en curso.

### [ ] TASK-027 Turno de generación en la revisión de fichas

- **Cubre:** REQ-032, DEC-002, ALT-003
- **Componente:** character-review-service | api/src/main/java/es/vargontoc/storyteller/application/service/CharacterReviewService.java
- **Depende de:** TASK-001
- **Alcance:** Hacer que la solicitud de revisión al agente de personaje pida turno, se rechace con la puerta ocupada y la libere al terminar.
- **Hecho cuando:** se cumple el tercer criterio de aceptación de REQ-032 en lo que toca al agente de personaje, y la puerta se libera al terminar la revisión con éxito o con fallo.
- **Pruebas automáticas:**
  - Generación de guion en curso: la solicitud de cambio de ficha se rechaza indicando que hay una generación en curso.
  - Revisión terminada con éxito o con fallo: la puerta queda libre.
- **Prueba manual:**
  1. Con un guion en generación, pedir un cambio de ficha: se rechaza indicando que hay una generación en curso.

### [ ] TASK-028 Turno ocupado durante la generación de retratos

- **Cubre:** REQ-032, DEC-002, ALT-003
- **Componente:** portrait-generation-service | api/src/main/java/es/vargontoc/storyteller/application/service/PortraitGenerationService.java
- **Depende de:** TASK-026
- **Alcance:** Mantener ocupado el turno de `generation-gate` desde que se crea el trabajo de retratos hasta que termina con éxito o error, y liberarlo entonces, sin cambiar el bloqueo por personaje de `portrait-lock-service`.
- **Hecho cuando:** se cumple el segundo criterio de aceptación de REQ-032 y la puerta se libera al terminar el trabajo de retratos.
- **Pruebas automáticas:**
  - Retratos en generación: pedir una página se rechaza indicando que hay una generación en curso.
  - Trabajo de retratos terminado con éxito: la puerta queda libre.
  - Trabajo de retratos terminado con error: la puerta queda libre.
  - El bloqueo por personaje durante la generación sigue impidiendo el renombrado como antes.
- **Prueba manual:**
  1. Solicitar retratos de un protagonista y, mientras se generan, pedir una página: se rechaza indicando que hay una generación en curso.
  2. Al terminar los retratos, pedir la página: se inicia.

### [ ] TASK-029 Cliente REST de portada y páginas

- **Cubre:** REQ-019, REQ-022, REQ-040, DEC-003
- **Componente:** story-pages-api | por definir (analista técnico)
- **Depende de:** TASK-022
- **Alcance:** Cliente REST con las operaciones de portada y páginas de los cuentos con escaleta: pedir portada o página N, cambio con hint, regenerar sin hint, marcar vigente, aceptar (con confirmación del descarte), descartar propuestas, consultar y obtener la imagen de la versión aceptada; sin operaciones de edición manual.
- **Hecho cuando:** cada operación llama al servicio correspondiente con sus datos y devuelve la respuesta o el rechazo con su motivo.
- **Pruebas automáticas:**
  - Cada operación envía la petición esperada y devuelve los datos tipados de la respuesta.
  - Aceptar sin confirmación en una reapertura con páginas posteriores: devuelve el número de páginas a eliminar.
  - Respuesta de rechazo (generación en curso, página anterior sin aceptar, hint vacío, aviso de número de frases, cuento completo): se devuelve el motivo.
- **Prueba manual:** No aplica: no tiene interfaz propia; se verifica de forma manual en TASK-031 a TASK-033.

### [ ] TASK-030 Eventos de generación de portada y página en el store WebSocket

- **Cubre:** REQ-020, REQ-022, DEC-003
- **Componente:** websocket-store | app/src/stores/websocket.ts
- **Depende de:** TASK-009
- **Alcance:** Hacer que `websocket-store` reciba y exponga los eventos de inicio, versión presentada, hint rechazado con motivo y fallo de portada y página, además de los de retratos.
- **Hecho cuando:** un evento de portada o página recibido por /storyteller queda disponible en el store con su tipo, su cuento, su página y su motivo, y los eventos de retratos se siguen tratando igual.
- **Pruebas automáticas:**
  - Llega un evento de versión presentada: el store expone el cuento y la página afectados.
  - Llega un evento de hint rechazado: el store expone el motivo.
  - Llega un evento de retratos: se trata igual que antes.
- **Prueba manual:** No aplica: no tiene interfaz propia; se verifica de forma manual en TASK-034.

### [ ] TASK-031 Panel de páginas: vista y acciones básicas

- **Cubre:** REQ-002, REQ-010, REQ-011, REQ-013, REQ-019, REQ-021, REQ-022, REQ-040, DEC-012
- **Componente:** story-pages-panel | por definir (analista técnico)
- **Depende de:** TASK-029
- **Alcance:** Vista de la portada (título y escena) y de las páginas (texto y escena) de un cuento con escaleta, con su estado, la imagen solo en la versión aceptada, la acción de generar portada o la página siguiente cuando corresponde, aceptar y pedir un cambio con hint; sin ningún campo de edición manual del texto o de la escena.
- **Hecho cuando:** se cumplen los criterios de aceptación primero a cuarto, octavo y décimo de REQ-022, el de REQ-019 y el segundo de REQ-021.
- **Pruebas automáticas:**
  - Página aceptada con imagen: se muestran la imagen, el texto y la escena.
  - Portada aceptada con imagen: se muestran la imagen, el título y la escena.
  - Página aceptada sin imagen: se muestran el texto y la escena, sin imagen.
  - Página pendiente sin aviso: se muestran el estado pendiente y las acciones de aceptar y de cambio con hint.
  - Página aceptada sin propuestas: estado aceptada, acción de cambio con hint y sin histórico.
  - Última página existente aceptada y no última de la escaleta: se ofrece generar la siguiente; cuento completo: no se ofrece.
  - Ningún estado muestra un campo editable para el texto o la escena.
  - Hint vacío: no se envía y se indica que el hint es obligatorio.
  - Rechazo del servidor (por ejemplo, generación en curso): se muestra el motivo.
- **Prueba manual:**
  1. Abrir un cuento con guion aprobado y pedir la portada: al terminar se ve la portada pendiente con las acciones de aceptar y de cambio con hint.
  2. Aceptar la portada: aparece la acción de generar la página 1 y no empieza ninguna generación.
  3. Comprobar que no hay ningún campo para escribir en el texto o la escena.

### [ ] TASK-032 Panel de páginas: histórico, avisos, vigente y regeneración sin hint

- **Cubre:** REQ-022, REQ-026, REQ-027, REQ-028, REQ-029, REQ-036, REQ-039, REQ-040, DEC-001
- **Componente:** story-pages-panel | por definir (analista técnico)
- **Depende de:** TASK-031
- **Alcance:** Mostrar el histórico de una portada o página pendiente con el aviso de cada versión y la vigente destacada, la acción de marcar como vigente cualquier otra, y las acciones según el aviso de la vigente: aceptar si no tiene aviso de número de frases, y regenerar sin hint solo si lo tiene.
- **Hecho cuando:** se cumplen los criterios de aceptación quinto, sexto y séptimo de REQ-022 y el segundo de REQ-029.
- **Pruebas automáticas:**
  - Pendiente con varias versiones: se muestran todas con su aviso, la vigente destacada y la acción de marcar como vigente en las demás.
  - Vigente con aviso de exceso de palabras: se muestran el aviso y las acciones de aceptar y de cambio con hint.
  - Vigente con aviso de número de frases: se muestran el aviso y las acciones de cambio con hint y de regenerar sin hint, sin la de aceptar.
  - Vigente sin aviso de número de frases y otra versión con ese aviso: no se muestra regenerar sin hint.
  - Ninguna versión del histórico muestra imagen.
  - Histórico con 11 versiones: se muestran todas.
- **Prueba manual:**
  1. Con una página con 3 versiones con aviso, marcar como vigente la primera: la página muestra su texto, su escena y su aviso.
  2. Con una vigente con aviso de número de frases: no aparece aceptar y sí regenerar sin hint.

### [ ] TASK-033 Panel de páginas: reapertura, confirmación del descarte en cascada y descarte de propuestas

- **Cubre:** REQ-015, REQ-017, REQ-022, REQ-030, REQ-031, REQ-038, REQ-040, DEC-012, ALT-012, ALT-013
- **Componente:** story-pages-panel | por definir (analista técnico)
- **Depende de:** TASK-032
- **Alcance:** En una portada o página reabierta, mostrar la versión aceptada (con su imagen si la tiene) separada del histórico de propuestas (sin imagen), ofrecer descartar las propuestas y, al aceptar una propuesta con páginas posteriores, pedir confirmación indicando cuántas páginas se eliminarán.
- **Hecho cuando:** se cumplen los criterios de aceptación noveno de REQ-022, los de REQ-017, el primero y el cuarto de REQ-038 y el segundo de REQ-040.
- **Pruebas automáticas:**
  - Página reabierta: la versión aceptada aparece separada de las propuestas, con la acción de descartar.
  - Página aceptada con imagen y reabierta: la aceptada se muestra con imagen y las propuestas sin imagen.
  - Aceptar la propuesta de la página 2 con las páginas 3 y 4 existentes: se pide confirmación indicando que se eliminarán 2 páginas.
  - Cancelar la confirmación: no se envía nada y la propuesta sigue pendiente.
  - Aceptar la propuesta de la portada: no se pide confirmación de descarte.
  - Solo se puede marcar como vigente entre las propuestas, no la versión aceptada.
- **Prueba manual:**
  1. Con las páginas 1 a 4 aceptadas, reabrir la página 2 y pulsar aceptar: aparece el aviso de que se eliminarán 2 páginas; al cancelar, las páginas 3 y 4 siguen.
  2. Confirmar: el cuento queda con las páginas 1 y 2 y se ofrece generar la página 3.
  3. Reabrir otra página y descartar las propuestas: vuelve a verse la versión aceptada.

### [ ] TASK-034 Integración: recarga del panel por eventos de generación

- **Cubre:** REQ-014, REQ-020, REQ-022, REQ-025, DEC-003
- **Componente:** websocket-store | app/src/stores/websocket.ts; story-pages-panel | por definir (analista técnico)
- **Depende de:** TASK-030, TASK-033
- **Alcance:** Hacer que `story-pages-panel` recargue la portada o la página afectada al recibir de `websocket-store` un evento de versión presentada, muestre el motivo del hint rechazado y el mensaje de fallo, indique la generación en curso tras el evento de inicio y recargue el estado al reconectar el WebSocket.
- **Hecho cuando:** el resultado de cada generación aparece en el panel sin recargar la vista, el rechazo y el fallo se muestran con su mensaje y, tras una reconexión, el panel refleja el estado del servidor.
- **Pruebas automáticas:**
  - Evento de versión presentada del cuento abierto: el panel recarga la página afectada.
  - Evento de otro cuento: el panel no recarga.
  - Evento de hint rechazado: se muestra el motivo y la página no cambia.
  - Evento de fallo: se muestra un mensaje de error y se puede volver a pedir la operación.
  - Reconexión del WebSocket: el panel recarga el estado.
- **Prueba manual:**
  1. Pedir una página y esperar sin recargar: la página aparece al terminar.
  2. Detener Ollama y pedir una página: aparece un mensaje de error y se puede volver a pedir.
  3. Reiniciar el servidor durante una generación: al reconectar, el panel muestra el estado real y permite volver a pedir la operación.

### [ ] TASK-035 Modal del cuento: pestaña de páginas según escaleta

- **Cubre:** REQ-022, REQ-023, DEC-007
- **Componente:** story-modal | app/src/components/StoryModal.vue
- **Depende de:** TASK-034
- **Alcance:** En los cuentos con escaleta, mostrar `story-pages-panel` en la pestaña de páginas; en los cuentos antiguos, mantener la vista anterior.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-023 en la interfaz: un cuento con escaleta abre el panel nuevo y uno antiguo la vista anterior.
- **Pruebas automáticas:**
  - Cuento con escaleta: la pestaña de páginas muestra `story-pages-panel`.
  - Cuento sin escaleta: la pestaña de páginas muestra la vista anterior.
- **Prueba manual:**
  1. Abrir un cuento con escaleta y luego uno antiguo: el primero muestra el panel nuevo y el segundo la vista de páginas de siempre.

### [ ] TASK-036 Contrato: openapi - esquema de página del cuento

- **Cubre:** REQ-005, REQ-011, REQ-021, REQ-022, REQ-038, REQ-040, DEC-001, DEC-010, DEC-012
- **Componente:** story-page-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageService.java. Contrato: docs/contracts/openapi/schemas/story-page.yaml
- **Depende de:** TASK-022
- **Alcance:** Actualizar `story-page.yaml` con el estado de la página, el beat asociado, el contenido aceptado con la indicación de imagen importada, el histórico de versiones o de propuestas con aviso y vigente, y las acciones disponibles; la portada sin texto narrativo y sin aviso.
- **Hecho cuando:** el contrato existe en `docs/contracts/openapi/` y describe la interfaz tal como quedó implementada.
- **Pruebas automáticas:**
  - El contrato es OpenAPI válido según un validador del formato.
- **Prueba manual:**
  1. Comparar cada campo del esquema con la respuesta real de la consulta de páginas: coinciden.

### [ ] TASK-037 Contrato: openapi - esquema de versión de página

- **Cubre:** REQ-026, REQ-034, REQ-036, REQ-040, DEC-001, DEC-012
- **Componente:** story-page-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageService.java. Contrato: docs/contracts/openapi/schemas/, fichero por definir (analista técnico)
- **Depende de:** TASK-022
- **Alcance:** Crear el esquema de versión de portada o página con su texto, su escena, su aviso de longitud y la marca de vigente, sin imagen.
- **Hecho cuando:** el contrato existe en `docs/contracts/openapi/` y describe la interfaz tal como quedó implementada.
- **Pruebas automáticas:**
  - El contrato es OpenAPI válido según un validador del formato.
- **Prueba manual:**
  1. Comparar el esquema con una versión real devuelta por la API: coinciden los campos y los valores del aviso.

### [ ] TASK-038 Contrato: openapi - estado de cuento listo para audio

- **Cubre:** REQ-021
- **Componente:** story-page-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageService.java. Contrato: docs/contracts/openapi/schemas/enums/story-status.yaml
- **Depende de:** TASK-021
- **Alcance:** Añadir a `story-status.yaml` el nuevo estado de todas las páginas aceptadas y listo para la etapa de audio.
- **Hecho cuando:** el contrato existe en `docs/contracts/openapi/` y describe la interfaz tal como quedó implementada.
- **Pruebas automáticas:**
  - El contrato es OpenAPI válido según un validador del formato.
- **Prueba manual:**
  1. Comprobar que el valor del enumerado coincide con el que devuelve la API para un cuento completo.

### [ ] TASK-039 Contrato: openapi - paths de portada y páginas

- **Cubre:** REQ-001, REQ-002, REQ-010, REQ-012, REQ-013, REQ-015, REQ-016, REQ-017, REQ-021, REQ-028, REQ-029, REQ-030, REQ-032, REQ-033, REQ-036, REQ-040, DEC-003, DEC-007
- **Componente:** story-page-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageService.java. Contrato: docs/contracts/openapi/paths/story/, ficheros por definir (analista técnico)
- **Depende de:** TASK-011, TASK-012, TASK-013, TASK-015, TASK-016, TASK-017, TASK-018, TASK-019, TASK-020, TASK-021, TASK-022
- **Alcance:** Crear los paths para pedir portada o página N (respuesta de generación iniciada), cambio con hint, regeneración sin hint, marcar vigente, aceptar con confirmación del descarte y número de páginas a eliminar, descartar propuestas y obtener la imagen de la versión aceptada, con las respuestas de rechazo por portada o página anterior sin aceptar, aviso de número de frases, hint vacío, generación en curso y cuento completo.
- **Hecho cuando:** el contrato existe en `docs/contracts/openapi/` y describe la interfaz tal como quedó implementada.
- **Pruebas automáticas:**
  - El contrato es OpenAPI válido según un validador del formato.
  - Cada operación recoge las respuestas de rechazo que exigen REQ-002, REQ-010, REQ-013, REQ-028, REQ-032, REQ-033 y REQ-021.
- **Prueba manual:**
  1. Repasar que cada operación de la fila del diseño aparece en el contrato y coincide con la implementación.

### [ ] TASK-040 Contrato: openapi - registro de paths y esquemas en openapi.yaml

- **Cubre:** REQ-022, DEC-003
- **Componente:** story-page-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageService.java. Contrato: docs/contracts/openapi/openapi.yaml
- **Depende de:** TASK-036, TASK-037, TASK-039
- **Alcance:** Registrar en `openapi.yaml` los nuevos paths y esquemas de portada y páginas.
- **Hecho cuando:** el contrato existe en `docs/contracts/openapi/` y describe la interfaz tal como quedó implementada.
- **Pruebas automáticas:**
  - El documento raíz es OpenAPI válido y todas sus referencias a los nuevos paths y esquemas se resuelven.
- **Prueba manual:**
  1. Abrir `openapi.yaml` en un visor OpenAPI: aparecen las nuevas operaciones de portada y páginas.

### [ ] TASK-041 Contrato: openapi - rechazo por generación en curso en guion, revisión y edición de beat

- **Cubre:** REQ-032, DEC-002, DEC-008
- **Componente:** story-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryService.java. Contrato: docs/contracts/openapi/paths/story/generation-service.yaml, review-service.yaml, beat-edit-service.yaml
- **Depende de:** TASK-024, TASK-025
- **Alcance:** Añadir a `generation-service.yaml`, `review-service.yaml` (de `story-service`) y `beat-edit-service.yaml` (de `beat-editor`) la respuesta de rechazo por generación en curso en la aplicación.
- **Hecho cuando:** el contrato existe en `docs/contracts/openapi/` y describe la interfaz tal como quedó implementada.
- **Pruebas automáticas:**
  - Los contratos son OpenAPI válidos según un validador del formato y recogen la respuesta de rechazo.
- **Prueba manual:**
  1. Provocar el rechazo en cada operación con la puerta ocupada y comprobar que la respuesta coincide con la del contrato.

### [ ] TASK-042 Contrato: openapi - rechazo por generación en curso en personajes y retratos

- **Cubre:** REQ-032, DEC-002
- **Componente:** character-catalog-service | api/src/main/java/es/vargontoc/storyteller/application/service/CharacterCatalogService.java. Contrato: docs/contracts/openapi/paths/character/catalog-characters-service.yaml, character-review-request-service.yaml, character-portrait-generate-service.yaml
- **Depende de:** TASK-026, TASK-027, TASK-028
- **Alcance:** Añadir a `catalog-characters-service.yaml`, `character-portrait-generate-service.yaml` (de `character-catalog-service`) y `character-review-request-service.yaml` (de `character-review-service`) la respuesta de rechazo por generación en curso en la aplicación.
- **Hecho cuando:** el contrato existe en `docs/contracts/openapi/` y describe la interfaz tal como quedó implementada.
- **Pruebas automáticas:**
  - Los contratos son OpenAPI válidos según un validador del formato y recogen la respuesta de rechazo.
- **Prueba manual:**
  1. Provocar el rechazo en cada operación con la puerta ocupada y comprobar que la respuesta coincide con la del contrato.

### [ ] TASK-043 Contrato: websocket - eventos de generación de portada y página

- **Cubre:** REQ-014, REQ-020, REQ-022, DEC-003
- **Componente:** websocket-adapter | api/src/main/java/es/vargontoc/storyteller/infrastructure/adapters/in/websocket/WebsocketAdapter.java. Contrato: docs/contracts/websocket/, fichero por definir (analista técnico)
- **Depende de:** TASK-009
- **Alcance:** Crear el contrato de los eventos de inicio, versión presentada, hint rechazado con motivo y fallo, con el cuento y la página a los que afectan, de storyteller-server a storyteller-app por /storyteller.
- **Hecho cuando:** el contrato existe en `docs/contracts/websocket/` y describe la interfaz tal como quedó implementada.
- **Pruebas automáticas:**
  - El contrato es válido según las reglas de su formato, si el formato elegido lo permite.
- **Prueba manual:**
  1. Capturar los cuatro tipos de evento en /storyteller y comprobar que coinciden con el contrato.

## Cobertura

| Requisito | Tareas |
|---|---|
| REQ-001 | TASK-007, TASK-011, TASK-039 |
| REQ-002 | TASK-013, TASK-031, TASK-039 |
| REQ-003 | TASK-007, TASK-013 |
| REQ-004 | TASK-007, TASK-013 |
| REQ-005 | TASK-004, TASK-007, TASK-013, TASK-036 |
| REQ-006 | TASK-005, TASK-007 |
| REQ-007 | TASK-002, TASK-014 |
| REQ-008 | TASK-005 |
| REQ-009 | TASK-005 |
| REQ-010 | TASK-013, TASK-031, TASK-039 |
| REQ-011 | TASK-012, TASK-031, TASK-036 |
| REQ-012 | TASK-007, TASK-015, TASK-039 |
| REQ-013 | TASK-015, TASK-031, TASK-039 |
| REQ-014 | TASK-007, TASK-008, TASK-015, TASK-034, TASK-043 |
| REQ-015 | TASK-018, TASK-033, TASK-039 |
| REQ-016 | TASK-019, TASK-039 |
| REQ-017 | TASK-019, TASK-033, TASK-039 |
| REQ-018 | TASK-019 |
| REQ-019 | TASK-022, TASK-029, TASK-031 |
| REQ-020 | TASK-008, TASK-009, TASK-011, TASK-014, TASK-015, TASK-030, TASK-034, TASK-043 |
| REQ-021 | TASK-021, TASK-031, TASK-036, TASK-038, TASK-039 |
| REQ-022 | TASK-004, TASK-008, TASK-009, TASK-022, TASK-029, TASK-030, TASK-031, TASK-032, TASK-033, TASK-034, TASK-035, TASK-036, TASK-040, TASK-043 |
| REQ-023 | TASK-010, TASK-023, TASK-035 |
| REQ-024 | TASK-005, TASK-006 |
| REQ-025 | TASK-014, TASK-015, TASK-016, TASK-018, TASK-034 |
| REQ-026 | TASK-002, TASK-014, TASK-016, TASK-017, TASK-032, TASK-037 |
| REQ-027 | TASK-012, TASK-032 |
| REQ-028 | TASK-002, TASK-012, TASK-013, TASK-017, TASK-032, TASK-039 |
| REQ-029 | TASK-007, TASK-016, TASK-022, TASK-032, TASK-039 |
| REQ-030 | TASK-020, TASK-033, TASK-039 |
| REQ-031 | TASK-018, TASK-019, TASK-020, TASK-033 |
| REQ-032 | TASK-001, TASK-010, TASK-011, TASK-014, TASK-024, TASK-025, TASK-026, TASK-027, TASK-028, TASK-039, TASK-041, TASK-042 |
| REQ-033 | TASK-001, TASK-012, TASK-017, TASK-019, TASK-020, TASK-039 |
| REQ-034 | TASK-003, TASK-011, TASK-013, TASK-014, TASK-015, TASK-016, TASK-037 |
| REQ-035 | TASK-014, TASK-015 |
| REQ-036 | TASK-003, TASK-017, TASK-032, TASK-037, TASK-039 |
| REQ-037 | TASK-003, TASK-012 |
| REQ-038 | TASK-003, TASK-017, TASK-018, TASK-019, TASK-022, TASK-033, TASK-036 |
| REQ-039 | TASK-003, TASK-015, TASK-032 |
| REQ-040 | TASK-004, TASK-018, TASK-022, TASK-029, TASK-031, TASK-032, TASK-033, TASK-036, TASK-037, TASK-039 |
| REQ-041 | TASK-004, TASK-012, TASK-019 |
