# Requisitos: agent-director

Estado: aprobado
Revisión: 2

## Contexto

Storyteller Agent necesita renovar el flujo de creación del guion para que el agente director produzca una escaleta contractual página a página antes de que el escritor genere texto. Esta feature define el comportamiento del agente director, el modelo de escaleta, la puerta de aprobación, los mecanismos de revisión híbrida y la interfaz de usuario asociada.

## Fuera de alcance

- Generación del texto narrativo de las páginas: corresponde al escritor y esta feature solo fija el contrato de escaleta.
- Generación, selección o almacenamiento de retratos de personajes del catálogo.
- Generación o importación de ilustraciones de página.
- Definición de la política concreta de eliminación física de ficheros de assets; solo se especifica que las páginas invalidadas se eliminan del cuento y se aplican las reglas de gestión de assets del producto.
- Migración de cuentos creados con el formato anterior al nuevo modelo de escaleta contractual.
- Mecanismo técnico de aprovisionamiento del Modelfile del director en el servidor; solo se impone la restricción de mantenimiento de que deja de residir en `agents/`.

## Requisitos funcionales

### REQ-001 Protagonista obligatorio del catálogo

- **Requisito:** Cuando el usuario crea un cuento, el sistema deberá exigir un protagonista del catálogo con retrato aprobado; si el usuario no elige uno, el sistema escogerá uno al azar de entre los personajes del catálogo con retrato aprobado. No se podrá crear un cuento si no existe ningún personaje con retrato aprobado.
- **Criterios de aceptación:**
  - Dado un catálogo con al menos un personaje con retrato aprobado, cuando el usuario crea un cuento sin elegir protagonista, entonces el sistema asigna uno al azar de entre esos personajes.
  - Dado un catálogo vacío o sin personajes con retrato aprobado, cuando el usuario intenta crear un cuento, entonces el sistema impide la creación e informa de que debe existir al menos un protagonista con retrato aprobado.
  - Dado un cuento en creación, cuando el usuario elige un protagonista concreto, entonces el sistema lo utiliza como protagonista del cuento.

### REQ-002 Recibir ficha del protagonista

- **Requisito:** Cuando el director genera un guion, el sistema deberá proporcionarle la ficha textual y la referencia visual aprobada del protagonista elegido.
- **Criterios de aceptación:**
  - Dado un protagonista del catálogo, cuando el director inicia la generación del guion, entonces recibe nombre, descripción narrativa, descripción visual y referencia al retrato aprobado.
  - Dado un protagonista asignado al azar, cuando se genera el guion, entonces se conserva en el cuento la instantánea de ese personaje con su ficha y retrato aprobado en el momento de la generación.

### REQ-003 Generar guion completo

- **Requisito:** Cuando el usuario confirma tema, tamaño y protagonista, el sistema deberá pedir al director un guion completo con título, sinopsis, reparto episódico y escaleta.
- **Criterios de aceptación:**
  - Dados tema, tamaño y protagonista, cuando se solicita generar el guion, entonces el director devuelve título, sinopsis, reparto episódico y escaleta.
  - Dado un guion generado, cuando el usuario consulta el guion, entonces visualiza título, sinopsis, reparto episódico y escaleta.

### REQ-004 Escaleta estructurada por página

- **Requisito:** El sistema deberá exigir que cada beat de la escaleta incluya qué ocurre, función narrativa, quiénes aparecen, emoción o tono, y nota visual.
- **Criterios de aceptación:**
  - Dado un guion generado, cuando se inspecciona un beat, entonces contiene los cinco campos obligatorios.
  - Dado un beat incompleto devuelto por el agente, cuando se valida, entonces se rechaza y se solicita una nueva generación.

### REQ-005 Reparto secundario ligero

- **Requisito:** El sistema deberá permitir que el director incluya en el reparto episódico personajes secundarios con nombre y función narrativa, sin ficha visual ni persistencia en el catálogo.
- **Criterios de aceptación:**
  - Dado un guion con personajes secundarios, cuando se consulta el reparto episódico, entonces aparecen nombre y función narrativa de cada secundario.
  - Dado un personaje secundario del reparto episódico, cuando se intenta consultar su ficha visual del catálogo, entonces el sistema indica que no existe.

### REQ-006 Puerta de aprobación del guion

- **Requisito:** El sistema deberá impedir generar la primera página de un cuento mientras el guion no esté en estado aprobado.
- **Criterios de aceptación:**
  - Dado un cuento cuyo guion está pendiente de aprobación, cuando se solicita generar la portada o la primera página, entonces el sistema rechaza la operación y solicita la aprobación del guion.
  - Dado un cuento cuyo guion está aprobado, cuando se solicita generar la portada, entonces el sistema permite continuar.

### REQ-007 Aprobar o solicitar cambios del guion

- **Requisito:** Cuando el usuario revisa el guion generado, el sistema deberá permitir aprobarlo o solicitar cambios mediante un hint global.
- **Criterios de aceptación:**
  - Dado un guion pendiente de aprobación, cuando el usuario pulsa aprobar, entonces el guion pasa a estado aprobado.
  - Dado un guion pendiente de aprobación, cuando el usuario escribe un hint global, entonces se inicia una revisión del guion.

### REQ-008 Hint global con escaleta completa

- **Requisito:** Cuando el usuario envía un hint global, el sistema deberá pedir al director una escaleta completa revisada, preservando inalterados los beats no implicados por el hint y marcando los beats que cambian.
- **Criterios de aceptación:**
  - Dado un hint que afecta solo al final del cuento, cuando se recibe la escaleta revisada, entonces los beats iniciales son idénticos a los anteriores y los beats finales reflejan el cambio.
  - Dado un hint global, cuando se presenta la propuesta, entonces se visualiza la escaleta completa con indicación de qué beats han cambiado respecto a la versión anterior.

### REQ-009 Aceptación o rechazo del hint global

- **Requisito:** Cuando el director procesa un hint global, el sistema deberá devolver una propuesta aceptada o un rechazo con motivo.
- **Criterios de aceptación:**
  - Dado un hint válido según las reglas del producto, cuando el director responde, entonces se genera una revisión pendiente con la escaleta propuesta.
  - Dado un hint que viola las reglas de edad o la coherencia del cuento, cuando el director responde, entonces se indica que el hint no se acepta y se muestra el motivo del rechazo.

### REQ-010 Edición directa de un beat

- **Requisito:** El sistema deberá permitir editar directamente el texto de un beat concreto de la escaleta, validando el cambio contra las reglas de edad antes de guardarlo.
- **Criterios de aceptación:**
  - Dado un beat visible en la escaleta, cuando el usuario edita su texto, entonces el sistema valida el cambio contra las reglas de edad.
  - Dado un texto editado que cumple las reglas de edad, cuando se guarda, entonces el beat actualizado se refleja en la escaleta.
  - Dado un texto editado que incumple las reglas de edad, cuando se guarda, entonces el sistema rechaza el cambio y muestra el motivo.

### REQ-011 Edición manual invalida la aprobación

- **Requisito:** Cuando el usuario edita manualmente un beat de un guion aprobado, el sistema deberá volver el guion a estado pendiente de aprobación y bloquear la generación de nuevas páginas hasta una nueva aprobación.
- **Criterios de aceptación:**
  - Dado un guion aprobado, cuando el usuario guarda una edición manual de un beat, entonces el estado del guion pasa a pendiente de aprobación.
  - Dado un guion que ha vuelto a pendiente de aprobación por edición manual, cuando se solicita generar una nueva página, entonces el sistema rechaza la operación.

### REQ-012 Aviso de invalidación de páginas generadas

- **Requisito:** Si un guion aprobado tiene páginas ya generadas y el usuario edita manualmente un beat, el sistema deberá avisar de que esas páginas quedan invalidadas y se eliminarán antes de aplicar la edición.
- **Criterios de aceptación:**
  - Dado un guion aprobado con páginas generadas, cuando el usuario inicia la edición manual de un beat, entonces el sistema muestra un aviso de que las páginas existentes se invalidarán y eliminarán.
  - Dado el aviso de invalidación, cuando el usuario confirma, entonces se aplica la edición, se eliminan las páginas generadas del cuento y se aplican las reglas de gestión de assets del producto a sus assets asociados.
  - Dado el aviso de invalidación, cuando el usuario cancela, entonces no se aplica la edición, no cambia el estado del guion y las páginas permanecen.

### REQ-013 Mantener contrato con el escritor

- **Requisito:** El sistema deberá garantizar que la escaleta aprobada se entrega al escritor como contrato: el escritor narrará el hecho de cada beat sin alterar la función narrativa, los personajes que aparecen, la emoción o tono ni la nota visual.
- **Criterios de aceptación:**
  - Dado un guion aprobado, cuando se genera una página, entonces el escritor recibe el beat correspondiente de la escaleta aprobada.
  - Dado un beat aprobado, cuando el escritor genera texto, entonces respeta el hecho narrativo y los campos estructurados del beat.

### REQ-014 Revisión de personajes fuera del director

- **Requisito:** El sistema no deberá utilizar el agente director para revisar fichas de personajes del catálogo; esa responsabilidad corresponde al agente de personaje.
- **Criterios de aceptación:**
  - Dado un cambio solicitado sobre la ficha de un personaje del catálogo, cuando se procesa, entonces interviene el agente de personaje y no el agente director.
  - Dado un cuento en generación, cuando el director recibe el protagonista, entonces lo utiliza tal cual y no lo modifica.

### REQ-015 Modelfile del director en el servidor

- **Requisito:** El sistema deberá mantener el Modelfile del agente director desde el backend, dejando de utilizar la carpeta `agents/` como ubicación de mantenimiento.
- **Criterios de aceptación:**
  - Dado un despliegue del backend, cuando se consulta la configuración del agente director, entonces el Modelfile se carga o aprovisiona desde recursos gestionados por el backend.
  - Dado el repositorio tras esta feature, cuando se revisa la ubicación del Modelfile del director, entonces ya no reside en `agents/director-story-agent/`.

### REQ-016 Sin migración de cuentos antiguos

- **Requisito:** El sistema deberá conservar los cuentos creados con el formato anterior sin migrarlos al nuevo modelo de escaleta contractual.
- **Criterios de aceptación:**
  - Dado un cuento creado antes de esta feature, cuando se consulta, entonces se mantiene su estructura original de guion y personajes.
  - Dado un cuento nuevo creado tras esta feature, cuando se consulta su guion, entonces utiliza el modelo de escaleta contractual y reparto secundario ligero.

### REQ-017 Pantalla de aprobación del guion

- **Requisito:** Cuando el usuario abre un cuento cuyo guion está pendiente de aprobación, el sistema deberá mostrar la escaleta completa y las acciones para aprobar o solicitar cambios.
- **Criterios de aceptación:**
  - Dado un cuento cuyo guion está pendiente de aprobación, cuando el usuario abre su detalle, entonces visualiza título, sinopsis, reparto episódico y escaleta página a página.
  - Dado un cuento cuyo guion está pendiente de aprobación, cuando el usuario visualiza el guion, entonces dispone de acciones para aprobarlo o enviar un hint global.

### REQ-018 Indicador de cambios en la revisión global

- **Requisito:** Cuando se presenta una propuesta de revisión global, el sistema deberá resaltar los beats que han cambiado respecto a la versión anterior.
- **Criterios de aceptación:**
  - Dado un hint global que modifica uno o varios beats, cuando se muestra la propuesta, entonces los beats cambiados se distinguen visualmente de los beats inalterados.
  - Dado un hint global que no modifica ningún beat, cuando se muestra la propuesta, entonces se indica que no hay cambios respecto a la versión anterior.

### REQ-019 Modo de edición directa de beat

- **Requisito:** Cuando el usuario selecciona un beat de la escaleta, el sistema deberá permitir editar su texto de forma directa.
- **Criterios de aceptación:**
  - Dado un beat visible en la escaleta, cuando el usuario lo selecciona, entonces el sistema le permite editar el texto del beat.
  - Dado un beat en edición, cuando el usuario guarda el cambio, entonces el sistema lo valida y, si es válido, actualiza el beat.

### REQ-020 Selección de protagonista al crear cuento

- **Requisito:** Cuando el usuario crea un cuento, el sistema deberá mostrar el catálogo de personajes con retrato aprobado y una opción para dejar que el sistema elija al azar.
- **Criterios de aceptación:**
  - Dado un catálogo con personajes con retrato aprobado, cuando el usuario inicia la creación de un cuento, entonces ve la lista de protagonistas disponibles y la opción de elegir uno al azar.
  - Dado un catálogo sin personajes con retrato aprobado, cuando el usuario inicia la creación de un cuento, entonces no aparece la opción de protagonista o se muestra un aviso indicando que debe crear uno.

## Requisitos no funcionales

Ninguno.

## Supuestos

### SUP-001 Mantenimiento del Modelfile desde el backend

- **Tipo:** contextual
- **Descripción:** El Modelfile del director deja de mantenerse en `agents/director-story-agent/` y pasa a gestionarse desde el backend, de forma análoga al agente de personaje. El mecanismo concreto de aprovisionamiento queda para la fase de diseño.
- **Impacto:** REQ-015.
- **Estado:** validado

### SUP-002 Agente de personaje gestiona revisiones de personajes

- **Tipo:** contextual
- **Descripción:** El agente de personaje ya existe y es responsable de generar y revisar las fichas de personajes del catálogo.
- **Impacto:** REQ-014, REQ-002.
- **Estado:** validado

### SUP-003 Escritor fuera de alcance

- **Tipo:** contextual
- **Descripción:** Esta feature no modifica el escritor; solo fija el contrato de escaleta que el escritor deberá cumplir en generaciones futuras.
- **Impacto:** REQ-013.
- **Estado:** validado

### SUP-004 Sin migración de cuentos existentes

- **Tipo:** decisional
- **Descripción:** Los cuentos creados con el formato anterior no se migran; el nuevo flujo de escaleta contractual y reparto secundario ligero aplica solo a cuentos nuevos.
- **Impacto:** REQ-016.
- **Estado:** validado

### SUP-005 Política de assets de páginas invalidadas

- **Tipo:** contextual
- **Descripción:** La política concreta de eliminación física de imágenes, audio y otros assets de páginas invalidadas no se define en esta feature; solo se especifica que las páginas se eliminan del cuento y se aplican las reglas de gestión de assets del producto.
- **Impacto:** REQ-012.
- **Estado:** validado

### SUP-006 Protagonista obligatorio del catálogo

- **Tipo:** decisional
- **Descripción:** El protagonista deja de ser opcional: todo cuento nuevo debe crearse con un protagonista del catálogo que tenga retrato aprobado, elegido por el usuario o asignado al azar. Esto modifica el comportamiento anterior del producto, donde el protagonista era opcional y un catálogo vacío era un estado inicial válido (PDEC-006).
- **Impacto:** REQ-001, REQ-002, REQ-003 y REQ-020.
- **Estado:** validado

## Preguntas abiertas

Ninguna.
