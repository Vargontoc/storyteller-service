# Requisitos: character-inventory

Estado: aprobado
Revisión: 1

## Contexto

Storyteller Agent necesita un catálogo de protagonistas reutilizables entre cuentos. Esta especificación define la generación y revisión del texto de las fichas de personaje (nombre, descripción narrativa y descripción visual) y el flujo de usuario del catálogo. El retrato, la persistencia técnica, el tratamiento de imágenes y la generación de cuentos quedan fuera de alcance.

## Fuera de alcance

- Generación, selección y almacenamiento de retratos con ComfyUI o cualquier otro generador de imágenes.
- Diseño e implementación de la persistencia (esquema de base de datos, repositorios, etc.).
- Procesamiento, conversión o gestión de ficheros de imagen/audio.
- Generación de guiones, páginas o cuentos.
- Definición de las reglas concretas de edad y de especies fáciles de dibujar: se asume que residen en la configuración del agente de personaje.
- Edición directa de la descripción narrativa o visual sin intervención del agente; los cambios de descripción se realizan a través del flujo de revisión del agente.

## Requisitos funcionales

### REQ-001 Listado del catálogo de personajes

- **Requisito:** Cuando el usuario accede al catálogo de personajes, el sistema deberá mostrar la lista de protagonistas existentes con su nombre y su estado actual.
- **Criterios de aceptación:**
  - Dado que existen los personajes "Pepe el conejo" y "Luna la tortuga", cuando el usuario abre el catálogo, entonces visualiza ambos nombres y sus estados.
  - Dado que el catálogo está vacío, cuando el usuario abre el catálogo, entonces el sistema muestra un indicador de que no hay personajes.

### REQ-002 Selección y visualización de ficha

- **Requisito:** Cuando el usuario selecciona un personaje del catálogo, el sistema deberá mostrar su ficha textual: nombre, descripción narrativa y descripción visual.
- **Criterios de aceptación:**
  - Dado un personaje con ficha completa, cuando el usuario lo selecciona, entonces ve el nombre, la descripción narrativa y la descripción visual.
  - Dado un personaje con una revisión pendiente, cuando el usuario lo selecciona, entonces ve un indicador de revisión pendiente.

### REQ-003 Creación automática de personaje

- **Requisito:** Cuando el usuario elige crear un personaje en modo automático, el sistema deberá pedir al agente de personaje que genere un nombre, una descripción narrativa y una descripción visual acordes a las reglas de edad del producto.
- **Criterios de aceptación:**
  - Dado que el agente de personaje está disponible, cuando el usuario pulsa "Automático", entonces el sistema presenta una ficha textual completa sin que el usuario haya escrito nada.
  - Dado que se ha generado una ficha, cuando el usuario la revisa, entonces no aparecen elementos rechazados por las reglas de edad para niños de 3 a 4 años.

### REQ-004 Creación manual de personaje con validación

- **Requisito:** Cuando el usuario elige el modo manual y escribe una frase con el nombre y la descripción del personaje, el sistema deberá enviarla al agente de personaje; si es acorde a las reglas de edad, el agente completará la ficha textual; si no lo es, el sistema deberá mostrar un aviso con el motivo de rechazo.
- **Criterios de aceptación:**
  - Dada una frase válida como "Un conejo llamado Pepe que le gusta saltar", cuando el usuario la envía, entonces el sistema muestra una ficha textual completa.
  - Dada una frase no acorde a las reglas de edad, cuando el usuario la envía, entonces el sistema muestra un aviso de rechazo con una explicación y no crea la ficha.

### REQ-005 Indicador de borrador

- **Requisito:** Mientras un personaje no tenga un retrato aprobado, el sistema deberá mostrarlo en estado "borrador" en el catálogo.
- **Criterios de aceptación:**
  - Dado un personaje sin retrato aprobado, cuando se lista o selecciona, entonces aparece el estado "borrador".
  - Dado un personaje con retrato aprobado, cuando se lista o selecciona, entonces no aparece el estado "borrador".

### REQ-006 Renombrar personaje

- **Requisito:** Cuando el usuario edita el nombre de un personaje en el catálogo, el sistema deberá guardar el nuevo nombre para usos futuros y conservar el nombre anterior en los cuentos ya generados.
- **Criterios de aceptación:**
  - Dado un personaje llamado "Pepe", cuando el usuario cambia su nombre a "Pepe el saltarín", entonces la ficha del catálogo muestra el nuevo nombre.
  - Dado un personaje que aparece en un cuento ya generado, cuando el usuario lo renombra en el catálogo, entonces el cuento conserva el nombre anterior.

### REQ-007 Solicitud de revisión con descarte de previa

- **Requisito:** Cuando el usuario solicita una revisión de un personaje, el sistema deberá comprobar si existe una revisión previa pendiente; si la hay, deberá advertir al usuario y, si confirma, marcará la previa como descartada antes de procesar la nueva solicitud.
- **Criterios de aceptación:**
  - Dado un personaje con una revisión pendiente, cuando el usuario solicita una nueva revisión y confirma el aviso, entonces la revisión previa pasa a descartada y se procesa la nueva.
  - Dado un personaje sin revisión pendiente, cuando el usuario solicita una revisión, entonces se procesa directamente sin avisos.

### REQ-008 Validación del cambio propuesto

- **Requisito:** Cuando el usuario escribe un cambio en lenguaje natural para un personaje, el sistema deberá pedir al agente que valide si el cambio es acorde a las reglas del producto; si no lo es, deberá mostrar un aviso con el motivo de rechazo; si lo es, deberá mostrar la propuesta del agente como revisión pendiente para que el usuario la acepte más tarde.
- **Criterios de aceptación:**
  - Dado un cambio no válido según las reglas de edad, cuando el usuario lo envía, entonces el sistema muestra un aviso con el motivo de rechazo y no crea una revisión pendiente.
  - Dado un cambio válido, cuando el usuario lo envía, entonces el sistema muestra la propuesta del agente y conserva la revisión en estado pendiente.

### REQ-009 Aviso de consistencia visual

- **Requisito:** Si el cambio propuesto modifica la descripción visual de un personaje y ya existen imágenes generadas para ese personaje, el sistema deberá advertir al usuario de la posible pérdida de consistencia visual.
- **Criterios de aceptación:**
  - Dado un personaje con imágenes previas y un cambio en la descripción visual, cuando el agente genera una propuesta válida, entonces el sistema muestra un aviso de consistencia visual junto con la propuesta.
  - Dado un personaje sin imágenes previas, cuando se propone un cambio visual, entonces no aparece el aviso de consistencia visual.
  - Dado un personaje con imágenes previas y un cambio solo en la descripción narrativa, cuando se genera la propuesta, entonces no aparece el aviso de consistencia visual.

### REQ-010 Indicador de revisión pendiente

- **Requisito:** Cuando un personaje tiene una revisión pendiente, el sistema deberá mostrar un badge de revisión en el listado del catálogo y en la ficha del personaje.
- **Criterios de aceptación:**
  - Dado un personaje con revisión pendiente, cuando se muestra el catálogo, entonces aparece el badge en su tarjeta o fila.
  - Dado un personaje con revisión pendiente, cuando se abre su ficha, entonces el badge aparece junto a la sección de revisión.
  - Dado un personaje sin revisión pendiente, cuando se lista o selecciona, entonces no aparece ningún badge de revisión.

### REQ-011 Aceptar revisión

- **Requisito:** Cuando el usuario abre una revisión pendiente y la acepta, el sistema deberá aplicar los cambios textuales propuestos a la ficha del personaje y marcar la revisión como aceptada.
- **Criterios de aceptación:**
  - Dada una revisión pendiente que cambia la descripción narrativa, cuando el usuario pulsa "Aceptar", entonces la ficha muestra el nuevo texto y la revisión deja de estar pendiente.
  - Dada una revisión pendiente, cuando el usuario la acepta, entonces el badge de revisión pendiente desaparece.

### REQ-012 Descartar revisión

- **Requisito:** Cuando el usuario abre una revisión pendiente y la descarta, el sistema deberá conservar la ficha actual del personaje y marcar la revisión como descartada.
- **Criterios de aceptación:**
  - Dada una revisión pendiente, cuando el usuario pulsa "Descartar", entonces la ficha conserva su texto original.
  - Dada una revisión pendiente, cuando el usuario la descarta, entonces el badge de revisión pendiente desaparece.

## Requisitos no funcionales

Ninguno.

## Supuestos

### SUP-001 Reglas de edad y especies del agente

- **Tipo:** decisional
- **Descripción:** Las reglas concretas de edad (contenido apto para niños de 3 a 4 años) y la lista de especies fáciles de dibujar residen en la configuración del agente de personaje y no se definen en esta especificación.
- **Impacto:** Los requisitos de validación (REQ-004 y REQ-008) definen el comportamiento genérico de aceptación/rechazo; los casos de prueba concretos dependerán de esas reglas.
- **Estado:** validado

### SUP-002 Interpretación del segundo "Flujo 3"

- **Tipo:** contextual
- **Descripción:** El segundo flujo numerado como "Flujo 3" por el usuario se interpreta como el flujo de aceptación o descarte de una revisión que ya está pendiente.
- **Impacto:** Se modela mediante los requisitos REQ-010, REQ-011 y REQ-012.
- **Estado:** validado

### SUP-003 Existencia de retrato e imágenes generadas

- **Tipo:** contextual
- **Descripción:** El estado de "retrato aprobado" de un personaje y la existencia de imágenes generadas para ese personaje se mantienen fuera de esta feature.
- **Impacto:** REQ-005 y REQ-009 leen esa información como condición, pero no generan ni gestionan los activos visuales.
- **Estado:** validado

## Preguntas abiertas

Ninguna.
