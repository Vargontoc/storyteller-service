# Tareas: agent-director

Estado: aprobado
Revisión: 1
Basado en: requirements@2, design@1

## Resumen

El trabajo se divide entre la redefinición del agente director y su aprovisionamiento desde el backend, la persistencia del guion con escaleta contractual, la coordinación del caso de uso del guion, la edición directa de beats, la puerta de aprobación aplicada a la generación de páginas, la interfaz de usuario de la escaleta y la formalización de los contratos OpenAPI afectados. El escritor no se modifica: solo se le entrega el beat aprobado como contrato.

## Tareas

### [x] TASK-001 Redefinir el agente director para generar guion completo con escaleta contractual

- **Cubre:** REQ-003, REQ-004, REQ-005, REQ-008, REQ-009, ALT-002, ALT-003, DEC-003, DEC-006
- **Componente:** director-agent | api/src/main/resources/prompts/director.modelfile, api/src/main/java/es/vargontoc/storyteller/infrastructure/adapters/out/external/SpringAiDirectorAgent.java
- **Depende de:** -
- **Alcance:** Modificar el prompt y los parámetros del agente director para que, a partir de tema, tamaño y ficha del protagonista, devuelva título, sinopsis, reparto episódico ligero y una escaleta con un beat estructurado por página interior, con los cinco campos obligatorios, y para que procese un hint global devolviendo una escaleta completa con aceptación o rechazo y motivo.
- **Hecho cuando:** el agente dispone de una definición cargable cuyo contrato de salida incluye título, sinopsis, reparto con nombre y función narrativa, y una escaleta de beats con qué ocurre, función narrativa, quiénes aparecen, emoción o tono y nota visual; además, ante un hint global devuelve la escaleta completa revisada con indicación de aceptación y, en su caso, motivo de rechazo, y ante una validación de edición de beat devuelve el veredicto de edad con su motivo.
- **Pruebas automáticas:**
  - Una solicitud de generación produce un resultado con título, sinopsis, reparto episódico y un beat por página interior con los cinco campos obligatorios.
  - Un beat devuelto sin alguno de los cinco campos obligatorios es detectado como inválido por la validación del resultado del agente.
  - Una solicitud de revisión global devuelve la escaleta completa, un número de beats igual al de la escaleta vigente y el indicador de aceptación del hint.
  - Un hint que viola las reglas de edad o la coherencia del cuento se devuelve con aceptación negativa y motivo de rechazo.
  - Una solicitud de validación de edición de beat devuelve veredicto y motivo cuando el texto no cumple las reglas de edad.
- **Prueba manual:**
  1. Cargar el modelo del director según el procedimiento del módulo `agents` y solicitar un guion con tema, tamaño y protagonista.
  2. Comprobar que la respuesta incluye título, sinopsis, reparto con función narrativa y una escaleta con los cinco campos por beat.
  3. Enviar un hint global sobre el final del cuento y comprobar que la respuesta incluye la escaleta completa y el indicador de aceptación.
  4. Enviar un hint que vulnere las reglas de edad y comprobar que la respuesta indica el rechazo con su motivo.

### [x] TASK-002 Aprovisionar el modelo del director desde los recursos del backend

- **Cubre:** REQ-015, DEC-002
- **Componente:** director-model-provisioner | api/src/main/java/es/vargontoc/storyteller/infrastructure/config/DirectorModelProvisioner.java
- **Depende de:** -
- **Alcance:** Crear el aprovisionador que registra en Ollama el modelo del director al arrancar el backend a partir de un Modelfile gestionado como recurso del backend, análogo al aprovisionamiento del agente de personaje, y retirar el director del mantenimiento en `agents/`.
- **Hecho cuando:** el modelo del director queda registrado en Ollama al arrancar el backend a partir del recurso gestionado por la aplicación, y la carpeta `agents/director-story-agent/` deja de ser la ubicación de mantenimiento del Modelfile del director.
- **Pruebas automáticas:**
  - Al arrancar con el modelo ausente, el aprovisionador lo registra en Ollama con el nombre configurado.
  - Al arrancar con el modelo ya presente, el aprovisionador no vuelve a registrarlo.
  - La búsqueda del Modelfile del director lo resuelve como recurso del backend y no como fichero bajo `agents/`.
- **Prueba manual:**
  1. Arrancar el backend con Ollama disponible y sin el modelo del director y comprobar que queda registrado y disponible para el resto de operaciones.
  2. Comprobar que `agents/load_model.ps1` ya no carga el director y que el resto de agentes sigue cargándose con normalidad.

### [x] TASK-003 Persistir el guion con escaleta, reparto ligero y marcador de versión

- **Cubre:** REQ-003, REQ-004, REQ-005, REQ-016, DEC-005, DEC-006
- **Componente:** story-outline-repository | api/src/main/java/es/vargontoc/storyteller/infrastructure/adapters/out/persistence/StoryOutlineRepositoryAdapter.java
- **Depende de:** -
- **Alcance:** Crear el repositorio que persiste el guion de un cuento con título, sinopsis, repartoEpisódico ligero y la lista de beats estructurados vinculados a números de página interior, junto con el marcador de versión de guion que distingue los cuentos antiguos de los nuevos.
- **Hecho cuando:** se puede guardar y recuperar el guion completo de un cuento con sus beats ordenados por número de página, el reparto con nombre y función narrativa sin persistencia en el catálogo de personajes, y el marcador de versión que permite distinguir un cuento antiguo sin escaleta de uno nuevo con escaleta contractual.
- **Pruebas automáticas:**
  - Un guion guardado se recupera con título, sinopsis, reparto y la lista de beats en orden de página.
  - Cada beat recuperado conserva sus cinco campos: qué ocurre, función narrativa, quiénes aparecen, emoción o tono y nota visual.
  - El reparto recuperado contiene nombre y función narrativa de cada secundario y no genera entradas en el catálogo de personajes.
  - Un cuento con el marcador de versión antiguo se recupera sin escaleta y un cuento con el marcador nuevo se recupera con su escaleta.
  - Guardar un guion nuevo no altera los datos de un cuento antiguo ya persistido.
- **Prueba manual:**
  1. Crear un cuento nuevo y comprobar que su guion se guarda con la escaleta completa y el reparto ligero.
  2. Consultar un cuento anterior a la feature y comprobar que conserva su estructura original de guion y personajes.

### [x] TASK-004 Definir el modelo de escaleta y el reparto ligero en el dominio

- **Cubre:** REQ-003, REQ-004, REQ-005, REQ-016, DEC-005, DEC-006
- **Componente:** story-outline-repository | api/src/main/java/es/vargontoc/storyteller/domain/model/StoryOutline.java (con StoryBeat, EpisodicCastMember y StoryScriptVersion)
- **Depende de:** TASK-003
- **Alcance:** Definir en el dominio los tipos que representan el guion con escaleta, el beat estructurado con sus cinco campos obligatorios y el personaje secundario del reparto ligero, y añadir al cuento el marcador de versión de guion, sin alterar la estructura de los cuentos ya existentes.
- **Hecho cuando:** el dominio dispone del guion con escaleta, del beat con los cinco campos obligatorios y del reparto ligero con nombre y función narrativa, y el cuento expone el marcador de versión de guion que permite distinguir cuentos antiguos de nuevos.
- **Pruebas automáticas:**
  - Un beat se construye con los cinco campos obligatorios y los expone de forma consultable.
  - Un beat al que le falta alguno de los cinco campos se considera incompleto.
  - Un personaje secundario del reparto se construye con nombre y función narrativa y sin identificador de catálogo.
  - Un cuento creado con el formato nuevo expone el marcador de versión de escaleta contractual.
- **Prueba manual:**
  1. Revisar los tipos del dominio y comprobar que el beat refleja los cinco campos obligatorios de la escaleta.
  2. Comprobar que el reparto ligero no puede vincular un personaje del catálogo.

### [x] TASK-005 Modificar el servicio de guion para el protagonista obligatorio y la escaleta

- **Cubre:** REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-009, REQ-014, REQ-016, ALT-001, ALT-002, ALT-003, DEC-003, DEC-005
- **Componente:** story-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryService.java
- **Depende de:** TASK-001, TASK-003, TASK-004
- **Alcance:** Modificar el servicio de guion para exigir un protagonista del catálogo con retrato aprobado en la creación del cuento, resolver la elección al azar, tomar la instantánea textual y visual del protagonista y enviarla al director, validar que cada beat tenga los cinco campos obligatorios antes de persistir, y exponer la aprobación del guion y el envío de hint global con su resultado de aceptación o rechazo.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006 y REQ-007, un cuento se crea únicamente con un protagonista con retrato aprobado elegido por el usuario o al azar, el guion se persiste solo con una escaleta completa y válida, la aprobación deja el guion en estado aprobado y el hint global produce una revisión pendiente o un rechazo con motivo conservando la escaleta vigente.
- **Pruebas automáticas:**
  - Una creación sin protagonista indicado elige al azar un personaje del catálogo con retrato aprobado y lo deja como protagonista del cuento.
  - Una creación sin protagonista indicado cuando no hay ningún personaje con retrato aprobado se rechaza y no crea el cuento.
  - Una creación con un protagonista concreto utiliza ese personaje y conserva en el cuento la instantánea de su ficha y retrato aprobado.
  - El director recibe nombre, descripción narrativa, descripción visual y referencia al retrato aprobado del protagonista.
  - Un guion con un beat incompleto se rechaza, se solicita una nueva generación y no se persiste el cuento.
  - Un guion con todos los beats completos se persiste con su escaleta y el reparto ligero.
  - Aprobar el guion pendiente lo deja en estado aprobado.
  - Un hint global válido crea una revisión pendiente con la escaleta candidata.
  - Un hint global rechazado deja la revisión en estado rechazado con su motivo y la escaleta vigente sin cambios.
  - Los secundarios del reparto no se persisten en el catálogo de personajes ni se modifican en el catálogo.
- **Prueba manual:**
  1. Crear un cuento sin elegir protagonista y comprobar que se asigna uno con retrato aprobado y que el cuento lo conserva como protagonista.
  2. Crear un cuento con el catálogo sin retratos aprobados y comprobar que se rechaza indicando que debe existir un protagonista con retrato aprobado.
  3. Revisar el guion generado y comprobar que muestra título, sinopsis, reparto con función narrativa y escaleta completa.
  4. Aprobar el guion y comprobar que queda en estado aprobado.
  5. Enviar un hint global que no aplique a la edad y comprobar que se muestra el motivo del rechazo y la escaleta no cambia.

### [x] TASK-006 Modificar el repositorio de revisiones para la escaleta candidata

- **Cubre:** REQ-008, REQ-009, REQ-018, DEC-001
- **Componente:** story-review-repository | api/src/main/java/es/vargontoc/storyteller/application/ports/out/persistence/StoryReviewRepository.java
- **Depende de:** TASK-004
- **Alcance:** Modificar el repositorio de revisiones de guion para almacenar la escaleta candidata completa, el indicador de aceptación del hint, el motivo de rechazo y el conjunto de beats marcados como cambiados respecto a la versión vigente, reutilizando el ciclo de estados pendiente, confirmado y descartado.
- **Hecho cuando:** una revisión de guion pendiente recupera la escaleta candidata completa, la lista de beats que han cambiado respecto a la escaleta vigente y el resultado de aceptación del hint con su motivo de rechazo, y una revisión descartada o confirmada deja de ofrecerse como pendiente.
- **Pruebas automáticas:**
  - Una revisión creada con escaleta candidata se recupera con todos sus beats, el indicador de aceptación y, si procede, el motivo de rechazo.
  - La revisión expone la lista de beats marcados como cambiados respecto a la escaleta vigente.
  - Una escaleta candidata idéntica a la vigente se recupera sin ningún beat marcado como cambiado.
  - Descartar una revisión la retira como pendiente y conservar la escaleta vigente sin cambios.
  - Confirmar una revisión la retira como pendiente y deja disponible la escaleta candidata como versión vigente.
- **Prueba manual:**
  1. Enviar un hint global y comprobar que la revisión pendiente incluye la escaleta completa.
  2. Comprobar que la revisión señala qué beats han cambiado respecto a la escaleta anterior.

### [x] TASK-007 Implementar la edición directa de un beat con validación de edad

- **Cubre:** REQ-010, REQ-011, REQ-012, REQ-019, ALT-004, ALT-005, DEC-004
- **Componente:** beat-editor | api/src/main/java/es/vargontoc/storyteller/application/service/BeatEditorService.java
- **Depende de:** TASK-001, TASK-003, TASK-005, TASK-006
- **Alcance:** Crear el caso de uso de edición directa de un beat que valida el texto editado contra las reglas de edad mediante el agente director, guarda el beat validado, devuelve el guion al estado pendiente de aprobación, ycoordina el aviso de invalidación de las páginas ya generadas antes de aplicar la edición.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-010, REQ-011 y REQ-012, un texto válido se guarda y actualiza el beat, un texto que incumple las reglas de edad se rechaza con el motivo sin modificar el beat, y una edición sobre un guion aprobado con páginas generadas muestra el aviso de invalidación y solo elimina las páginas y aplica la edición cuando el usuario confirma.
- **Pruebas automáticas:**
  - Un texto editado que cumple las reglas de edad se guarda y el beat actualizado se refleja en la escaleta.
  - Un texto editado que incumple las reglas de edad se rechaza con el motivo y el beat conserva su texto anterior.
  - Una edición guardada sobre un guion aprobado devuelve el guion al estado pendiente de aprobación.
  - Una edición sobre un guion aprobado con páginas generadas devuelve el aviso de invalidación con el número de páginas afectadas.
  - Al confirmar el aviso, las páginas generadas se eliminan del cuento y el beat editado queda guardado con el guion en estado pendiente.
  - Al cancelar el aviso, no se aplica la edición, el estado del guion no cambia y las páginas permanecen en el cuento.
  - Un cambio en un beat no genera ninguna entrada en el catálogo de personajes.
- **Prueba manual:**
  1. Editar un beat de la escaleta con un texto apto y comprobar que se guarda y se refleja en la escaleta.
  2. Editar un beat con un texto no apto y comprobar que se rechaza con el motivo y que el beat no cambia.
  3. Con un guion aprobado, editar un beat y comprobar que el guion vuelve a estado pendiente y que la generación de páginas queda bloqueada.
  4. Con un guion aprobado y páginas generadas, iniciar la edición y comprobar que aparece el aviso de invalidación de las páginas.
  5. Confirmar el aviso y comprobar que las páginas desaparecen del cuento y que el beat editado está aplicado.
  6. Cancelar el aviso y comprobar que el guion conserva su estado y que las páginas siguen en el cuento.

### [x] TASK-008 Modificar el servicio de páginas para exigir el guion aprobado y entregar el beat al escritor

- **Cubre:** REQ-006, REQ-011, REQ-012, REQ-013, ALT-006, DEC-006
- **Componente:** story-page-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageService.java
- **Depende de:** TASK-003, TASK-004, TASK-007
- **Alcance:** Modificar el servicio de páginas para rechazar la generación de portada o página cuando el guion no esté aprobado, entregar al escritor el beat aprobado correspondiente a la página como contrato, y exponer la eliminación de las páginas invalidadas por una edición directa de beat.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-006 y REQ-013, la generación de portada o página se rechaza mientras el guion no esté aprobado, la generación de una página interior entrega al escritor el beat aprobado de esa página con sus cinco campos, y la eliminación de páginas invalidadas deja el cuento sin esas páginas.
- **Pruebas automáticas:**
  - Una solicitud de generación de portada con el guion pendiente de aprobación se rechaza indicando que debe aprobarse el guion.
  - Una solicitud de generación de página interior con el guion pendiente de aprobación se rechaza del mismo modo.
  - Una solicitud de generación con el guion aprobado se atiende.
  - La generación de la página N entrega al escritor el beat de la página N con qué ocurre, función narrativa, quiénes aparecen, emoción o tono y nota visual.
  - La generación de la portada no consume ningún beat y usa su propio contexto de título, sinopsis y protagonista.
  - La eliminación de páginas invalidadas deja el cuento sin las páginas indicadas y devuelve el número de páginas retiradas.
- **Prueba manual:**
  1. Con el guion pendiente de aprobación, pedir la portada y comprobar que se rechaza y se solicita la aprobación del guion.
  2. Aprobar el guion y pedir la portada y comprobar que se genera.
  3. Generar una página interior y comprobar que el texto generado sigue el hecho narrativo y los campos estructurados del beat correspondiente.
  4. Tras una edición directa confirmada, comprobar que las páginas invalidadas ya no aparecen en el cuento.

### [x] TASK-009 Eliminar el descubrimiento de personajes y el prompt asociado

- **Cubre:** REQ-005, REQ-014, DEC-003
- **Componente:** story-page-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageService.java
- **Depende de:** TASK-008
- **Alcance:** Eliminar el servicio de descubrimiento de personajes y la plantilla de prompt asociada, y retirar su invocación del flujo de generación y confirmación de páginas, de modo que el reparto secundario ligero no genere fichas ni entradas en el catálogo.
- **Hecho cuando:** no queda ningún servicio que detecte personajes nuevos en una escena ni que los genere con el agente director, la plantilla de descubrimiento de personajes deja de estar disponible para el flujo de páginas, y la generación de una página con secundarios en escena no crea personajes en el catálogo.
- **Pruebas automáticas:**
  - La generación de una página cuya escena incluye personajes no presentes en el reparto no crea ninguna entrada de personaje en el cuento.
  - La confirmación de la revisión de una página no genera ningún personaje adicional en el catálogo.
  - El listado de personajes del catálogo no incluye a los secundarios del reparto ligero.
- **Prueba manual:**
  1. Generar una página cuya escena mencione un personaje secundario y comprobar que no aparece en la pestaña de personajes del cuento.
  2. Revisar el catálogo de personajes y comprobar que no se ha creado ninguna ficha nueva.

### [x] TASK-010 Modificar la pantalla de creación de cuento para el protagonista obligatorio

- **Cubre:** REQ-001, REQ-020, ALT-001, DEC-005
- **Componente:** topic-selection-modal | app/src/components/TopicSelectionModal.vue
- **Depende de:** TASK-005
- **Alcance:** Modificar la pantalla de creación de cuento para mostrar el catálogo de personajes con retrato aprobado y una opción de dejar que el sistema elija al azar, y para avisar cuando no existe ningún protagonista apto.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-020, la pantalla ofrece la lista de protagonistas con retrato aprobado y la opción de elección al azar, y cuando el catálogo no tiene ningún protagonista apto muestra un aviso indicando que debe crearse y aprobarse uno.
- **Pruebas automáticas:**
  - No aplica: la verificación del flujo visual de la SPA requiere pruebas manuales hasta que el proyecto disponga de cobertura automatizada para este componente.
- **Prueba manual:**
  1. Iniciar la creación de un cuento con varios personajes con retrato aprobado y comprobar que aparecen listados y que existe la opción de elegir al azar.
  2. Elegir un protagonista concreto y crear el cuento, y comprobar que ese personaje es el protagonista del cuento creado.
  3. Elegir la opción al azar y crear el cuento, y comprobar que el cuento tiene un protagonista con retrato aprobado.
  4. Con el catálogo sin retratos aprobados, iniciar la creación de un cuento y comprobar que aparece el aviso y no se puede crear.

### [x] TASK-011 Construir la vista de guion con escaleta, aprobación y edición de beats

- **Cubre:** REQ-007, REQ-008, REQ-010, REQ-011, REQ-012, REQ-017, REQ-018, REQ-019, ALT-003, ALT-005
- **Componente:** story-script-panel | app/src/components/StoryScriptPanel.vue
- **Depende de:** TASK-005, TASK-006, TASK-007
- **Alcance:** Construir la vista del guion que muestra título, sinopsis, reparto episódico y la escaleta completa página a página, con las acciones de aprobar y de enviar un hint global, la presentación de la escaleta candidata con los beats cambiados resaltados y el modo de edición directa de un beat con su aviso de invalidación de páginas.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-007, REQ-008, REQ-010, REQ-011, REQ-012, REQ-017, REQ-018 y REQ-019, la vista permite aprobar el guion, enviar un hint global, ver la escaleta completa con los beats cambiados distinguidos del resto y editar directamente el texto de un beat, y una propuesta sin cambios respecto a la versión anterior se indica como tal.
- **Pruebas automáticas:**
  - No aplica: la verificación del flujo visual de la SPA requiere pruebas manuales hasta que el proyecto disponga de cobertura automatizada para este componente.
- **Prueba manual:**
  1. Abrir un cuento con el guion pendiente de aprobación y comprobar que se muestran título, sinopsis, reparto con función narrativa y la escaleta completa.
  2. Pulsar "Aprobar" y comprobar que el guion pasa a estado aprobado.
  3. Enviar un hint global y comprobar que se presenta la escaleta completa con los beats cambiados distinguidos visualmente del resto.
  4. Enviar un hint global que no modifica ningún beat y comprobar que se indica que no hay cambios respecto a la versión anterior.
  5. Seleccionar un beat, editar su texto y guardarlo, y comprobar que el beat actualizado se refleja en la escaleta.
  6. Editar un beat con un texto no apto y comprobar que se muestra el motivo del rechazo y que el beat no cambia.
  7. Con un guion aprobado y páginas generadas, editar un beat y comprobar que aparece el aviso de invalidación de las páginas antes de aplicar el cambio.
  8. Cancelar el aviso y comprobar que no se aplica la edición y que las páginas permanecen.

### [x] TASK-012 Orquestar las pestañas del cuento según el estado del guion

- **Cubre:** REQ-006, REQ-016, REQ-017, DEC-005
- **Componente:** story-modal | app/src/components/StoryModal.vue
- **Depende de:** TASK-011
- **Alcance:** Modificar el modal principal del cuento para redirigir a la pantalla de guion mientras esté pendiente de aprobación y para mantener la vista anterior en los cuentos antiguos, decidiendo la representación mediante el marcador de versión de guion.
- **Hecho cuando:** al abrir un cuento nuevo con el guion pendiente de aprobación se muestra la pantalla de guion con la escaleta y sus acciones, y al abrir un cuento antiguo se mantiene su representación previa de guion y personajes sin escaleta.
- **Pruebas automáticas:**
  - No aplica: la verificación del flujo visual de la SPA requiere pruebas manuales hasta que el proyecto disponga de cobertura automatizada para este componente.
- **Prueba manual:**
  1. Abrir un cuento nuevo recién creado y comprobar que se muestra la pantalla de guion con la escaleta y las acciones de aprobación.
  2. Abrir un cuento creado antes de la feature y comprobar que se muestra la vista previa de guion y personajes, sin escaleta.
  3. Aprobar el guion de un cuento nuevo y comprobar que se habilita el acceso a la generación de páginas.

### [ ] TASK-013 Integrar el flujo completo de creación, aprobación y escritura

- **Cubre:** REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010, REQ-011, REQ-012, REQ-013, REQ-016, REQ-017, REQ-018, REQ-019, REQ-020, ALT-001, ALT-002, ALT-003, ALT-004, ALT-005, ALT-006, DEC-001, DEC-004, DEC-005, DEC-006
- **Componente:** story-modal | app/src/components/StoryModal.vue
- **Depende de:** TASK-005, TASK-006, TASK-007, TASK-008, TASK-009, TASK-010, TASK-011, TASK-012
- **Alcance:** Verificar de extremo a extremo la comunicación entre la pantalla de creación de cuento, el servicio de guion, el agente director, el repositorio de revisiones, el caso de uso de edición de beats, el servicio de páginas y el escritor, desde la creación del cuento con protagonista hasta la generación de la primera página con el beat aprobado como contrato.
- **Hecho cuando:** desde la creación del cuento hasta la primera página generada, el flujo entrega al director la ficha del protagonista, persiste una escaleta completa, exige la aprobación antes de escribir, permite la revisión global y la edición directa de un beat, y entrega al escritor el beat aprobado de la página generada.
- **Pruebas automáticas:**
  - No aplica: la verificación del flujo visual de la SPA requiere pruebas manuales hasta que el proyecto disponga de cobertura automatizada para este componente.
- **Prueba manual:**
  1. Crear un cuento eligiendo protagonista y comprobar que el guion se genera con escaleta completa y reparto ligero.
  2. Intentar generar la portada sin aprobar y comprobar que se rechaza.
  3. Aprobar el guion y generar la portada y una página interior, y comprobar que el texto de la página sigue el beat correspondiente.
  4. Enviar un hint global, ver la propuesta con los beats cambiados, aceptarla y comprobar que la escaleta vigente se actualiza y sigue pendiente de aprobación.
  5. Editar un beat y comprobar que el guion vuelve a pendiente y que la generación de páginas queda bloqueada hasta una nueva aprobación.

### [x] TASK-014 Contrato: openapi - esquemas de escaleta, reparto ligero y guion del cuento

- **Cubre:** REQ-003, REQ-004, REQ-005, REQ-016, DEC-005, DEC-006
- **Componente:** story-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryService.java
- **Depende de:** TASK-003, TASK-004, TASK-005
- **Alcance:** Crear o actualizar el contrato OpenAPI de los esquemas del guion en `docs/contracts/openapi/schemas/` y el documento raíz `docs/contracts/openapi/openapi.yaml`, con la escaleta de beats y sus cinco campos, el reparto episódico ligero y los esquemas de story, con el nombre del fichero por definir (analista técnico).
- **Hecho cuando:** el contrato existe en `docs/contracts/openapi/` y representa el beat con sus cinco campos obligatorios, el repartoEpisódico ligero con nombre y función narrativa, y el esquema del cuento con el estado del guion, el marcador de versión de guion, la escaleta y el reparto, tal como quedó implementada, y el documento raíz registra los nuevos esquemas y paths.
- **Pruebas automáticas:**
  - Los contratos OpenAPI afectados son válidos según las reglas de su formato.
  - El esquema del beat expone los cinco campos obligatorios de la escaleta.
  - El esquema del repartoEpisódico ligero expone nombre y función narrativa y no incluye identificador de catálogo.
  - El esquema del cuento expone el estado del guion y el marcador de versión de guion.
  - El esquema del cuento permite representar un cuento antiguo sin escaleta.
  - El documento raíz `openapi.yaml` registra los nuevos esquemas de escaleta y reparto ligero y los paths de aprobación y edición de beat.
- **Prueba manual:**
  1. Recorrer los esquemas y comprobar que la escaleta, el reparto ligero y el estado del guion están representados.
  2. Comparar los esquemas con la implementación y confirmar que los campos, el estado del guion y el marcador de versión coinciden.

### [x] TASK-015 Contrato: openapi - comando de generación de cuento con protagonista obligatorio

- **Cubre:** REQ-001, REQ-002, REQ-020, ALT-001
- **Componente:** story-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryService.java
- **Depende de:** TASK-005, TASK-010
- **Alcance:** Actualizar el contrato OpenAPI del comando de generación de cuento en `docs/contracts/openapi/schemas/requests/story-generate-command.yaml` para incorporar el identificador del protagonista del catálogo y la indicación de selección aleatoria.
- **Hecho cuando:** el contrato existe en `docs/contracts/openapi/` y describe el comando de creación de cuento con el protagonista del catálogo y la opción de selección aleatoria, tal como quedó implementada, incluyendo la respuesta de rechazo cuando no hay ningún protagonista apto.
- **Pruebas automáticas:**
  - El contrato OpenAPI afectado es válido según las reglas de su formato.
  - El comando admite el identificador del protagonista del catálogo y la indicación de selección aleatoria.
  - El contrato recoge la respuesta de rechazo cuando no existe ningún personaje del catálogo con retrato aprobado.
- **Prueba manual:**
  1. Recorrer el contrato y comprobar que aparecen el protagonista del catálogo y la selección aleatoria con su entrada y salida.
  2. Comparar el contrato con la implementación y confirmar que la creación con y sin protagonista explícito coinciden.

### [x] TASK-016 Contrato: openapi - revisión de guion con escaleta candidata

- **Cubre:** REQ-007, REQ-008, REQ-009, REQ-018, DEC-001
- **Componente:** story-review-repository | api/src/main/java/es/vargontoc/storyteller/application/ports/out/persistence/StoryReviewRepository.java
- **Depende de:** TASK-006
- **Alcance:** Actualizar el contrato OpenAPI de la revisión de guion en `docs/contracts/openapi/schemas/story-review.yaml` para sustituir o ampliar la previsualización de guion por la escaleta candidata completa, el conjunto de beats marcados como cambiados y el resultado de aceptación del hint con su motivo de rechazo.
- **Hecho cuando:** el contrato existe en `docs/contracts/openapi/` y describe la revisión de guion con la escaleta candidata, la lista de beats cambiados, el indicador de aceptación del hint y el motivo de rechazo, tal como quedó implementada.
- **Pruebas automáticas:**
  - El contrato OpenAPI afectado es válido según las reglas de su formato.
  - El esquema de la revisión expone la escaleta candidata completa y la lista de beats marcados como cambiados.
  - El esquema de la revisión expone el resultado de aceptación del hint y el motivo de rechazo.
  - El esquema de la revisión recoge la respuesta de rechazo por hint no válido.
- **Prueba manual:**
  1. Recorrer el esquema y comprobar que aparecen la escaleta candidata, los beats cambiados y el resultado del hint.
  2. Comparar el esquema con la implementación y confirmar que el ciclo de estados y los campos del hint coinciden.

### [x] TASK-017 Contrato: openapi - paths de guion, aprobación y edición de beat

- **Cubre:** REQ-006, REQ-007, REQ-008, REQ-009, REQ-010, REQ-011, REQ-012, REQ-013, REQ-017, REQ-018, REQ-019, ALT-005, ALT-006
- **Componente:** story-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryService.java
- **Depende de:** TASK-005, TASK-007, TASK-008
- **Alcance:** Crear o actualizar los contratos OpenAPI de los paths de guion en `docs/contracts/openapi/paths/story/`: generación del guion con protagonista del catálogo y escaleta, revisión global que devuelve la escaleta completa con aceptación o rechazo, consulta de la revisión pendiente con los beats cambiados, confirmación y descarte de la revisión, aprobación explícita del guion y edición directa de un beat con validación de edad incluida, con los nombres de fichero por definir (analista técnico).
- **Hecho cuando:** el contrato existe en `docs/contracts/openapi/` y describe cada interfaz de guion afectada con su método, entrada, salida y errores relevantes, tal como quedó implementada, incluidas las respuestas de rechazo exigidas por los requisitos.
- **Pruebas automáticas:**
  - Los contratos OpenAPI afectados son válidos según las reglas de su formato.
  - El contrato de generación de guion recoge que el director recibe el protagonista del catálogo y devuelve escaleta.
  - El contrato de revisión de guion recoge la escaleta completa devuelta y el resultado de aceptación o rechazo del hint.
  - El contrato de consulta de la revisión pendiente recoge la escaleta candidata y los beats marcados como cambiados.
  - El contrato de confirmación de revisión recoge la confirmación y el descarte de la revisión global de la escaleta.
  - El contrato recoge la interfaz de aprobación explícita del guion.
  - El contrato recoge la interfaz de edición directa de un beat y la respuesta de rechazo por incumplimiento de las reglas de edad.
  - El contrato de generación de página recoge la respuesta de rechazo cuando el guion no está aprobado.
- **Prueba manual:**
  1. Recorrer los paths de guion y comprobar que cada interfaz de esta tarea aparece con su método, entrada, salida y errores relevantes.
  2. Comparar los paths con la implementación y confirmar que generación, revisión, confirmación, aprobación y edición de beat coinciden.

### [x] TASK-018 Contrato: openapi - esquema de página y entrega del beat al escritor

- **Cubre:** REQ-013, DEC-006
- **Componente:** story-page-service | api/src/main/java/es/vargontoc/storyteller/application/service/StoryPageService.java
- **Depende de:** TASK-008
- **Alcance:** Actualizar el contrato OpenAPI del esquema de página en `docs/contracts/openapi/schemas/story-page.yaml` para recoger el contexto de generación ampliado, dejando constancia de que el beat aprobado se entrega internamente al escritor y no forma parte de la representación pública de la página.
- **Hecho cuando:** el contrato existe en `docs/contracts/openapi/` y representa la página con el contexto de generación vigente, tal como quedó implementada, y distingue el beat aprobado como contrato interno entre el escritor y la página persistida.
- **Pruebas automáticas:**
  - El contrato OpenAPI afectado es válido según las reglas de su formato.
  - El esquema de página no expone el beat como contenido editable por el cliente.
  - El contrato no introduce un campo público para el contrato del escritor.
- **Prueba manual:**
  1. Revisar el esquema de página y comprobar que refleja la implementación vigente.
  2. Comprobar que el beat aprobado no aparece como un campo de la página en el contrato público.

## Cobertura

| Requisito | Tareas |
|---|---|
| REQ-001 | TASK-005, TASK-010, TASK-013, TASK-015 |
| REQ-002 | TASK-005, TASK-013 |
| REQ-003 | TASK-001, TASK-003, TASK-004, TASK-005, TASK-013, TASK-014 |
| REQ-004 | TASK-001, TASK-003, TASK-004, TASK-005, TASK-013, TASK-014 |
| REQ-005 | TASK-001, TASK-003, TASK-004, TASK-005, TASK-009, TASK-013, TASK-014 |
| REQ-006 | TASK-005, TASK-008, TASK-012, TASK-013, TASK-017 |
| REQ-007 | TASK-005, TASK-011, TASK-013, TASK-016, TASK-017 |
| REQ-008 | TASK-001, TASK-006, TASK-011, TASK-013, TASK-016, TASK-017 |
| REQ-009 | TASK-001, TASK-005, TASK-013, TASK-016, TASK-017 |
| REQ-010 | TASK-007, TASK-011, TASK-013, TASK-017 |
| REQ-011 | TASK-007, TASK-008, TASK-011, TASK-013, TASK-017 |
| REQ-012 | TASK-007, TASK-008, TASK-011, TASK-013, TASK-017 |
| REQ-013 | TASK-008, TASK-013, TASK-018 |
| REQ-014 | TASK-005, TASK-009 |
| REQ-015 | TASK-002 |
| REQ-016 | TASK-003, TASK-004, TASK-012, TASK-013, TASK-014 |
| REQ-017 | TASK-011, TASK-012, TASK-013, TASK-017 |
| REQ-018 | TASK-006, TASK-011, TASK-013, TASK-016, TASK-017 |
| REQ-019 | TASK-007, TASK-011, TASK-013, TASK-017 |
| REQ-020 | TASK-010, TASK-013, TASK-015 |

## Avisos

- `.sdd/team.md` no define la opción `contract-strategy`; se aplica `implementation-first` por defecto, en línea con el criterio seguido en `character-inventory` y `character-portraits`. Las tareas de contrato TASK-014 a TASK-018 dependen de las tareas que implementan sus interfaces, y las de implementación no dependen de ellas.
- No existe `.sdd/task-types.md`, por lo que no se han generado tipos de tarea adicionales.
- Los componentes nuevos (`director-model-provisioner`, `story-outline-repository`, `beat-editor`, `story-script-panel`) no tienen ruta asignada: se marcan como "por definir (analista técnico)".
- El mapa de arquitectura sitúa el `director-agent` en `agents/director-story-agent/Modelfile`, pero DEC-002 traslada ese Modelfile a los recursos del backend y elimina esa carpeta. TASK-001 modifica el agente y TASK-002 ejecuta el traslado y el aprovisionamiento; la ruta definitiva corresponde al analista técnico.
- `character-catalog-service`, `writer-agent` y `character-agent` son componentes existentes que el diseño no modifica. No se generan tareas propias para ellos; su uso queda limitado a lo que el diseño les asigna.
- `writer-agent` no se modifica: TASK-008 solo entrega el beat aprobado al escritor como contrato, conforme a REQ-013 y SUP-003.
- DEC-003 elimina `DiscoverCharacterService` y el prompt `discover_characcter.st` (nombre real en el repositorio, con la errata de la nomenclatura original). TASK-009 cubre esa eliminación junto con la retirada de su invocación en `story-page-service`.
- No hay tarea de migración de cuentos antiguos: DEC-005 se limita al marcador de versión de guion, cubierto por TASK-003, TASK-004, TASK-012 y TASK-014.
- La política concreta de eliminación física de los assets de las páginas invalidadas no se define en esta feature (SUP-005): TASK-007 y TASK-008 se limitan a retirar las páginas del cuento y a aplicar las reglas de gestión de assets del producto.
- Las once filas de contratos afectados del diseño se formalizan en cinco tareas (TASK-014 a TASK-018): la fila de `openapi.yaml` y la de `schemas/story.yaml` quedan integradas en TASK-014 junto a los esquemas de escaleta y reparto ligero; las filas de `paths/story/generation-service.yaml`, `review-service.yaml`, `confirm-review-service.yaml` y `review-by-id-service.yaml` quedan en TASK-017; las dos filas de `paths/story/*.yaml` (aprobación del guion y edición de beat) se agrupan en TASK-017; y las filas de `schemas/requests/story-generate-command.yaml`, `schemas/story-review.yaml` y `schemas/story-page.yaml` dan lugar a TASK-015, TASK-016 y TASK-018.
- La fila de `schemas/story-page.yaml` del diseño es condicional ("posible ampliación"); TASK-018 se limita a recoger el resultado de la implementación sin inventar campos públicos.
- TASK-013 es una tarea de integración: su resultado verificable es la comunicación entre `story-modal`, `topic-selection-modal`, `story-script-panel`, `story-service`, `beat-editor`, `story-page-service` y `director-agent`.
- La verificación de los flujos de la SPA (TASK-010, TASK-011, TASK-012 y TASK-013) se apoya en pruebas manuales, de forma coherente con `character-inventory` y `character-portraits`, porque el proyecto no dispone de cobertura automatizada para el frontend.
- La comparación de los beats devueltos con los vigentes para marcar los realmente cambiados (mitigación del riesgo del diseño) se implementa en el backend dentro del flujo de revisión global, cubierto por TASK-006.
