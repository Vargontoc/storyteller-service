# Requisitos: agent-narrator

Estado: aprobado
Revisión: 1

## Contexto

El adulto quiere narrar en español los cuentos con escaleta, con una voz que elige y una expresividad controlada (tono y énfasis), y obtener un audio por cada página interior para reproducir solo la página que se está viendo (FUN-017, evolución de FUN-004). Para que el audio diga lo mismo que el texto, el escritor incorpora las onomatopeyas en el texto de la página con reglas de uso acotadas (PDEC-025, PDEC-030).

## Fuera de alcance

- Narración de cuentos antiguos (sin escaleta): conservan el comportamiento actual y no reciben esta funcionalidad.
- Pista de audio completa del cuento y reproducción automática encadenada de páginas (PDEC-028).
- Narración de la portada: no tiene texto y no se narra (PDEC-026).
- Mostrar, editar o aprobar el guion de audio (tono y énfasis) que prepara el narrador.
- Regenerar o corregir el audio de una sola página; la única corrección es renarrar el cuento entero.
- Paso de aceptación del audio narrado.
- Añadir onomatopeyas, efectos o sonidos solo en el audio que no estén en el texto (PDEC-025).
- Alterar el texto aceptado de las páginas durante la narración, incluido su número de palabras.
- Cambiar el límite de palabras ni la validación de longitud de página existentes.
- Motor Chatterbox Turbo y finetune es-ES (PDEC-027).
- Editar o renombrar una voz ya subida.
- Borrar o sustituir desde la aplicación la voz del sistema.

## Requisitos funcionales

### REQ-001 Máximo una onomatopeya por página

- **Requisito:** Cuando el escritor redacte una página interior, el sistema deberá producir un texto con como máximo una onomatopeya.
- **Criterios de aceptación:**
  - Dado un cuento con escaleta aprobada, cuando se genera cualquier página interior, entonces su texto contiene cero o una onomatopeya, nunca dos o más.

### REQ-002 Onomatopeya solo con sonido natural en la escena

- **Requisito:** Cuando el escritor redacte una página interior cuyo beat no contenga un sonido natural en la escena, el sistema deberá producir un texto sin onomatopeyas.
- **Criterios de aceptación:**
  - Dado un beat en el que no ocurre nada que suene (por ejemplo, el protagonista mira las estrellas en silencio), cuando se genera la página, entonces su texto no contiene ninguna onomatopeya.
  - Dado un beat con un sonido natural en la escena (por ejemplo, un perro que ladra), cuando se genera la página y contiene una onomatopeya, entonces esta corresponde a ese sonido.

### REQ-003 Forma estándar y pronunciable

- **Requisito:** Cuando el escritor incluya una onomatopeya en una página interior, el sistema deberá escribirla en una forma estándar del español y pronunciable al leerla en voz alta.
- **Criterios de aceptación:**
  - Dada una página interior con onomatopeya, cuando un revisor la lee, entonces es una forma reconocible en español (por ejemplo, «guau», «miau», «pum», «chof») y no una secuencia de letras repetidas sin forma estándar (por ejemplo, «grrrrrrrr» o «zzzzzz» alargados) ni un símbolo.

### REQ-004 Sin onomatopeyas en la portada

- **Requisito:** Cuando el escritor genere la portada, el sistema deberá producirla sin ninguna onomatopeya.
- **Criterios de aceptación:**
  - Dado un cuento con escaleta aprobada, cuando se genera la portada, entonces su contenido no incluye ninguna onomatopeya.

### REQ-005 La onomatopeya cuenta en el límite de palabras

- **Requisito:** El sistema deberá contar la onomatopeya de una página interior como palabra dentro del límite de palabras vigente de esa página (objetivo de producto de 60 palabras, PDEC-030).
- **Criterios de aceptación:**
  - Dada una página interior con una onomatopeya, cuando se evalúa su longitud, entonces el recuento de palabras incluye la onomatopeya y el aviso de longitud se calcula igual que para cualquier otra palabra.

### REQ-006 Subida de voz en WAV o MP3

- **Requisito:** Cuando el adulto suba una voz, el sistema deberá aceptar solo ficheros de audio en formato WAV o MP3.
- **Criterios de aceptación:**
  - Dado un fichero WAV o MP3 válido que cumple el resto de condiciones, cuando el adulto lo sube, entonces la voz aparece en la biblioteca de voces.
  - Dado un fichero en otro formato (por ejemplo, OGG o un documento de texto), cuando el adulto lo sube, entonces el sistema rechaza la subida con un aviso que indica los formatos admitidos y la voz no aparece en la biblioteca.

### REQ-007 Duración de la voz entre 5 y 30 segundos

- **Requisito:** Si el audio de la voz subida dura menos de 5 segundos o más de 30 segundos, entonces el sistema deberá rechazar la subida con un aviso que indique el rango admitido.
- **Criterios de aceptación:**
  - Dado un audio de 4 segundos, cuando el adulto lo sube, entonces se rechaza con aviso del rango de 5 a 30 segundos.
  - Dado un audio de 31 segundos, cuando el adulto lo sube, entonces se rechaza con el mismo aviso.
  - Dado un audio de 5 segundos o de 30 segundos que cumple el resto de condiciones, cuando el adulto lo sube, entonces se acepta.

### REQ-008 Idioma obligatorio con español preseleccionado

- **Requisito:** Cuando el adulto abra la subida de una voz, el sistema deberá exigir un idioma para la voz y mostrar español preseleccionado.
- **Criterios de aceptación:**
  - Dado el formulario de subida recién abierto, cuando el adulto lo observa, entonces el idioma aparece con español seleccionado.
  - Dado el formulario de subida sin idioma seleccionado, cuando el adulto intenta subir la voz, entonces el sistema rechaza la subida con un aviso de idioma obligatorio.
  - Dada una voz subida, cuando el adulto consulta la biblioteca, entonces se muestra el idioma declarado de esa voz.

### REQ-009 Declaración de permiso obligatoria

- **Requisito:** Si el adulto intenta subir una voz sin marcar la casilla de declaración de que tiene permiso para usar esa voz, entonces el sistema deberá rechazar la subida con un aviso.
- **Criterios de aceptación:**
  - Dado el formulario de subida con la casilla de permiso sin marcar, cuando el adulto intenta subir la voz, entonces se rechaza con aviso y la voz no aparece en la biblioteca.
  - Dado el formulario con la casilla marcada y el resto de condiciones cumplidas, cuando el adulto sube la voz, entonces se acepta.

### REQ-010 Nombre único por voz

- **Requisito:** Si el adulto sube una voz con un nombre que ya tiene otra voz de la biblioteca, incluida la voz del sistema, entonces el sistema deberá rechazar la subida con un aviso de nombre duplicado.
- **Criterios de aceptación:**
  - Dada una voz llamada «Abuela» en la biblioteca, cuando el adulto sube otra voz con el nombre «Abuela», entonces se rechaza con aviso de nombre duplicado y la biblioteca no cambia.
  - Dado un formulario sin nombre, cuando el adulto intenta subir la voz, entonces se rechaza con aviso de nombre obligatorio.

### REQ-011 Biblioteca de voces

- **Requisito:** El sistema deberá mostrar al adulto la biblioteca de voces con la voz del sistema y todas las voces subidas no borradas, indicando de cada una su nombre, su idioma y si es la voz del sistema.
- **Criterios de aceptación:**
  - Dada la voz del sistema y dos voces subidas, cuando el adulto consulta la biblioteca, entonces ve las tres voces con nombre e idioma, y la voz del sistema identificada como tal.

### REQ-012 Voz del sistema configurable

- **Requisito:** El sistema deberá tomar el nombre y el audio de referencia de la voz del sistema de la configuración de la instalación, sin un valor fijado en estos requisitos.
- **Criterios de aceptación:**
  - Dada una instalación en la que se configura una voz del sistema con un nombre y un audio de referencia, cuando el adulto consulta la biblioteca de voces, entonces la voz del sistema aparece con el nombre configurado.
  - Dada la configuración cambiada a otro nombre y audio y la aplicación reiniciada, cuando el adulto consulta la biblioteca, entonces la voz del sistema aparece con el nuevo nombre.

### REQ-013 Voz del sistema no borrable

- **Requisito:** El sistema deberá impedir que el adulto borre la voz del sistema.
- **Criterios de aceptación:**
  - Dada la biblioteca de voces, cuando el adulto intenta borrar la voz del sistema, entonces no existe la opción de borrado o la operación se rechaza con aviso, y la voz sigue en la biblioteca.

### REQ-014 Borrado de voces subidas

- **Requisito:** Cuando el adulto borre una voz subida, el sistema deberá retirarla de la biblioteca de voces.
- **Criterios de aceptación:**
  - Dada una voz subida, cuando el adulto la borra, entonces deja de aparecer en la biblioteca.

### REQ-015 El borrado no afecta al audio ya generado

- **Requisito:** Cuando el adulto borre una voz subida que se usó para narrar uno o más cuentos, el sistema deberá conservar el audio de esos cuentos y el nombre de la voz con la que se narraron.
- **Criterios de aceptación:**
  - Dado un cuento narrado con la voz «Abuela», cuando el adulto borra la voz «Abuela», entonces el audio de todas las páginas interiores del cuento sigue reproduciéndose y el cuento sigue indicando que se narró con «Abuela».

### REQ-016 Voz borrada no elegible

- **Requisito:** Cuando el adulto elija voz para narrar o renarrar un cuento, el sistema deberá excluir de las opciones las voces borradas.
- **Criterios de aceptación:**
  - Dado un cuento narrado con una voz que después se ha borrado, cuando el adulto abre la renarración, entonces esa voz no aparece entre las opciones.

### REQ-017 Narración solo en cuentos con escaleta

- **Requisito:** El sistema deberá ofrecer la narración solo en cuentos con escaleta.
- **Criterios de aceptación:**
  - Dado un cuento antiguo sin escaleta, cuando el adulto lo abre, entonces no se ofrece la narración de esta funcionalidad.

### REQ-018 Narración habilitada con todas las páginas interiores aceptadas

- **Requisito:** Mientras alguna página interior de un cuento con escaleta no esté generada y aceptada, el sistema deberá mantener deshabilitada la narración de ese cuento.
- **Criterios de aceptación:**
  - Dado un cuento con escaleta de 8 páginas interiores y 7 aceptadas, cuando el adulto abre el cuento, entonces la acción de narrar aparece deshabilitada con la indicación de que faltan páginas por aceptar.
  - Dado el mismo cuento con las 8 páginas interiores aceptadas, con independencia del estado de la portada, cuando el adulto abre el cuento, entonces la acción de narrar está habilitada.

### REQ-019 Elección de voz al narrar

- **Requisito:** Cuando el adulto inicie la narración de un cuento, el sistema deberá pedirle que elija una voz, con la voz del sistema preseleccionada.
- **Criterios de aceptación:**
  - Dado un cuento con todas sus páginas interiores aceptadas, cuando el adulto pulsa narrar, entonces se le muestran las voces elegibles con la voz del sistema seleccionada y puede cambiarla por otra antes de confirmar.

### REQ-020 Solo voces en español elegibles

- **Requisito:** Cuando el adulto elija voz para narrar o renarrar, el sistema deberá ofrecer como elegibles solo las voces cuyo idioma sea español.
- **Criterios de aceptación:**
  - Dada una voz subida con idioma inglés y otra con idioma español, cuando el adulto elige voz para narrar, entonces solo la voz en español (y la voz del sistema) aparece como elegible.

### REQ-021 Turno único de generación al narrar

- **Requisito:** Cuando el adulto confirme la narración o la renarración de un cuento, el sistema deberá solicitar el turno único de generación de la aplicación antes de empezar.
- **Criterios de aceptación:**
  - Dado que no hay ninguna generación en curso, cuando el adulto confirma la narración, entonces la narración empieza y cualquier otra generación que se intente mientras dura se rechaza con el aviso habitual de generación en curso.
  - Dada una narración terminada, con éxito o con error, cuando el adulto intenta otra generación, entonces el turno está libre y la generación se acepta.

### REQ-022 Rechazo si hay otra generación en curso

- **Requisito:** Si hay otra generación en curso en la aplicación cuando el adulto confirma la narración o la renarración, entonces el sistema deberá rechazarla con un aviso de generación en curso, sin modificar el audio existente del cuento.
- **Criterios de aceptación:**
  - Dada una generación de retratos en curso, cuando el adulto confirma la narración de un cuento, entonces se muestra el aviso de generación en curso, no empieza la narración y el audio previo del cuento, si existía, sigue intacto.

### REQ-023 Guion de audio a partir del cuento entero

- **Requisito:** Cuando empiece la narración de un cuento, el sistema deberá hacer que el narrador lea el texto aceptado de todas las páginas interiores y prepare para cada una un guion de audio con indicaciones de tono y énfasis.
- **Criterios de aceptación:**
  - Dado un cuento cuya página 3 es emotiva y la página 5 es tranquila, cuando se narra, entonces al escuchar el audio la expresividad de cada página es acorde con su contenido y no idéntica en todas las páginas.

### REQ-024 El narrador no altera el texto

- **Requisito:** El sistema deberá narrar en cada página interior exactamente el texto aceptado de esa página, sin añadir, quitar, reordenar ni sustituir palabras.
- **Criterios de aceptación:**
  - Dada una página interior con su texto aceptado, cuando el adulto escucha su audio, entonces oye las mismas palabras en el mismo orden que el texto, incluida la onomatopeya si la hay.
  - Dado un cuento narrado, cuando el adulto consulta el texto de sus páginas, entonces el texto aceptado es idéntico al de antes de narrar.

### REQ-025 Guion de audio no visible

- **Requisito:** El sistema deberá usar el guion de audio sin mostrarlo al adulto ni pedirle su aprobación.
- **Criterios de aceptación:**
  - Dado un cuento en narración, cuando el adulto sigue el proceso hasta el final, entonces en ningún momento se le muestra el guion de audio ni se le pide aprobarlo.

### REQ-026 Síntesis de todas las páginas interiores de una vez

- **Requisito:** Cuando el adulto confirme la narración, el sistema deberá sintetizar en español, con la voz elegida, un audio por cada página interior del cuento en una única operación, sin pedir confirmación página a página.
- **Criterios de aceptación:**
  - Dado un cuento con 8 páginas interiores aceptadas, cuando el adulto confirma la narración y termina con éxito, entonces existen 8 audios, uno por página interior, y ningún audio para la portada.
  - Dada la narración terminada, cuando el adulto escucha cualquier página, entonces el audio está en español y con la voz elegida.

### REQ-027 Narración en curso visible

- **Requisito:** Mientras la narración de un cuento esté en curso, el sistema deberá indicar en ese cuento que se está narrando y mantener deshabilitadas las acciones de narrar y renarrar.
- **Criterios de aceptación:**
  - Dado un cuento en narración, cuando el adulto lo consulta, entonces ve el indicador de narración en curso y no puede lanzar otra narración.
  - Dada la narración terminada, cuando el adulto consulta el cuento, entonces el indicador desaparece y se muestra el resultado (audio disponible o aviso de error).

### REQ-028 Error durante la narración

- **Requisito:** Si la narración falla en cualquier página, entonces el sistema deberá dejar el cuento sin audio, avisar al adulto del error y liberar el turno de generación.
- **Criterios de aceptación:**
  - Dado un cuento cuya síntesis falla en la página 4 de 8, cuando termina la narración con error, entonces el adulto ve un aviso de error, ninguna página tiene audio y el adulto puede volver a narrar el cuento.

### REQ-029 Voz guardada con el cuento

- **Requisito:** Cuando la narración termine con éxito, el sistema deberá guardar con el cuento el nombre de la voz usada y mostrarlo al adulto.
- **Criterios de aceptación:**
  - Dado un cuento narrado con la voz «Abuela», cuando el adulto consulta el cuento, entonces se indica que está narrado con «Abuela».

### REQ-030 Escucha por página

- **Requisito:** Mientras un cuento tenga audio, el sistema deberá permitir al adulto reproducir el audio de cada página interior de forma individual desde esa página.
- **Criterios de aceptación:**
  - Dado un cuento narrado, cuando el adulto está en la página 2 y pulsa reproducir, entonces suena solo el audio de la página 2 y al terminar no empieza el de la página 3.

### REQ-031 Renarrar el cuento entero

- **Requisito:** Mientras un cuento tenga audio y todas sus páginas interiores estén aceptadas, el sistema deberá permitir al adulto renarrar el cuento entero con la misma voz u otra voz elegible.
- **Criterios de aceptación:**
  - Dado un cuento narrado con la voz «Abuela», cuando el adulto elige renarrar, entonces puede elegir «Abuela» u otra voz elegible, con la voz del sistema preseleccionada.

### REQ-032 Aviso de sustitución al renarrar

- **Requisito:** Cuando el adulto pida renarrar un cuento, el sistema deberá avisarle de que se sustituirá todo el audio del cuento y pedir confirmación antes de empezar.
- **Criterios de aceptación:**
  - Dado un cuento narrado, cuando el adulto pide renarrar y cancela el aviso, entonces no empieza ninguna narración y el audio y la voz guardada siguen intactos.
  - Dado un cuento narrado, cuando el adulto confirma el aviso y la renarración termina con éxito, entonces todas las páginas interiores tienen el audio nuevo y el cuento indica la voz nueva.

### REQ-033 Cambio de página interior borra el audio

- **Requisito:** Cuando el adulto reabra o cambie una página interior de un cuento que tiene audio, el sistema deberá avisarle de que se borrará todo el audio del cuento y, tras su confirmación, borrar el audio de todas las páginas y la voz guardada.
- **Criterios de aceptación:**
  - Dado un cuento narrado, cuando el adulto pide reabrir la página 5, entonces el aviso indica que se borrará todo el audio del cuento, además del descarte en cascada habitual.
  - Dado el mismo caso, cuando el adulto confirma, entonces ninguna página del cuento tiene audio y el cuento ya no indica voz de narración.
  - Dado el mismo caso, cuando el adulto cancela, entonces la página no se reabre y el audio sigue intacto.

### REQ-034 Narración deshabilitada tras un cambio hasta reaceptar

- **Requisito:** Cuando se borre el audio de un cuento por reabrir o cambiar una página interior, el sistema deberá mantener deshabilitada la narración hasta que todas las páginas interiores vuelvan a estar aceptadas.
- **Criterios de aceptación:**
  - Dado un cuento al que se ha borrado el audio por reabrir la página 5, cuando las páginas 5 a 8 están pendientes, entonces narrar aparece deshabilitado.
  - Dado el mismo cuento con todas sus páginas interiores aceptadas de nuevo, cuando el adulto lo abre, entonces narrar vuelve a estar habilitado.

## Requisitos no funcionales

Ninguno.

## Supuestos

### SUP-001 Biblioteca de voces única para la aplicación

- **Tipo:** contextual
- **Descripción:** La aplicación no tiene cuentas de usuario; la biblioteca de voces es única y compartida por todos los cuentos.
- **Impacto:** Alcance de la unicidad del nombre (REQ-010) y de la biblioteca (REQ-011).
- **Estado:** pendiente

### SUP-002 Fallo de narración sin audio parcial

- **Tipo:** decisional
- **Descripción:** Si la síntesis falla en cualquier página, no se conserva audio parcial: el cuento queda sin audio y el adulto puede volver a narrar entero (REQ-028).
- **Impacto:** Comportamiento ante errores y criterios de REQ-028.
- **Estado:** validado

### SUP-003 La renarración confirmada descarta el audio previo aunque falle

- **Tipo:** decisional
- **Descripción:** Tras confirmar el aviso de sustitución (REQ-032), el audio anterior ya no se conserva: si la renarración falla, el cuento queda sin audio como en REQ-028, en lugar de recuperar el audio anterior.
- **Impacto:** Resultado de una renarración fallida y qué voz figura en el cuento después del fallo.
- **Estado:** validado

### SUP-004 Páginas bloqueadas durante la narración

- **Tipo:** decisional
- **Descripción:** Mientras la narración de un cuento está en curso, no se permite reabrir ni cambiar sus páginas interiores; el sistema lo rechaza con aviso de narración en curso.
- **Impacto:** Evita narrar texto que cambia a mitad de la síntesis; complementa REQ-027 y REQ-033.
- **Estado:** validado

### SUP-005 Voz del sistema ausente o inválida

- **Tipo:** decisional
- **Descripción:** Si la voz del sistema no está configurada o su audio de referencia no es válido, la aplicación sigue funcionando, avisa de ello en la biblioteca de voces y no la ofrece al narrar; si no hay ninguna voz en español elegible, narrar se rechaza con aviso.
- **Impacto:** Comportamiento de REQ-012, REQ-019 y REQ-020 cuando falta la configuración.
- **Estado:** validado

### SUP-006 Voz del sistema en español

- **Tipo:** contextual
- **Descripción:** La voz del sistema configurada es una voz en español y, por tanto, siempre elegible para narrar.
- **Impacto:** Preselección de REQ-019 y filtro de REQ-020.
- **Estado:** pendiente

### SUP-007 Motor de síntesis acordado

- **Tipo:** contextual
- **Descripción:** La síntesis usa el motor acordado en PDEC-027 (Chatterbox Multilingual V3 en español), que admite clonación por audio de referencia y controles de expresividad.
- **Impacto:** Viabilidad de REQ-023 y REQ-026.
- **Estado:** pendiente

## Preguntas abiertas

- La lista de idiomas que se pueden asignar a una voz al subirla (REQ-008) no está fijada; se propone limitarla a los que admite el motor de síntesis, y se concretará en diseño.
